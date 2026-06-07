package org.gnit.bible.cli

import com.github.ajalt.clikt.core.main
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.Language
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.ct.BibleBengaliAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.ct.BibleEnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.ct.BibleSpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.hi.ct.BibleHindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.ct.BiblePortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.ct.BibleRussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.ct.BibleTamilAnalyzer
import org.gnit.lucenekmp.analysis.te.ct.BibleTeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
        return when {
            language == Language.ru && BibleRussianAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            language == Language.sv && BibleSwedishAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            else -> emptyList()
        }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "en" -> BibleEnglishAnalyzer()
            "es" -> BibleSpanishAnalyzer()
            "pt" -> BiblePortugueseAnalyzer()
            "de" -> BibleGermanAnalyzer()
            "fr" -> FrenchAnalyzer()
            "ru" -> BibleRussianAnalyzer()
            "nl" -> DutchAnalyzer()
            "it" -> ItalianAnalyzer()
            "sv" -> BibleSwedishAnalyzer()
            "id" -> IndonesianAnalyzer()
            "th" -> ThaiAnalyzer()
            "hi" -> BibleHindiAnalyzer()
            "bn" -> BibleBengaliAnalyzer()
            "ta" -> BibleTamilAnalyzer()
            "te" -> BibleTeluguAnalyzer()
            "ne" -> BibleNepaliAnalyzer()
            else -> SimpleAnalyzer()
        }
    }
}

class SearchCommon(bible: Bible, analyzerProvider: AnalyzerProvider) : SearchHelperCli(
    searchHelperBinaryName = "bbl-search-common",
    bible = bible,
    analyzerProvider = analyzerProvider
)

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchCommon(
        bible = Bible(),
        analyzerProvider = CommonAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}
