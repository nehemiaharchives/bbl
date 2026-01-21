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
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.mr.MarathiAnalyzer
import org.gnit.lucenekmp.analysis.tl.TagalogAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseConfig

private class ExtraAnalyzerProvider : AnalyzerProvider {

    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "tl" -> TagalogAnalyzer()
            "vi" -> VietnameseAnalyzer(VietnameseConfig())
            "mr" -> MarathiAnalyzer()
            "gu" -> GujaratiAnalyzer()
            "ur" -> UrduAnalyzer()
            else -> throw UnsupportedOperationException("language $code is not supported by ExtraAnalyzerProvider")
        }
    }
}

private class SearchHelperCli(
    private val bible: Bible
) : CliktCommand(name = "bbl-search-extra") {

    private val termParts by argument(help = "search term").multiple()
    private val translationCode by option("-t", "--translation", help = "translation code")
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
    val bible = Bible(analyzerProvider = ExtraAnalyzerProvider())
    SearchHelperCli(bible).main(args)
}