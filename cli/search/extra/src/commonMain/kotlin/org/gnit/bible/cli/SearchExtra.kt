package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.VersePointerJson
import org.gnit.bible.BblVersion
import org.gnit.bible.Books
import org.gnit.bible.SearchQueryText
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

private const val searchHelperBinaryName = "bbl-search-extra"

internal class SearchHelperCli(
    private val bible: Bible,
    val analyzerProvider: AnalyzerProvider
) : CoreCliktCommand(name = searchHelperBinaryName) {

    private val termParts by argument(help = "search term").multiple()
    private val versionFlag by option("-v", "--version", help = "prints out software version of this program").flag()
    private val artifactCompatibilityVersionFlag by option("--artifact-compat-version", help = "prints out bbl artifact compatibility version").flag()
    private val translationCode by option("-t", "--translation", help = "translation code")
    private val bookNumber by option("--book", help = "book number").convert { it.toInt() }
    private val startChapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val categoryKeys by option("--category", help = "category key").multiple()
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
        if (versionFlag) {
            echo(BblVersion.searchHelperVersionLine(searchHelperBinaryName))
            return
        }

        if (artifactCompatibilityVersionFlag) {
            echo(BblVersion.artifactCompatibilityVersionLine())
            return
        }

        val term = SearchQueryText.searchTermFromArgs(termParts)
        if (term.isBlank()) throw UsageError("Missing search term")

        val translation = resolveTranslationOrThrow()
        val (start, end) = validateAndResolveChapterRange(bookNumber)
        val filters = Books.Category.resolveAllOrThrow(categoryKeys) {
            UsageError("Category key '$it' not found. Run 'bbl list categories' to see supported category names.")
        }

        val results = bible.search(
            term = term,
            bookNumber = bookNumber,
            startChapter = start,
            endChapter = end,
            verses = verses,
            filters = filters,
            translation = translation,
            analyzerProvider = analyzerProvider
        )

        if (results.isNotEmpty()) {
            echo(VersePointerJson.encodeList(results))
        }
    }

    private fun resolveTranslationOrThrow() = (translationCode ?: throw UsageError("Missing translation code")).let { code ->
        org.gnit.bible.SupportedTranslation.entries.find { it.translation.code == code }?.translation
            ?: throw UsageError("Translation '$code' not found or not supported by this search helper.")
    }

    private fun validateAndResolveChapterRange(bookNumber: Int?): Pair<Int?, Int?> {
        if (bookNumber == null && (startChapter != null || endChapter != null)) {
            throw UsageError("Chapter options require --book")
        }
        return startChapter to endChapter
    }
}

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchHelperCli(
        bible = Bible(),
        analyzerProvider = ExtraAnalyzerProvider()
    ).main(args)
}
