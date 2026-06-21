package org.gnit.bible.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.gnit.bible.*

class SearchCli(
    private val bible: Bible,
    private val processRunner: ProcessRunner = PlatformProcessRunner(),
    private val backendProvider: ((Translation) -> SearchBackend)? = null
) : CoreCliktCommand(name = "search") {

    override fun help(context: Context): String = """
        Search Bible text by word or exact phrase with book or category filters
        
        bbl search <terms> in [book|chapter|chapter range] in <translation> limit <count>
        
        bbl search Jesus Christ         search entire bible by terms
        bbl s Jesus Christ limit 3      specify number of search results
        bbl s Jesus Christ in kjv       search in other version of bible
        bbl s Jesus Christ in romans    filter by a book
        bbl s Jesus Christ in rom 3     filter by a chapter
        bbl s Jesus in rom 5-12         filter by chapter range
        bbl s Jesus in rom 5-12 in kjv  chapter range and in other bible
        bbl s Jesus in en de fr es ..   compare en result with 3+ languages
        bbl s jews gentiles in paul     filter by category i.e. set of books
        bbl s "Jesus wept"              exact search by double quotation
        bbl s "your faith" in gospels   exact search filtered by category
    """.trimIndent()

    private val termParts by argument(help = "search term").multiple()

    override fun run() {
        val (cleanTermParts, limitOverride) = SearchCliSupport.parseLimitOverride(termParts)
        val inlineFilters = SearchCliSupport.parseInlineFilters(normalizeLanguageScopes(cleanTermParts), bible)
        val term = SearchCliSupport.parseSearchTerm(inlineFilters.termParts)
        if (term.isBlank()) {
            throw UsageError("Missing search term")
        }

        val translations = SearchCliSupport.resolveTranslations(bible, null, inlineFilters.translationCodes)
        val translation = translations.first()
        val bookNumber = inlineFilters.bookNumber
        val (startChapter, endChapterValue) = SearchCliSupport.resolveChapterRange(
            bookNumber = bookNumber,
            explicitStartChapter = null,
            explicitEndChapter = null,
            inlineStartChapter = inlineFilters.startChapter,
            inlineEndChapter = inlineFilters.endChapter
        )

        val request = SearchRequest(
            term = term,
            translation = translation,
            bookNumber = bookNumber,
            startChapter = startChapter,
            endChapter = endChapterValue,
            verses = limitOverride ?: bible.searchResultFromSettings(),
            filters = inlineFilters.filters,
            categoryKeys = inlineFilters.categoryKeys
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
        BblHistory.record(bible, BblHistory.command("bbl search", termParts.joinToString(" ")))
    }

    private fun normalizeLanguageScopes(parts: List<String>): List<String> {
        var insideScope = false
        return parts.map { part ->
            if (part.equals("in", ignoreCase = true)) {
                insideScope = true
                part
            } else if (insideScope) {
                Language.parse(part)?.let(SupportedTranslation::defaultTranslationOf)?.code ?: part
            } else {
                part
            }
        }
    }
}
