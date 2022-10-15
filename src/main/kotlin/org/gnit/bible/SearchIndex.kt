package org.gnit.bible

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists
import kotlin.system.exitProcess

fun indexPath(translation: Translation): Path = Paths.get("src/main/resources/texts/$translation/index")

fun createIndex(translation: Translation) {

    val indexPath = indexPath(translation)

    if (indexPath.notExists()) {
        indexPath.createDirectory()
    }

    indexPath.toFile().listFiles()?.forEach { it.delete() }

    if (indexPath.toFile().listFiles()?.size == 0) {
        logger.debug("index files of $translation deleted")
    } else {
        logger.error("index files of $translation exists, terminating indexing process")
        exitProcess(0)
    }

    logger.debug("creating index files of $translation")

    val analyzer: Analyzer = translation.getAnalyzer()
    val config = IndexWriterConfig(analyzer)
    val iWriter = IndexWriter(FSDirectory.open(indexPath), config)

    (1..66).forEach { book ->
        val maxChapter = Chapters.maxChapter(book)
        (1..maxChapter).forEach { chapter ->

            val versePointer = VersePointer(translation = translation, book = book, chapter = chapter)

            val path = chapterTextPath(versePointer)

            logger.debug("reading book $book ${bookName(book)} $chapter in $path")

            val aChapter = File("src/main/resources/$path").readText()

            val split = splitChapterToVerses(aChapter)

            logger.debug("book $book ${bookName(book)} $chapter split to ${split.size}")

            split.forEachIndexed { index, text ->
                val doc = Document()
                val verse = index + 1

                logger.debug("adding book $book ${bookName(book)} $chapter:$verse $text as a document")

                doc.add(IntPoint("book", book))
                doc.add(StoredField("book", book))

                doc.add(IntPoint("chapter", chapter))
                doc.add((StoredField("chapter", chapter)))

                doc.add(IntPoint("verse", verse))
                doc.add((StoredField("verse", verse)))

                doc.add(Field("text", text, TextField.TYPE_STORED))
                iWriter.addDocument(doc)
                val count = iWriter.docStats.numDocs

                logger.debug("$count docs were added to the index of $translation")
            }
        }
    }

    iWriter.close()
}

fun main() {
    /*listOf(
        Translation.webus,
        Translation.kjv,
        Translation.cunp,
        Translation.krv,
        Translation.jc,
    )*/
    Translation.values().forEach { translation ->
        createIndex(translation)
    }
}