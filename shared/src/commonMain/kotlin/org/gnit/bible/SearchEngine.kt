package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.index.DirectoryReader
import org.gnit.lucenekmp.index.StandardDirectoryReader
import org.gnit.lucenekmp.jdkport.putIfAbsent
import org.gnit.lucenekmp.queryparser.classic.QueryParser
import org.gnit.lucenekmp.search.BooleanClause
import org.gnit.lucenekmp.search.BooleanQuery
import org.gnit.lucenekmp.search.IndexSearcher
import org.gnit.lucenekmp.search.Query
import org.gnit.lucenekmp.search.Sort
import org.gnit.lucenekmp.search.SortField
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import org.gnit.lucenekmp.store.IOContext
import org.gnit.lucenekmp.util.QueryBuilder

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
            val requireAllTokens = term.any { it.isWhitespace() }
            val filterClauses = mutableListOf<Pair<Query, BooleanClause.Occur>>()

            /*if (includesNewTestamentOnlyPhrase(term)) {
                filterClauses.add(IntPoint.newRangeQuery("book", 40, 66) to BooleanClause.Occur.MUST)
            }*/

            if (bookNumber != null) {
                filterClauses.add(IntPoint.newExactQuery("book", bookNumber) to BooleanClause.Occur.MUST)

                if (startChapter != null) {
                    filterClauses.add(chapterQuery(startChapter, endChapter) to BooleanClause.Occur.MUST)
                }
            }

            fun buildQuery(termQuery: Query): Query {
                val builder = BooleanQuery.Builder()
                filterClauses.forEach { (query, occur) -> builder.add(query, occur) }
                builder.add(termQuery, BooleanClause.Occur.MUST)
                return builder.build()
            }

            fun runSearch(queryAnalyzer: Analyzer, requireAllTokens: Boolean): Array<org.gnit.lucenekmp.search.ScoreDoc> {
                val queries = buildTermQueries(term, queryAnalyzer, requireAllTokens)
                if (queries.isEmpty()) return emptyArray()
                val merged = LinkedHashMap<Int, org.gnit.lucenekmp.search.ScoreDoc>()
                for (queryTerm in queries) {
                    val query = buildQuery(queryTerm)
                    val hits = iSearcher.search(query, reader.maxDoc(), Sort(SortField.FIELD_DOC)).scoreDocs
                    for (hit in hits) {
                        merged.putIfAbsent(hit.doc, hit)
                    }
                }
                return merged.values.toTypedArray()
            }

            logger.debug {
                "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
            }

            var hits = runSearch(analyzer, requireAllTokens)
            if (hits.isEmpty() && requireAllTokens) {
                hits = runSearch(analyzer, requireAllTokens = false)
            }
            if (hits.isEmpty() && analyzer !is SimpleAnalyzer) {
                hits = runSearch(SimpleAnalyzer(), requireAllTokens)
                if (hits.isEmpty() && requireAllTokens) {
                    hits = runSearch(SimpleAnalyzer(), requireAllTokens = false)
                }
            }
            data class VerseHit(
                val book: Int,
                val chapter: Int,
                val verse: Int,
                val text: String
            )
            val verseHits = hits.map { hit ->
                val hitDoc = iSearcher.indexReader.storedFields().document(hit.doc)
                val book = hitDoc.getField("book")?.numericValue()?.toInt()!!
                val chapter = hitDoc.getField("chapter")?.numericValue()?.toInt()!!
                val verse = hitDoc.getField("verse")?.numericValue()?.toInt()!!
                val text = hitDoc.get("text") ?: ""

                VerseHit(book, chapter, verse, text)
            }
            val normalizedTerm = normalizeForContains(term)
            val exactHits = if (normalizedTerm.isNotBlank()) {
                verseHits.filter { normalizeForContains(it.text).contains(normalizedTerm) }
            } else {
                emptyList()
            }
            val fallbackHits = if (exactHits.isNotEmpty()) {
                exactHits
            } else {
                verseHits
            }
            return fallbackHits
                .sortedWith(compareBy<VerseHit>({ it.book }, { it.chapter }, { it.verse }))
                .take(verses)
                .map { hit ->
                    "${bookNameFor(bookNumber = hit.book, translation = translation)} ${hit.chapter}:${hit.verse} ${hit.text}"
                }
        }
    }

    private fun buildAnalyzedQuery(
        field: String,
        text: String,
        analyzer: Analyzer,
        requireAllTokens: Boolean
    ): Query? {
        if (text.isBlank()) return null
        val occur = if (requireAllTokens) BooleanClause.Occur.MUST else BooleanClause.Occur.SHOULD
        val builder = QueryBuilder(analyzer)
        return runCatching { builder.createBooleanQuery(field, text, occur) }.getOrNull()
    }

    private fun buildTermQueries(
        term: String,
        analyzer: Analyzer,
        requireAllTokens: Boolean
    ): List<Query> {
        if (term.isBlank()) return emptyList()
        val queries = ArrayList<Query>()
        val parser = QueryParser("text", analyzer)
        if (requireAllTokens) {
            parser.setDefaultOperator(QueryParser.Operator.AND)
        }
        runCatching { parser.parse(term) }.getOrNull()?.let { queries.add(it) }
        val builder = QueryBuilder(analyzer)
        if (requireAllTokens && term.any { it.isWhitespace() }) {
            runCatching { builder.createPhraseQuery("text", term) }.getOrNull()?.let { queries.add(it) }
        }
        buildAnalyzedQuery("text", term, analyzer, requireAllTokens)?.let { queries.add(it) }
        return queries.distinct()
    }

    private fun normalizeForContains(text: String): String {
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
            sb.append(mapped)
        }
        return sb.toString().lowercase()
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
