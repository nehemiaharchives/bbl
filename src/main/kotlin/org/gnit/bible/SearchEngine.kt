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

    arrayOf("_0.cfe", "_0.cfs", "_0.si", "segments_1", "write.lock").forEach { fileName ->
        val webusCfe = indexDir.resolve(fileName)
        Files.write(webusCfe, getResourceReader().readBytes("index/$translation/$fileName"))
    }

    val iReader: DirectoryReader = StandardDirectoryReader.open(NIOFSDirectory(indexDir, FSLockFactory.getDefault()))
    val iSearcher = IndexSearcher(iReader)
    val parser = QueryParser("text", getAnalyzer(translation))
    val termQuery = parser.parse(term)

    val queryBuilder = BooleanQuery.Builder()

    if (bookNumber != null) {
        queryBuilder.add(IntPoint.newExactQuery("book", bookNumber), BooleanClause.Occur.MUST)

        if (startChapter != null) {
            queryBuilder.add(chapterQuery(startChapter, endChapter), BooleanClause.Occur.MUST)
        }
    }

    val query = queryBuilder.add(termQuery, BooleanClause.Occur.MUST).build()

    logger.debug("searching $term ${if (bookNumber!=null) "in ${bookNameCapital(bookNumber)} " else " "}in $translation")

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

fun main() {
    search("Jesus Christ", null, null, null, 100, Translation.webus).forEach { println(it) }
}
