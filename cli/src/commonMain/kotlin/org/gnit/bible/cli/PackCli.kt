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
import okio.Path
import org.gnit.bible.Bible
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.Books
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.bookNameEnglish
import org.gnit.bible.downloadableTranslationCodeList
import org.gnit.lucenekmp.analysis.standard.StandardAnalyzer
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

        // TODO create lucene-kmp index later

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
            logger.info { "zip file ${zip.name} exists" }
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
                it.zipDirectory(sourceDirectory, shallow = true) {
                    name -> name.endsWith(".txt")
                }
            }
        }

    }

    fun createLuceneKmpIndex(translation: Translation /* TODO define proper parameters including fileSystem swappable with FakeFileSystem for tests*/) {
        // TODO create lucene-kmp search index for English language bible

        val indexPath: Path = TODO("proper index path for both production and test")

        val analyzer = StandardAnalyzer()
        val config = IndexWriterConfig(analyzer)
        val iWriter = IndexWriter(FSDirectory.open(indexPath), config)
        (1..66).forEach { book ->
            val maxChapter = Books.maxChapter(book)
            (1..maxChapter).forEach { chapter ->

                val versePointer =
                    VersePointer(translation = translation, book = book, chapter = chapter)

                val aChapter = bible.verses(translation = versePointer.translation.code, book = versePointer.book, chapter = versePointer.chapter)

                val split = splitChapterToVerses(aChapter)

                logger.debug {"book $book ${bookNameEnglish(book)} $chapter split to ${split.size}"}

                split.forEachIndexed { index, text ->
                    val doc = Document()
                    val verse = index + 1

                    logger.debug {"adding book $book ${bookNameEnglish(book)} $chapter:$verse $text as a document"}

                    doc.add(IntPoint("book", book))
                    doc.add(StoredField("book", book))

                    doc.add(IntPoint("chapter", chapter))
                    doc.add((StoredField("chapter", chapter)))

                    doc.add(IntPoint("verse", verse))
                    doc.add((StoredField("verse", verse)))

                    doc.add(Field("text", text, TextField.TYPE_STORED))
                    iWriter.addDocument(doc)
                    val count = iWriter.getDocStats().numDocs

                    logger.debug {"$count docs were added to the index of $translation"}
                }
            }
        }

        iWriter.close()
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
