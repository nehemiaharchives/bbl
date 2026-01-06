package org.gnit.bible.cli

// Extracted from :cli to :cli:packer as part of Phase 6.

import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.DefaultAnalyzerProvider
import org.gnit.bible.SearchEngine.Companion.INDEX_MANIFEST_FILENAME_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.bookNameEnglish
import org.gnit.lucenekmp.document.Document
import org.gnit.lucenekmp.document.Field
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.document.StoredField
import org.gnit.lucenekmp.document.TextField
import org.gnit.lucenekmp.index.IndexWriter
import org.gnit.lucenekmp.index.IndexWriterConfig
import org.gnit.lucenekmp.store.FSDirectory

class IndexBuilder(
    bible: Bible,
    private val analyzerProvider: AnalyzerProvider = DefaultAnalyzerProvider()
) {

    private val logger = KotlinLogging.logger {}

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

        val analyzer = analyzerProvider.analyzerFor(translation.language)
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
                val verses = Bible.splitChapterToVerses(chapterText)

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
                val marker = indexPath.resolve("${translation.code}.index.marker")
                if (!fileSystem.exists(marker)) {
                    fileSystem.write(marker) { writeUtf8("ok\n") }
                }

                val meta = indexPath.resolve("${translation.code}.index.meta")
                if (!fileSystem.exists(meta)) {
                    fileSystem.write(meta) { writeUtf8("docs=$totalDocs\n") }
                }
            }.getOrNull()

            runCatching {
                val lockPath = indexPath.resolve("write.lock")
                if (fileSystem.exists(lockPath)) fileSystem.delete(lockPath)
            }.getOrNull()

            logger.debug { "creating index manifest for ${translation.code} at $indexPath" }
            runCatching {
                val manifestPath = indexPath.resolve("${translation.code}$INDEX_MANIFEST_FILENAME_POSTFIX")
                val entries = fileSystem.list(indexPath)
                    .filter { p -> fileSystem.metadata(p).isRegularFile }
                    .map { it.name }
                    .filter { it != manifestPath.name }
                    .sorted()

                fileSystem.write(manifestPath) {
                    writeUtf8(entries.joinToString(separator = "\n", postfix = if (entries.isNotEmpty()) "\n" else ""))
                }
            }.onFailure { e ->
                logger.error { "failed to write index manifest for ${translation.code} at $indexPath: ${e.message}" }
            }
        }

        return totalDocs
    }
}

fun createEmbeddedLuceneKmpIndex(translation: Translation) {
    val translationDir =
        "../composeApp/src/commonMain/composeResources/files/bblpacks/${translation.code}".toPath()

    IndexBuilder(Bible()).createLuceneKmpIndex(
        translation = translation,
        translationDir = translationDir
    )
}

