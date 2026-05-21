package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.index.DirectoryReader
import org.gnit.lucenekmp.index.StandardDirectoryReader
import org.gnit.lucenekmp.queryparser.classic.QueryParser
import org.gnit.lucenekmp.search.BooleanClause
import org.gnit.lucenekmp.search.BooleanQuery
import org.gnit.lucenekmp.search.IndexSearcher
import org.gnit.lucenekmp.search.MatchAllDocsQuery
import org.gnit.lucenekmp.search.Query
import org.gnit.lucenekmp.search.ScoreDoc
import org.gnit.lucenekmp.search.Sort
import org.gnit.lucenekmp.search.SortField
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import org.gnit.lucenekmp.store.IOContext

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

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        filter: BibleFilter,
        translation: Translation,
    ): List<VersePointer>{
        return search(term, bookNumber, startChapter, endChapter, verses, listOf(filter), translation)
    }

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        filters: List<BibleFilter> = emptyList(),
        translation: Translation,
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
            val hits = searchIndex(
                indexSearcher = iSearcher,
                analyzer = analyzer,
                term = term,
                bookNumber = bookNumber,
                startChapter = startChapter,
                endChapter = endChapter,
                verses = verses,
                translation = translation,
                filters = filters
            )

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

    private fun searchIndex(
        indexSearcher: IndexSearcher,
        analyzer: Analyzer,
        term: String,
        bookNumber: Int?,
        startChapter: Int?,
        endChapter: Int?,
        verses: Int,
        translation: Translation,
        filters: List<BibleFilter>
    ): Array<ScoreDoc> {
        if (term.isBlank()) return emptyArray()
        logger.debug {
            "searching $term ${if (bookNumber != null) "in ${bookNameEnglishCapital(bookNumber)} " else " "}in $translation"
        }

        val parser = QueryParser("text", analyzer)
        parser.setDefaultOperator(QueryParser.Operator.AND)
        val termQuery = parser.parse(normalizeWordInternalHyphens(term)) ?: return emptyArray()
        val filterClauses = buildFilterClauses(bookNumber, startChapter, endChapter)
        filters.forEach { filter ->
            filterClauses.add(filterQuery(filter) to BooleanClause.Occur.MUST)
        }
        if (bookNumber == null && filters.isEmpty()) {
            analyzerProvider.bibleFiltersFor(translation.language, term).forEach { filter ->
                filterClauses.add(filterQuery(filter) to BooleanClause.Occur.MUST)
            }
        }
        val query = buildQuery(filterClauses, termQuery)
        return indexSearcher.search(query, verses, Sort(SortField.FIELD_DOC)).scoreDocs
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

    private fun filterQuery(filter: BibleFilter): Query {
        return when (filter) {
            BibleFilter.All -> MatchAllDocsQuery()
            is BibleFilter.BookRange -> {
                val range = filter.range
                IntPoint.newRangeQuery("book", range.first, range.last)
            }
            is BibleFilter.BookSet -> IntPoint.newSetQuery("book", filter.books.toMutableList())
            is BibleFilter.Passage -> passageQuery(filter.start, filter.endInclusive)
            is BibleFilter.Union -> {
                val builder = BooleanQuery.Builder()
                filter.filters.forEach { child ->
                    builder.add(filterQuery(child), BooleanClause.Occur.SHOULD)
                }
                builder.build()
            }
        }
    }

    private fun passageQuery(start: BookChapterVerse, endInclusive: BookChapterVerse): Query {
        if (start.book == endInclusive.book) {
            return bookPassageQuery(start.book, start.chapter, start.verse, endInclusive.chapter, endInclusive.verse)
        }

        val builder = BooleanQuery.Builder()
        builder.add(
            bookPassageQuery(start.book, start.chapter, start.verse, Books.maxChapter(start.book), Int.MAX_VALUE),
            BooleanClause.Occur.SHOULD
        )
        if (start.book + 1 <= endInclusive.book - 1) {
            builder.add(IntPoint.newRangeQuery("book", start.book + 1, endInclusive.book - 1), BooleanClause.Occur.SHOULD)
        }
        builder.add(
            bookPassageQuery(endInclusive.book, 1, 1, endInclusive.chapter, endInclusive.verse),
            BooleanClause.Occur.SHOULD
        )
        return builder.build()
    }

    private fun bookPassageQuery(
        book: Int,
        startChapter: Int,
        startVerse: Int,
        endChapter: Int,
        endVerse: Int
    ): Query {
        val builder = BooleanQuery.Builder()
        builder.add(IntPoint.newExactQuery("book", book), BooleanClause.Occur.MUST)

        if (startChapter == endChapter) {
            builder.add(IntPoint.newExactQuery("chapter", startChapter), BooleanClause.Occur.MUST)
            builder.add(IntPoint.newRangeQuery("verse", startVerse, endVerse), BooleanClause.Occur.MUST)
            return builder.build()
        }

        val chapterBuilder = BooleanQuery.Builder()
        chapterBuilder.add(chapterVerseRangeQuery(startChapter, startVerse, Int.MAX_VALUE), BooleanClause.Occur.SHOULD)
        if (startChapter + 1 <= endChapter - 1) {
            chapterBuilder.add(IntPoint.newRangeQuery("chapter", startChapter + 1, endChapter - 1), BooleanClause.Occur.SHOULD)
        }
        chapterBuilder.add(chapterVerseRangeQuery(endChapter, 1, endVerse), BooleanClause.Occur.SHOULD)
        builder.add(chapterBuilder.build(), BooleanClause.Occur.MUST)
        return builder.build()
    }

    private fun chapterVerseRangeQuery(chapter: Int, startVerse: Int, endVerse: Int): Query {
        val builder = BooleanQuery.Builder()
        builder.add(IntPoint.newExactQuery("chapter", chapter), BooleanClause.Occur.MUST)
        builder.add(IntPoint.newRangeQuery("verse", startVerse, endVerse), BooleanClause.Occur.MUST)
        return builder.build()
    }

    private fun normalizeWordInternalHyphens(term: String): String {
        val builder = StringBuilder(term.length)
        var replaced = false
        term.forEachIndexed { index, char ->
            if (char == '-' && index > 0 && index < term.lastIndex &&
                term[index - 1].isLetterOrDigit() && term[index + 1].isLetterOrDigit()
            ) {
                builder.append(' ')
                replaced = true
            } else {
                builder.append(char)
            }
        }
        val normalized = builder.toString()
        return if (replaced && '"' !in normalized) {
            "\"$normalized\""
        } else {
            normalized
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
    }
}

fun searchTermFromArgs(args: List<String>): String {
    if (args.isEmpty()) return ""

    val term = args.joinToString(separator = " ").trim()
    if (args.size == 1 && term.isNotEmpty() && term.any(Char::isWhitespace) && !isQuotedTerm(term)) {
        return "\"$term\""
    }
    return term
}

private fun isQuotedTerm(term: String): Boolean {
    return term.length >= 2 && term.first() == '"' && term.last() == '"'
}
