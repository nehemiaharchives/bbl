package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.Translation
import org.gnit.bible.VersePointerJson
import org.gnit.bible.bblSearchHelperArtifactCompatibilityVersionLine
import org.gnit.bible.bblSearchHelperVersionLine
import org.gnit.bible.suppressKotlinLoggingStartupMessage
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.hi.HindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.ct.BibleTamilAnalyzer
import org.gnit.lucenekmp.analysis.te.TeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            // search common (embedded in cmp)
            "en" -> EnglishAnalyzer()
            "es" -> SpanishAnalyzer()
            "pt" -> PortugueseAnalyzer()
            "de" -> BibleGermanAnalyzer()
            "fr" -> FrenchAnalyzer()
            "ru" -> RussianAnalyzer()
            "nl" -> DutchAnalyzer()
            "it" -> ItalianAnalyzer()
            "sv" -> BibleSwedishAnalyzer()

            // search common (downloadable in cmp)
            "id" -> IndonesianAnalyzer()
            "th" -> ThaiAnalyzer()
            "hi" -> HindiAnalyzer()
            "bn" -> BengaliAnalyzer()
            "ta" -> BibleTamilAnalyzer()
            "te" -> TeluguAnalyzer()
            "ne" -> BibleNepaliAnalyzer()
            else -> SimpleAnalyzer()
        }
    }

}

private const val searchHelperBinaryName = "bbl-search-common"

private class SearchHelperCli(
    private val bible: Bible
) : CliktCommand(name = searchHelperBinaryName) {

    private val termParts by argument(help = "search term").multiple()
    private val versionFlag by option("-v", "--version", help = "prints out software version of this program").flag()
    private val artifactCompatibilityVersionFlag by option("--artifact-compat-version", help = "prints out bbl artifact compatibility version").flag()
    private val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    private val bookNumber by option("--book", help = "book number").convert { it.toInt() }
    private val startChapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
        if (versionFlag) {
            echo(bblSearchHelperVersionLine(searchHelperBinaryName))
            return
        }

        if (artifactCompatibilityVersionFlag) {
            echo(bblSearchHelperArtifactCompatibilityVersionLine())
            return
        }

        val term = termParts.joinToString(separator = " ").trim()
        if (term.isBlank()) throw UsageError("Missing search term")

        val translation = resolveTranslationOrThrow()
        val (start, end) = validateAndResolveChapterRange(bookNumber)

        val results = bible.search(
            term = term,
            bookNumber = bookNumber,
            startChapter = start,
            endChapter = end,
            verses = verses,
            translation = translation
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
    suppressKotlinLoggingStartupMessage()
    val bible = Bible(analyzerProvider = CommonAnalyzerProvider())
    SearchHelperCli(bible).main(args)
}
