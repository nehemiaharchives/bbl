package org.gnit.bible.cli

import com.github.ajalt.clikt.core.main
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.Language
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.morfologik.MorfologikAnalyzer
import org.gnit.lucenekmp.analysis.uk.ct.BibleUkrainianAnalyzer

class MorfologikAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) {
            when (language.code) {
                "pl" -> MorfologikAnalyzer()
                "uk" -> BibleUkrainianAnalyzer()
                else -> throw UnsupportedOperationException("MorfologikAnalyzerProvider only supports Polish and Ukrainian")
            }
        }
    }

    override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
        return  when {
            language == Language.uk && BibleUkrainianAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            else -> emptyList()
        }
    }
}

class SearchMorfologik(bible: Bible, analyzerProvider: AnalyzerProvider) : SearchHelperCli(
    searchHelperBinaryName = "bbl-search-morfologik",
    bible = bible,
    analyzerProvider = analyzerProvider
)

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchMorfologik(
        bible = Bible(),
        analyzerProvider = MorfologikAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}