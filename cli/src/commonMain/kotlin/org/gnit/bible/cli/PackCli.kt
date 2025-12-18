package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import org.gnit.bible.Bible
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.bookNameEnglish
import org.gnit.bible.downloadableTranslationCodeList
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.document.Document
import org.gnit.lucenekmp.document.Field
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.document.StoredField
import org.gnit.lucenekmp.document.TextField
import org.gnit.lucenekmp.index.IndexWriter
import org.gnit.lucenekmp.index.IndexWriterConfig
import org.gnit.lucenekmp.store.FSDirectory

class PackCli(
    private val bible: Bible
) : CliktCommand(name = "pack") {

    val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Pack a directory named with translation code into a zip file of bblpack file which can be imported for bbl"
    }

    private val target by argument(help = "translation code of new bblpack dir/zip to be created, e.g. webus, jc")

    override fun run(){
        val normalizedTarget = target.trim().lowercase()
        if (normalizedTarget.isEmpty()) {
            throw CliktError("Missing translation code")
        }

        val inputPathString = when {
            normalizedTarget.contains("/") || normalizedTarget.contains("\\") -> normalizedTarget
            else -> "../server/src/main/resources/files/bbltexts/$normalizedTarget"
        }

        val outputPathString = "../server/src/main/resources/files/bblpacks"
        createBblPack(inputPathString = inputPathString, outputPathString = outputPathString)
    }

    private val fileSystem = bible.assetManager.fileSystem

    private fun deleteRecursively(fileSystem: FileSystem, path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) {
            fileSystem.list(path).forEach { child ->
                deleteRecursively(fileSystem, child)
            }
        }
        fileSystem.delete(path)
    }

    private fun sanitizeForLuceneStandardAnalyzer(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val mapped = when (ch) {
                '\u2018', '\u2019' -> '\''
                '\u201C', '\u201D' -> '"'
                '\u2013', '\u2014' -> '-'
                '\u00A0' -> ' '
                else -> ch
            }
            val cleaned = when {
                mapped.code in 0x00..0x1F && mapped != '\n' && mapped != '\t' && mapped != '\r' -> ' '
                else -> mapped
            }
            sb.append(cleaned)
        }
        return sb.toString()
    }

    fun createBblPack(inputPathString: String, outputPathString: String = "bblpack") {
        val currentDir = currentDir()
        logger.debug { "currentDir: $currentDir" }

        // validate input path
        val inputPath = currentDir.resolve(inputPathString, true)
        if (fileSystem.exists(inputPath) && fileSystem.metadata(inputPath).isDirectory) {
            logger.info { "Input path $inputPath exists and is dir" }
        } else {
            if (!fileSystem.exists(inputPath)) {
                logger.error { "Input path $inputPath does not exits" }; return
            }
            if (!fileSystem.metadata(inputPath).isDirectory) {
                logger.error { "Input path $inputPath is not a directory" }; return
            }
        }

        // validate output path
        val outputPath = currentDir.resolve(outputPathString, true)
        if (fileSystem.exists(outputPath) && fileSystem.metadata(outputPath).isDirectory) {
            logger.info { "Output path $outputPath exists and is dir" }
        } else {
            if (!fileSystem.exists(outputPath)) {
                logger.error { "Input path $outputPath does not exits" }; return
            }
            if (!fileSystem.metadata(outputPath).isDirectory) {
                logger.error { "Input path $outputPath is not a directory" }; return
            }
        }

        // get translation code
        val translationCode = inputPath.name
        logger.info { "translationCode: $translationCode" }

        // validate manifest json
        val manifestPath = inputPath.resolve("$translationCode$MANIFEST_JSON_POSTFIX")
        if (fileSystem.exists(manifestPath) && fileSystem.metadata(manifestPath).isRegularFile) {
            logger.info { "Manifest path $manifestPath exists and is file" }
        } else {
            if (!fileSystem.exists(manifestPath)) {
                logger.error { "Manifest path $manifestPath does not exits" }; return
            }
            if (!fileSystem.metadata(manifestPath).isRegularFile) {
                logger.error { "Manifest path $manifestPath is not a file" }; return
            }
        }

        val translation: Translation
        try {
            translation = Translation.fromJson(fileSystem.read(manifestPath) { readUtf8() })
        } catch (e: Throwable) {
            logger.error { "error while reading/parsing $manifestPath: ${e.message}" }; return
        }

        runCatching { createLuceneKmpIndex(translation = translation, translationDir = inputPath) }
            .onFailure { e ->
                logger.error { "failed to create lucene-kmp index for ${translation.code} at $inputPath: ${e.message}" }
                return
            }

        // zip everything into ${translationCode}.zip
        val dir = File(outputPath.toString())
        if (dir.exists && dir.isDirectory) {
            logger.info { "Output directory ${dir.name} exists and is directory" }
        }else{
            if (!dir.exists) {
                logger.error { "Output directory ${dir.name} does not exits" }; return
            }
            if (!dir.isDirectory) {
                logger.error { "Output directory ${dir.name} is not a directory" }; return
            }
        }
        val sourceDirectory = File(inputPath.toString())
        if (sourceDirectory.exists && sourceDirectory.isDirectory) {
            logger.info { "Source directory ${sourceDirectory.name} exists and is directory" }
        } else {
            if (!sourceDirectory.exists) {
                logger.error { "Source directory ${sourceDirectory.name} does not exits" }; return
            }
            if (!sourceDirectory.isDirectory) {
                logger.error { "Source directory ${sourceDirectory.name} is not a directory" }; return
            }
        }

        val zip = File(dir, "${translation.code}.zip")
        if (zip.exists) {
            logger.info { "zip file ${zip.name} exists; deleting to recreate (ZipFile does not support overwriting entries)" }
            val deleted = runCatching { runBlocking { zip.delete() } }
                .onFailure { e -> logger.error { "failed to delete existing zip file ${zip.name}: ${e.message}" } }
                .getOrElse { false }
            if (!deleted) {
                logger.error { "failed to delete existing zip file ${zip.name}" }
                return
            }
            if (zip.exists) {
                logger.error { "failed to delete existing zip file ${zip.name}" }
                return
            }
        } else {
            logger.info { "Zip file ${zip.name} does not exist as expected, creating new one" }
        }

        val manifestFile = File(manifestPath.toString())
        if(manifestFile.exists && !manifestFile.isDirectory) {
            logger.info { "Manifest file ${manifestFile.name} exists and is not dir (so maybe is file as expected)" }
        } else {
            if (!manifestFile.exists) {
                logger.error { "Manifest file ${manifestFile.name} does not exits" }; return
            }
            if (!manifestFile.isDirectory) {
                logger.error { "Manifest file ${manifestFile.name} is dir and so is not a file" }; return
            }
        }

        runBlocking {
            ZipFile(zip, FileMode.Write).use {
                it.zipFile(manifestFile)
                it.zipDirectory(sourceDirectory, shallow = false) { name ->
                    val normalized = name.replace('\\', '/')
                    normalized.endsWith(".txt") ||
                        (normalized.startsWith("index/") && !normalized.endsWith("write.lock"))
                }
            }
        }

    }

    fun createLuceneKmpIndex(
        translation: Translation,
        translationDir: Path,
        indexDirName: String = "index",
        fileSystem: FileSystem = this.fileSystem
    ): Int {
        val indexPath = translationDir.resolve(indexDirName)
        if (fileSystem.exists(indexPath)) {
            deleteRecursively(fileSystem, indexPath)
        }

        fileSystem.createDirectories(indexPath)

        val analyzer = SimpleAnalyzer()
        val config = IndexWriterConfig(analyzer)
        val iWriter = IndexWriter(FSDirectory.open(indexPath), config)
        val chapterFileRegex =
            Regex("^${Regex.escape(translation.code)}\\.(\\d{1,2})\\.(\\d{1,3})\\.txt$")

        var totalDocs = 0
        try {
            val chapterFiles = fileSystem.list(translationDir)
                .filter { path ->
                    fileSystem.metadata(path).isRegularFile && chapterFileRegex.matches(path.name)
                }
                .sortedWith(
                    compareBy<Path>(
                        { chapterFileRegex.matchEntire(it.name)!!.groupValues[1].toInt() },
                        { chapterFileRegex.matchEntire(it.name)!!.groupValues[2].toInt() }
                    )
                )

            chapterFiles.forEach { chapterPath ->
                val match = chapterFileRegex.matchEntire(chapterPath.name)!!
                val book = match.groupValues[1].toInt()
                val chapter = match.groupValues[2].toInt()

                val chapterText = fileSystem.read(chapterPath) { readUtf8() }
                val verses = splitChapterToVerses(chapterText)

                logger.debug { "indexing ${translation.code} ${bookNameEnglish(book)} $chapter (${verses.size} verses) from $chapterPath" }

                verses.forEachIndexed { index, text ->
                    val verse = index + 1
                    val sanitizedText = sanitizeForLuceneStandardAnalyzer(text)
                    val doc = Document().apply {
                        add(IntPoint("book", book))
                        add(StoredField("book", book))

                        add(IntPoint("chapter", chapter))
                        add(StoredField("chapter", chapter))

                        add(IntPoint("verse", verse))
                        add(StoredField("verse", verse))

                        add(Field("text", sanitizedText, TextField.TYPE_STORED))
                    }
                    iWriter.addDocument(doc)
                    totalDocs++
                }
            }
        } finally {
            runCatching { iWriter.close() }.getOrNull()
            runCatching {
                val lockPath = indexPath.resolve("write.lock")
                if (fileSystem.exists(lockPath)) fileSystem.delete(lockPath)
            }.getOrNull()
        }

        return totalDocs
    }
}


fun packTranslation(translationCode: String){
    PackCli(Bible()).createBblPack(
        inputPathString = "../server/src/main/resources/files/bbltexts/$translationCode",
        outputPathString = "../server/src/main/resources/files/bblpacks/"
    )
}

fun main() {
    downloadableTranslationCodeList.forEach { translationCode ->
        packTranslation(translationCode)
    }
}
