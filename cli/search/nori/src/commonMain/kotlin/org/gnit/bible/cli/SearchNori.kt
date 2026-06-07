package org.gnit.bible.cli

import com.github.ajalt.clikt.core.main
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.ko.ct.BibleKoreanAnalyzer

class NoriAnalyzerProvider : AnalyzerProvider {
    private var cached: Analyzer? = null

    override fun analyzerFor(language: Language): Analyzer {
        val existing = cached
        if (existing != null) return existing
        val created = BibleKoreanAnalyzer()
        cached = created
        return created
    }
}

class SearchNori(bible: Bible, analyzerProvider: AnalyzerProvider) : SearchHelperCli(
    searchHelperBinaryName = "bbl-search-nori",
    bible = bible,
    analyzerProvider = analyzerProvider
)

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchNori(
        bible = Bible(),
        analyzerProvider = NoriAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}