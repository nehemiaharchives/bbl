package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.lucenekmp.analysis.Analyzer
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
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import org.gnit.lucenekmp.store.IOContext

class SearchEngine(private val reader: BibleResourcesReader) {

    private val logger = KotlinLogging.logger {}
    private val directoriesByTranslation = HashMap<String, ByteBuffersDirectory>()

    private val languageAnalyserCache: MutableMap<String, Analyzer> = mutableMapOf()

    private fun analyzerFor(translation: Translation): Analyzer{
        return languageAnalyserCache.getOrPut(translation.language.code){
            translation.language.analyzerFactory?.invoke() ?: SimpleAnalyzer()
        }
    }

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ): List<String> {
        val directory = embeddedIndexDirectory(translation)

        val iReader: DirectoryReader = StandardDirectoryReader.open(
            directory = directory,
            leafSorter = null,
            commit = null
        )

        iReader.use { reader ->
            val iSearcher = IndexSearcher(reader)
            val analyzer = analyzerFor(translation)
            val parser = QueryParser("text", analyzer)
            val termQuery = parser.parse(term)!!

            val queryBuilder = BooleanQuery.Builder()

            /*if (includesNewTestamentOnlyPhrase(term)) {
                queryBuilder.add(IntPoint.newRangeQuery("book", 40, 66), BooleanClause.Occur.MUST)
            }*/

            if (bookNumber != null) {
                queryBuilder.add(IntPoint.newExactQuery("book", bookNumber), BooleanClause.Occur.MUST)

                if (startChapter != null) {
                    queryBuilder.add(chapterQuery(startChapter, endChapter), BooleanClause.Occur.MUST)
                }
            }

            val query = queryBuilder.add(termQuery, BooleanClause.Occur.MUST).build()

            logger.debug {
                "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
            }

            val hits = iSearcher.search(query, verses, Sort(SortField.FIELD_DOC)).scoreDocs
            return hits.map { hit ->
                val hitDoc = iSearcher.indexReader.storedFields().document(hit.doc)
                val book = hitDoc.getField("book")?.numericValue()?.toInt()!!
                val chapter = hitDoc.getField("chapter")?.numericValue()?.toInt()
                val verse = hitDoc.getField("verse")?.numericValue()?.toInt()
                val text = hitDoc.get("text")

                "${bookNameFor(bookNumber = book, translation = translation)} $chapter:$verse $text"
            }
        }
    }

    private fun embeddedIndexDirectory(translation: Translation): ByteBuffersDirectory {
        directoriesByTranslation[translation.code]?.let { return it }

        val indexDir = ByteBuffersDirectory()
        val files = reader.listIndexFiles(translation.code)
        require(files.isNotEmpty()) { "Index manifest returned empty file list for ${translation.code}" }

        files.forEach { name ->
            val bytes = reader.readIndexFile(translation.code, name)
            indexDir.createOutput(name, IOContext.DEFAULT).use { out ->
                out.writeBytes(bytes, 0, bytes.size)
            }
        }

        directoriesByTranslation[translation.code] = indexDir
        logger.debug { "loaded ${files.size} embedded index files into ByteBuffersDirectory for ${translation.code}" }
        return indexDir
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
