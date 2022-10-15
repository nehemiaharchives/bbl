package org.gnit.bible

import com.google.common.jimfs.Jimfs
import org.apache.lucene.document.IntPoint
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.StandardDirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.FSLockFactory
import org.apache.lucene.store.NIOFSDirectory
import java.nio.file.Files

fun chapterQuery(startChapter: Int, endChapter: Int?): Query {
    return IntPoint.newRangeQuery("chapter", startChapter, endChapter ?: startChapter)
}

val indexFiles = arrayOf("_0.cfe", "_0.cfs", "_0.si", "segments_1", "write.lock")

fun search(
    term: String,
    bookNumber: Int?,
    startChapter: Int?,
    endChapter: Int?,
    verses: Int,
    translation: Translation
): List<String> {

    val fs = Jimfs.newFileSystem()
    val root = fs.rootDirectories.first()

    val indexDir = root.resolve("index")
    Files.createDirectory(indexDir)

    indexFiles.forEach { fileName ->
        val indexFile = indexDir.resolve(fileName)
        Files.write(indexFile, getResourceReader().readBytes("texts/$translation/index/$fileName"))
    }

    val iReader: DirectoryReader = StandardDirectoryReader.open(NIOFSDirectory(indexDir, FSLockFactory.getDefault()))
    val iSearcher = IndexSearcher(iReader)
    val parser = QueryParser("text", translation.getAnalyzer())
    val termQuery = parser.parse(term)

    val queryBuilder = BooleanQuery.Builder()

    if (includesNewTestamentOnlyPhrase(term)) {
        queryBuilder.add(IntPoint.newRangeQuery("book", 40, 66), BooleanClause.Occur.MUST)
    }

    if (bookNumber != null) {
        queryBuilder.add(IntPoint.newExactQuery("book", bookNumber), BooleanClause.Occur.MUST)

        if (startChapter != null) {
            queryBuilder.add(chapterQuery(startChapter, endChapter), BooleanClause.Occur.MUST)
        }
    }

    val query = queryBuilder.add(termQuery, BooleanClause.Occur.MUST).build()

    logger.debug("searching $term ${if (bookNumber != null) "in ${bookNameCapital(bookNumber)} " else " "}in $translation")

    val hits = iSearcher.search(query, verses, Sort(SortField.FIELD_DOC)).scoreDocs
    return hits.map { hit ->
        val hitDoc = iSearcher.doc(hit.doc)
        val book = hitDoc.getField("book").numericValue().toInt()
        val chapter = hitDoc.getField("chapter").numericValue().toInt()
        val verse = hitDoc.getField("verse").numericValue().toInt()
        val text = hitDoc.get("text")

        "${bookNameCapital(book)} $chapter:$verse $text"
    }
}

fun includesNewTestamentOnlyPhrase(term: String): Boolean {
    arrayOf("Jesus Cristo", "Иисуса Христа", "Ісуса Христа", "Jesu Kristi", "예수 그리스도").forEach { jesusChrist ->
        if (term.contains(jesusChrist)) return true
    }
    return false
}

fun main() {
    search("Jesus Christ", null, null, null, 100, Translation.webus).forEach { println(it) }
}
