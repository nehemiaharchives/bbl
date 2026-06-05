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
import org.gnit.bible.Translation
import org.gnit.bible.VersePointerJson
import org.gnit.bible.BblVersion
import org.gnit.bible.Books
import org.gnit.bible.SearchQueryText
import org.gnit.bible.LoggingSetup
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer

class SmartcnAnalyzerProvider : AnalyzerProvider {
    private var cached: Analyzer? = null

    override fun analyzerFor(language: Language): Analyzer {
        val existing = cached
        if (existing != null) return existing
        val created = SmartChineseAnalyzer()
        cached = created
        return created
    }
}

private const val searchHelperBinaryName = "bbl-search-smartcn"

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

    private fun resolveTranslationOrThrow(): Translation {
        val code = translationCode?.lowercase() ?: throw UsageError("Missing required option -t/--translation")
        if (!bible.findTranslationByCode(code)) {
            throw UsageError("Translation code '$code' not found (is the pack installed?)")
        }
        return bible.availableTranslations().first { it.code == code }
    }

    private fun validateAndResolveChapterRange(bookNumber: Int?): Pair<Int?, Int?> {
        val start = startChapter
        val end = endChapter
        if (start != null && bookNumber == null) throw UsageError("--chapter requires --book")
        if (end != null && bookNumber == null) throw UsageError("--end-chapter requires --book")
        if (end != null && start == null) throw UsageError("--end-chapter requires --chapter")
        if (start != null && end != null && end < start) throw UsageError("--end-chapter must be >= --chapter")
        if (verses <= 0) throw UsageError("--verses must be > 0")
        return start to end
    }
}

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    SearchHelperCli(
        bible = Bible(),
        analyzerProvider = SmartcnAnalyzerProvider()
    ).main(platformCommandLineArgs(args))
}
