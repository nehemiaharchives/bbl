package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.Translation
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.GermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.SwedishAnalyzer

private class CommonHelperAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "en" -> EnglishAnalyzer()
            "es" -> SpanishAnalyzer()
            "pt" -> PortugueseAnalyzer()
            "de" -> GermanAnalyzer()
            "fr" -> FrenchAnalyzer()
            "ru" -> RussianAnalyzer()
            "nl" -> DutchAnalyzer()
            "it" -> ItalianAnalyzer()
            "sv" -> SwedishAnalyzer()
            else -> SimpleAnalyzer()
        }
    }
}

private class SearchHelperCli(
    private val bible: Bible
) : CliktCommand(name = "bbl-search-common") {

    private val termParts by argument(help = "search term").multiple()
    private val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    private val bookNumber by option("--book", help = "book number").convert { it.toInt() }
    private val startChapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
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
            echo(results.joinToString(separator = "\n"))
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
    // Helper binaries intentionally don't embed translations. They search installed packs.
    val bible = Bible(analyzerProvider = CommonHelperAnalyzerProvider())
    SearchHelperCli(bible).main(args)
}