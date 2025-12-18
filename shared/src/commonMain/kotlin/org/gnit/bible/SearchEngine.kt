package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.index.DirectoryReader
import org.gnit.lucenekmp.index.StandardDirectoryReader
import org.gnit.lucenekmp.queryparser.classic.QueryParser
import org.gnit.lucenekmp.search.BooleanClause
import org.gnit.lucenekmp.search.BooleanQuery
import org.gnit.lucenekmp.search.IndexSearcher
import org.gnit.lucenekmp.search.Query
import org.gnit.lucenekmp.search.Sort
import org.gnit.lucenekmp.search.SortField
import org.gnit.lucenekmp.store.FSDirectory

class SearchEngine(val fs: FileSystem) {

    val logger = KotlinLogging.logger {}

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ): List<String> {
        val result = mutableListOf<String>()

        val indexDir: Path = TODO("implement some kotlin/common code to obtain index dir which works on all platforms including, Kotlin/Common for both [composeApp: Kotlin/Android, iOS Kotlin/Native, Kotlin/JVM] and [cli: macOS Kotlin/Native, linux Kotlin/Native]")

        val fsDirectory: FSDirectory = TODO("proper FSDirectory implementation need to be spplied depending on platform using indexDir")

        val iReader: DirectoryReader = StandardDirectoryReader.open(
            directory = fsDirectory,
            leafSorter = null,
            commit = null
        )

        val iSearcher = IndexSearcher(iReader)
        val parser = QueryParser("text", SimpleAnalyzer())
        val termQuery = parser.parse(term)!!

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

        logger.debug {"searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"}

        val hits = iSearcher.search(query, verses, Sort(SortField.FIELD_DOC)).scoreDocs
        return hits.map { hit ->
            val hitDoc = iSearcher.indexReader.storedFields().document(hit.doc)
            val book = hitDoc.getField("book")?.numericValue()?.toInt()!!
            val chapter = hitDoc.getField("chapter")?.numericValue()?.toInt()
            val verse = hitDoc.getField("verse")?.numericValue()?.toInt()
            val text = hitDoc.get("text")

            "${bookNameEnglishCapital(book)} $chapter:$verse $text"
        }

        return result
    }

    companion object {

        /**
         * filename postfix for index manifest the file name will be constructed as
         * `val fileName: String = "${translation.code}$INDEX_MANIFEST_FILENAME_POSTFIX"`
         *
         * file format is a plaintext containing list of file names like following:
         *
         * ```
         * _0.cfe
         * _0.cfs
         * _0.si
         * segments_1
         * write.lock
         * ```
        */
        const val INDEX_MANIFEST_FILENAME_POSTFIX = ".index.manifest"

        fun chapterQuery(startChapter: Int, endChapter: Int?): Query {
            return IntPoint.newRangeQuery("chapter", startChapter, endChapter ?: startChapter)
        }

        fun includesNewTestamentOnlyPhrase(term: String): Boolean {
            arrayOf("Jesus Cristo", "Иисуса Христа", "Ісуса Христа", "Jesu Kristi", "예수 그리스도").forEach { jesusChrist ->
                if (term.contains(jesusChrist)) return true
            }
            return false
        }
    }
}
