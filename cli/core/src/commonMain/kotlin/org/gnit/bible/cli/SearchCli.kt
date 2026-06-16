package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.VersePointerJson

class SearchCli(
    private val bible: Bible,
    private val processRunner: ProcessRunner = PlatformProcessRunner(),
    private val backendProvider: ((Translation) -> SearchBackend)? = null
) : CoreCliktCommand(name = "search") {

    private val termParts by argument(help = "search term").multiple()
    private val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    private val book by option("-b", "--book", help = "book name or number")
    private val chapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val categoryKeys by option("--category", help = "category key").multiple()
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }

    override fun run() {
        val inlineFilters = SearchCliSupport.parseInlineFilters(termParts, bible)
        val term = SearchCliSupport.parseSearchTerm(inlineFilters.termParts)
        if (term.isBlank()) {
            throw UsageError("Missing search term")
        }

        val translations = SearchCliSupport.resolveTranslations(bible, translationCode, inlineFilters.translationCodes)
        val translation = translations.first()
        val bookNumber = SearchCliSupport.resolveBookNumber(book, inlineFilters.bookNumber)
        val (startChapter, endChapterValue) = SearchCliSupport.resolveChapterRange(
            bookNumber = bookNumber,
            explicitStartChapter = chapter,
            explicitEndChapter = endChapter,
            inlineStartChapter = inlineFilters.startChapter,
            inlineEndChapter = inlineFilters.endChapter
        )
        val (explicitCategoryKeys, explicitCategoryFilters) = SearchCliSupport.resolveCategoryFilters(categoryKeys)
        val requestCategoryKeys = (inlineFilters.categoryKeys + explicitCategoryKeys).distinct()
        val requestFilters = (inlineFilters.filters + explicitCategoryFilters).distinct()

        val request = SearchRequest(
            term = term,
            translation = translation,
            bookNumber = bookNumber,
            startChapter = startChapter,
            endChapter = endChapterValue,
            verses = verses ?: bible.searchResultFromSettings(),
            filters = requestFilters,
            categoryKeys = requestCategoryKeys
        )

        val backend = backendProvider ?: { requestedTranslation ->
            SearchBackendSelector(
                bible = bible,
                processRunner = processRunner
            ).backendFor(requestedTranslation)
        }

        val output = try {
            backend(translation).search(request)
        } catch (e: SearchBackendException) {
            throw UsageError(e.message ?: "Search failed")
        }

        val hits = try {
            VersePointerJson.decodeList(output.text)
        } catch (e: Exception) {
            throw UsageError("Search backend returned invalid VersePointer JSON: ${e.message ?: "unknown error"}")
        }

        if (hits.isNotEmpty()) {
            echo(SearchCliSupport.renderComparisonHits(bible, hits, translations))
        }
    }
}
