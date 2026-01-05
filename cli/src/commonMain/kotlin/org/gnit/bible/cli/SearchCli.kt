package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.bookNumber

class SearchCli(
    private val bible: Bible,
    private val processRunner: ProcessRunner = PlatformProcessRunner()
) : CliktCommand(name = "search") {

    private val termParts by argument(help = "search term").multiple()
    private val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    private val book by option("-b", "--book", help = "book name or number")
    private val chapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
        val term = termParts.joinToString(separator = " ").trim()
        if (term.isBlank()) {
            throw UsageError("Missing search term")
        }

        val translation = resolveTranslation()
        val bookNumber = resolveBookNumber()
        val (startChapter, endChapterValue) = resolveChapterRange(bookNumber)

        val request = SearchRequest(
            term = term,
            translation = translation,
            bookNumber = bookNumber,
            startChapter = startChapter,
            endChapter = endChapterValue,
            verses = verses
        )

        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = processRunner
        )

        val output = try {
            selector.backendFor(translation.language).search(request)
        } catch (e: SearchBackendException) {
            throw UsageError(e.message ?: "Search failed")
        }

        if (output.text.isNotBlank()) {
            echo(output.text)
        }
    }

    private fun resolveTranslation(): Translation {
        val code = translationCode?.lowercase()
            ?: bible.defaultTranslationFromSettings().code
        if (!bible.findTranslationByCode(code)) {
            throw UsageError("Translation code '$code' not found")
        }
        return bible.availableTranslations().first { it.code == code }
    }

    private fun resolveBookNumber(): Int? {
        val raw = book ?: return null
        val asInt = raw.toIntOrNull()
        if (asInt != null) {
            return asInt
        }
        return try {
            bookNumber(raw.lowercase())
        } catch (_: Exception) {
            throw UsageError("Unknown book '$raw'. Run 'bbl list books' to see supported book names.")
        }
    }

    private fun resolveChapterRange(bookNumber: Int?): Pair<Int?, Int?> {
        val start = chapter
        val end = endChapter
        if (start != null && bookNumber == null) {
            throw UsageError("--chapter requires --book")
        }
        if (end != null && bookNumber == null) {
            throw UsageError("--end-chapter requires --book")
        }
        if (end != null && start == null) {
            throw UsageError("--end-chapter requires --chapter")
        }
        if (start != null && end != null && end < start) {
            throw UsageError("--end-chapter must be >= --chapter")
        }
        return Pair(start, end)
    }
}
