package org.gnit.bible.cli

import com.github.ajalt.clikt.core.main
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.ja.ct.BibleJapaneseAnalyzer

class KuromojiAnalyzerProvider : AnalyzerProvider {
    private var cached: Analyzer? = null

    override fun analyzerFor(language: Language): Analyzer {
        val existing = cached
        if (existing != null) return existing
        val created = BibleJapaneseAnalyzer()
        cached = created
        return created
    }
}

class SearchKuromoji(bible: Bible, analyzerProvider: AnalyzerProvider) : SearchHelperCli(
    searchHelperBinaryName = "bbl-search-kuromoji",
    bible = bible,
    analyzerProvider = analyzerProvider
)

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchKuromoji(
        bible = Bible(),
        analyzerProvider = KuromojiAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}
