package org.gnit.bible.cli

import com.github.ajalt.clikt.core.main
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.mr.ct.BibleMarathiAnalyzer
import org.gnit.lucenekmp.analysis.tl.ct.BibleTagalogAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.ct.BibleVietnameseAnalyzer

class ExtraAnalyzerProvider : AnalyzerProvider {

    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "tl" -> BibleTagalogAnalyzer()
            "vi" -> BibleVietnameseAnalyzer()
            "gu" -> GujaratiAnalyzer()
            "mr" -> BibleMarathiAnalyzer()
            "ur" -> UrduAnalyzer()
            else -> throw UnsupportedOperationException("language $code is not supported by ExtraAnalyzerProvider")
        }
    }
}

class SearchExtra(bible: Bible, analyzerProvider: AnalyzerProvider) : SearchHelperCli(
    searchHelperBinaryName = "bbl-search-extra",
    bible = bible,
    analyzerProvider = analyzerProvider
)

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchExtra(
        bible = Bible(),
        analyzerProvider = ExtraAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}
