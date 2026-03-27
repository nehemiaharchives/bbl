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
import org.gnit.lucenekmp.search.TermQuery
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import org.gnit.lucenekmp.store.IOContext
import org.gnit.lucenekmp.analysis.tokenattributes.CharTermAttribute
import org.gnit.lucenekmp.analysis.tokenattributes.TermToBytesRefAttribute
import org.gnit.lucenekmp.util.QueryBuilder
import org.gnit.lucenekmp.index.Term
import org.gnit.lucenekmp.index.MultiTerms
import org.gnit.lucenekmp.analysis.standard.StandardAnalyzer
import org.gnit.lucenekmp.index.IndexReader
import org.gnit.lucenekmp.index.TermsEnum
import org.gnit.lucenekmp.search.ScoreDoc
import org.gnit.lucenekmp.util.BytesRef

/**
 * in `cli` SearchEngine is called following split binaries:
 * 1. `bbl-search-common`
 * 2. `bbl-search-morfologik`
 * 3. `bbl-search-smartcn`
 * 4. `bbl-search-nori`
 * 5. `bbl-search-kuromoji`
 * 6. `bbl-search-extra`
 * Each search binary depends on corresponding analyzer library e.g. `bbl-search-common` and `lucene-kmp-analysis-common` to reduce binary size.
 * So one `AnalyzerProvider` will be stored in `SearchEngine`
 *
 * in `composeApp`, SearchEngine has all analyzers and does not care about size
 * because in KMP app, it is impossible to split and dynamically download/load components like osgi.
 * Android can do that but iOS app does not have such things.
 * So a single `AnalyzerProvider` can cover all languages in one place
 *
 */
class SearchEngine(
    private val reader: BibleResourcesReader,
    private val analyzerProvider: AnalyzerProvider
) {
    private val languageAnalyserCache: MutableMap<String, Analyzer> = mutableMapOf()

    private fun analyzerFor(translation: Translation): Analyzer {
        return languageAnalyserCache.getOrPut(translation.language.code) {
            analyzerProvider.analyzerFor(translation.language)
        }
    }

    private val logger = KotlinLogging.logger {}
    private val directoriesByTranslation = HashMap<String, ByteBuffersDirectory>()

    private fun analyzeTokens(
        analyzer: Analyzer,
        field: String,
        text: String,
        maxTokens: Int = 20
    ): List<String> {
        val tokens = ArrayList<String>()
        val stream = analyzer.tokenStream(field, text)
        stream.use {
            val termAtt = it.addAttribute(CharTermAttribute::class)
            it.reset()
            while (it.incrementToken() && tokens.size < maxTokens) {
                tokens.add(termAtt.toString())
            }
            it.end()
        }
        return tokens
    }

    private fun sampleIndexTerms(reader: IndexReader, field: String, maxTerms: Int = 20): List<String> {
        val terms = MultiTerms.getTerms(reader, field) ?: return emptyList()
        val enum = terms.iterator()
        val samples = ArrayList<String>(maxTerms)
        while (samples.size < maxTerms) {
            val term = enum.next() ?: break
            samples.add(term.utf8ToString())
        }
        return samples
    }

    private fun sampleNonAsciiTerms(
        reader: IndexReader,
        field: String,
        maxTerms: Int = 10,
        maxScans: Int = 20000
    ): List<String> {
        val terms = MultiTerms.getTerms(reader, field) ?: return emptyList()
        val enum = terms.iterator()
        val samples = ArrayList<String>(maxTerms)
        var scanned = 0
        while (samples.size < maxTerms && scanned < maxScans) {
            val term = enum.next() ?: break
            val text = term.utf8ToString()
            if (text.any { it.code > 127 }) {
                samples.add(text)
            }
            scanned++
        }
        return samples
    }

    private fun seekCeilTerm(reader: IndexReader, field: String, target: String): String {
        val terms = MultiTerms.getTerms(reader, field) ?: return "missing"
        val enum = terms.iterator()
        val status = enum.seekCeil(BytesRef(target))
        val term = if (status == TermsEnum.SeekStatus.END) null else enum.term()?.utf8ToString()
        return "$status:${term ?: "null"}"
    }

    private fun analyzeTokenExistence(
        reader: IndexReader,
        analyzer: Analyzer,
        field: String,
        text: String,
        maxTokens: Int = 10
    ): Map<String, Boolean> {
        val terms = MultiTerms.getTerms(reader, field) ?: return emptyMap()
        val enum = terms.iterator()
        val results = LinkedHashMap<String, Boolean>()
        val stream = analyzer.tokenStream(field, text)
        stream.use {
            val termAtt = it.addAttribute(CharTermAttribute::class)
            val bytesAtt = it.addAttribute(TermToBytesRefAttribute::class)
            it.reset()
            while (it.incrementToken() && results.size < maxTokens) {
                val token = termAtt.toString()
                val exists = runCatching { enum.seekExact(bytesAtt.bytesRef) }.getOrDefault(false)
                results[token] = exists
            }
            it.end()
        }
        return results
    }


    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ): List<VersePointer> {
        // Some unit-test fixtures only include a placeholder codec file and no segments_N.
        // Without a segments* file Lucene can't open the directory.
        val fileNames = reader.listIndexFiles(translation.code)
        if (fileNames.none { it.startsWith("segments") }) {
            logger.debug { "no segments* file found for ${translation.code}; returning empty search result" }
            return emptyList()
        }

        val directory = embeddedIndexDirectory(translation)

        val iReader: DirectoryReader = StandardDirectoryReader.open(directory, commit = null, leafSorter = null)

        iReader.use { reader ->
            val iSearcher = IndexSearcher(reader)
            val analyzer = analyzerFor(translation)

            val hits = when (translation.language) {
                Language.zh, Language.ja, Language.ko -> cjkSearchPreProcess(
                    indexSearcher = iSearcher,
                    analyzer = analyzer,
                    term = term,
                    bookNumber = bookNumber,
                    startChapter = startChapter,
                    endChapter = endChapter,
                    verses = verses,
                    translation = translation
                )
                Language.th -> thaiSearchPreProcess(
                    indexSearcher = iSearcher,
                    analyzer = analyzer,
                    term = term,
                    bookNumber = bookNumber,
                    startChapter = startChapter,
                    endChapter = endChapter,
                    verses = verses,
                    translation = translation
                )
                Language.hi -> hindiSearchPreProcess(
                    indexSearcher = iSearcher,
                    analyzer = analyzer,
                    term = term,
                    bookNumber = bookNumber,
                    startChapter = startChapter,
                    endChapter = endChapter,
                    verses = verses,
                    translation = translation
                )
                else -> defaultSearchPreProcess(
                    indexSearcher = iSearcher,
                    analyzer = analyzer,
                    term = term,
                    bookNumber = bookNumber,
                    startChapter = startChapter,
                    endChapter = endChapter,
                    verses = verses,
                    translation = translation
                )
            }


            data class VerseHit(
                val book: Int,
                val chapter: Int,
                val verse: Int,
            )

            val verseHits = hits.map { hit ->
                val hitDoc = iSearcher.indexReader.storedFields().document(hit.doc)
                val book = hitDoc.getField("book")?.numericValue()?.toInt()!!
                val chapter = hitDoc.getField("chapter")?.numericValue()?.toInt()!!
                val verse = hitDoc.getField("verse")?.numericValue()?.toInt()!!
                VerseHit(book, chapter, verse)
            }

            return verseHits
                .sortedWith(compareBy<VerseHit>({ it.book }, { it.chapter }, { it.verse }))
                .take(verses)
                .map { hit ->
                    VersePointer(
                        translation = translation,
                        book = hit.book,
                        chapter = hit.chapter,
                        startVerse = hit.verse,
                        endVerse = null,
                    )
                }
        }
    }

    private fun buildFilterClauses(
        bookNumber: Int?,
        startChapter: Int?,
        endChapter: Int?
    ): MutableList<Pair<Query, BooleanClause.Occur>> {
        val filterClauses = mutableListOf<Pair<Query, BooleanClause.Occur>>()
        if (bookNumber != null) {
            filterClauses.add(IntPoint.newExactQuery("book", bookNumber) to BooleanClause.Occur.MUST)
            if (startChapter != null) {
                filterClauses.add(chapterQuery(startChapter, endChapter) to BooleanClause.Occur.MUST)
            }
        }
        return filterClauses
    }

    private fun buildQuery(
        filterClauses: List<Pair<Query, BooleanClause.Occur>>,
        termQuery: Query
    ): Query {
        val builder = BooleanQuery.Builder()
        filterClauses.forEach { (query, occur) -> builder.add(query, occur) }
        builder.add(termQuery, BooleanClause.Occur.MUST)
        return builder.build()
    }

    private fun runSearchQueries(
        indexSearcher: IndexSearcher,
        indexReader: IndexReader,
        filterClauses: List<Pair<Query, BooleanClause.Occur>>,
        analyzer: Analyzer,
        term: String,
        requireAllTokens: Boolean,
        maxHits: Int,
        sortByDoc: Boolean
    ): Array<ScoreDoc> {
        val queries = buildTermQueries(term, analyzer, requireAllTokens)
        if (queries.isEmpty()) return emptyArray()
        val merged = LinkedHashMap<Int, ScoreDoc>()
        for (queryTerm in queries) {
            val query = buildQuery(filterClauses, queryTerm)
            val hits = if (sortByDoc) {
                indexSearcher.search(query, maxHits, Sort(SortField.FIELD_DOC)).scoreDocs
            } else {
                indexSearcher.search(query, maxHits).scoreDocs
            }
            for (hit in hits) {
                if (!merged.containsKey(hit.doc)) {
                    merged[hit.doc] = hit
                }
            }
        }
        return merged.values.toTypedArray()
    }

    private fun runSingleQuery(
        indexSearcher: IndexSearcher,
        filterClauses: List<Pair<Query, BooleanClause.Occur>>,
        termQuery: Query,
        maxHits: Int,
        sortByDoc: Boolean
    ): Array<ScoreDoc> {
        val query = buildQuery(filterClauses, termQuery)
        return if (sortByDoc) {
            indexSearcher.search(query, maxHits, Sort(SortField.FIELD_DOC)).scoreDocs
        } else {
            indexSearcher.search(query, maxHits).scoreDocs
        }
    }

    private fun runSearchPipeline(
        indexSearcher: IndexSearcher,
        indexReader: IndexReader,
        filterClauses: List<Pair<Query, BooleanClause.Occur>>,
        analyzer: Analyzer,
        term: String,
        requireAllTokens: Boolean,
        verses: Int,
        allowRelaxed: Boolean = true,
        allowSimpleAnalyzer: Boolean = true,
        sortByDoc: Boolean = true
    ): Array<ScoreDoc> {
        var hits = runSearchQueries(
            indexSearcher = indexSearcher,
            indexReader = indexReader,
            filterClauses = filterClauses,
            analyzer = analyzer,
            term = term,
            requireAllTokens = requireAllTokens,
            maxHits = verses,
            sortByDoc = sortByDoc
        )
        if (hits.isEmpty() && allowRelaxed && requireAllTokens) {
            hits = runSearchQueries(
                indexSearcher = indexSearcher,
                indexReader = indexReader,
                filterClauses = filterClauses,
                analyzer = analyzer,
                term = term,
                requireAllTokens = false,
                maxHits = verses,
                sortByDoc = sortByDoc
            )
        }
        if (hits.isEmpty() && allowSimpleAnalyzer && analyzer !is SimpleAnalyzer) {
            hits = runSearchQueries(
                indexSearcher = indexSearcher,
                indexReader = indexReader,
                filterClauses = filterClauses,
                analyzer = SimpleAnalyzer(),
                term = term,
                requireAllTokens = requireAllTokens,
                maxHits = verses,
                sortByDoc = sortByDoc
            )
            if (hits.isEmpty() && allowRelaxed && requireAllTokens) {
                hits = runSearchQueries(
                    indexSearcher = indexSearcher,
                    indexReader = indexReader,
                    filterClauses = filterClauses,
                    analyzer = SimpleAnalyzer(),
                    term = term,
                    requireAllTokens = false,
                    maxHits = verses,
                    sortByDoc = sortByDoc
                )
            }
        }
        return hits
    }

    private fun buildRequireExistingTokenQuery(
        reader: IndexReader,
        analyzer: Analyzer,
        field: String,
        text: String,
        maxTokens: Int = 10
    ): Query? {
        if (text.isBlank()) return null
        val builder = BooleanQuery.Builder()
        var added = 0
        val stream = analyzer.tokenStream(field, text)
        stream.use {
            val bytesAtt = it.addAttribute(TermToBytesRefAttribute::class)
            it.reset()
            while (it.incrementToken() && added < maxTokens) {
                val tokenTerm = Term(field, bytesAtt.bytesRef)
                val freq = runCatching { reader.docFreq(tokenTerm) }.getOrDefault(0)
                if (freq > 0) {
                    builder.add(TermQuery(tokenTerm), BooleanClause.Occur.MUST)
                    added++
                }
            }
            it.end()
        }
        return if (added == 0) null else builder.build()
    }

    private fun defaultSearchPreProcess(
        indexSearcher: IndexSearcher,
        analyzer: Analyzer,
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ):  Array<ScoreDoc>{
        logger.debug {
            "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
        }
        val filterClauses = buildFilterClauses(bookNumber, startChapter, endChapter)
        val requireAllTokens = term.any { it.isWhitespace() }
        return runSearchPipeline(
            indexSearcher = indexSearcher,
            indexReader = indexSearcher.indexReader,
            filterClauses = filterClauses,
            analyzer = analyzer,
            term = term,
            requireAllTokens = requireAllTokens,
            verses = verses
        )
    }

    private fun cjkSearchPreProcess(
        indexSearcher: IndexSearcher,
        analyzer: Analyzer,
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ):  Array<ScoreDoc>{
        logger.debug {
            "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
        }
        val filterClauses = buildFilterClauses(bookNumber, startChapter, endChapter)
        val normalizedTerm = term.filterNot { it.isWhitespace() }
        if (normalizedTerm.isNotBlank()) {
            val phraseText = if (term.any { it.isWhitespace() }) normalizedTerm else term
            val phraseQuery = QueryBuilder(analyzer).createPhraseQuery("text", phraseText)!!
            val hits = runSingleQuery(
                indexSearcher = indexSearcher,
                filterClauses = filterClauses,
                termQuery = phraseQuery,
                maxHits = verses,
                sortByDoc = true
            )
            if (hits.isNotEmpty()) {
                return hits
            }
        }
        val requireAllTokens = term.any { it.isWhitespace() }
        val primaryQuery = buildAnalyzedQuery("text", term, analyzer, requireAllTokens)
            ?: runCatching {
                val parser = QueryParser("text", analyzer)
                if (requireAllTokens) {
                    parser.setDefaultOperator(QueryParser.Operator.AND)
                }
                parser.parse(term)
            }.getOrNull()
        if (primaryQuery != null) {
            val hits = runSingleQuery(
                indexSearcher = indexSearcher,
                filterClauses = filterClauses,
                termQuery = primaryQuery,
                maxHits = verses,
                sortByDoc = true
            )
            if (hits.isNotEmpty()) {
                return hits
            }
        }
        if (requireAllTokens) {
            val relaxedQuery = buildAnalyzedQuery("text", term, analyzer, requireAllTokens = false)
            if (relaxedQuery != null) {
                return runSingleQuery(
                    indexSearcher = indexSearcher,
                    filterClauses = filterClauses,
                    termQuery = relaxedQuery,
                    maxHits = verses,
                    sortByDoc = true
                )
            }
        }
        return emptyArray()
    }

    private fun thaiSearchPreProcess(
        indexSearcher: IndexSearcher,
        analyzer: Analyzer,
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ):  Array<ScoreDoc>{
        logger.debug {
            "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
        }
        val filterClauses = buildFilterClauses(bookNumber, startChapter, endChapter)
        var hits = runSearchPipeline(
            indexSearcher = indexSearcher,
            indexReader = indexSearcher.indexReader,
            filterClauses = filterClauses,
            analyzer = analyzer,
            term = term,
            requireAllTokens = true,
            verses = verses,
            allowRelaxed = false
        )
        if (hits.isEmpty()) {
            val fallbackQuery = buildRequireExistingTokenQuery(
                reader = indexSearcher.indexReader,
                analyzer = analyzer,
                field = "text",
                text = term
            )
            if (fallbackQuery != null) {
                hits = runSingleQuery(
                    indexSearcher = indexSearcher,
                    filterClauses = filterClauses,
                    termQuery = fallbackQuery,
                    maxHits = verses,
                    sortByDoc = true
                )
            }
        }
        if (hits.isEmpty()) {
            hits = runSearchPipeline(
                indexSearcher = indexSearcher,
                indexReader = indexSearcher.indexReader,
                filterClauses = filterClauses,
                analyzer = analyzer,
                term = term,
                requireAllTokens = false,
                verses = verses
            )
        }
        return hits
    }


    private fun hindiSearchPreProcess(
        indexSearcher: IndexSearcher,
        analyzer: Analyzer,
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ):  Array<ScoreDoc>{
        logger.debug {
            "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
        }
        val filterClauses = buildFilterClauses(bookNumber, startChapter, endChapter)
        val requireAllTokens = term.any { it.isWhitespace() }
        val hits = runSearchPipeline(
            indexSearcher = indexSearcher,
            indexReader = indexSearcher.indexReader,
            filterClauses = filterClauses,
            analyzer = analyzer,
            term = term,
            requireAllTokens = requireAllTokens,
            verses = verses
        )
        if (hits.isEmpty()) {
            val tokens = analyzeTokens(analyzer, "text", term)
            val tokenFreqs = tokens.associateWith { token ->
                runCatching { indexSearcher.indexReader.docFreq(Term("text", token)) }.getOrDefault(-1)
            }
            val sampleTerms = sampleIndexTerms(indexSearcher.indexReader, "text")
            val nonAsciiTerms = sampleNonAsciiTerms(indexSearcher.indexReader, "text")
            val seekYish = seekCeilTerm(indexSearcher.indexReader, "text", "यिश")
            val seekMasih = seekCeilTerm(indexSearcher.indexReader, "text", "मसिह")
            val seekYeeshu = seekCeilTerm(indexSearcher.indexReader, "text", "यीशु")
            val tokenExists = analyzeTokenExistence(indexSearcher.indexReader, analyzer, "text", term)
            val standardAnalyzer = StandardAnalyzer()
            val standardTokens = analyzeTokens(standardAnalyzer, "text", term)
            val standardExists = analyzeTokenExistence(indexSearcher.indexReader, standardAnalyzer, "text", term)
            logger.debug {
                "no hits for '$term' in ${translation.code} using ${analyzer::class.simpleName}; tokens=$tokens docFreqs=$tokenFreqs tokenExists=$tokenExists standardTokens=$standardTokens standardExists=$standardExists sampleTerms=$sampleTerms nonAsciiTerms=$nonAsciiTerms seekYish=$seekYish seekMasih=$seekMasih seekYeeshu=$seekYeeshu"
            }
        }
        return hits
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
    }
}
