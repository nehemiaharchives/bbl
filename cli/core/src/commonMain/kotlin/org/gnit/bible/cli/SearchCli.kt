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
import org.gnit.bible.VersePointer
import org.gnit.bible.VersePointerJson
import org.gnit.bible.bookNumber
import org.gnit.bible.formatHeader

private data class InlineSearchFilters(
    val term: String,
    val translationCode: String?,
    val bookNumber: Int?,
    val startChapter: Int?,
    val endChapter: Int?
)

class SearchCli(
    private val bible: Bible,
    private val processRunner: ProcessRunner = PlatformProcessRunner(),
    private val backendProvider: ((Translation) -> SearchBackend)? = null
) : CliktCommand(name = "search") {

    private val termParts by argument(help = "search term").multiple()
    private val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    private val book by option("-b", "--book", help = "book name or number")
    private val chapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    private val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
        val inlineFilters = parseInlineFilters(termParts)
        val term = inlineFilters.term
        if (term.isBlank()) {
            throw UsageError("Missing search term")
        }

        val translation = resolveTranslation(inlineFilters.translationCode)
        val bookNumber = resolveBookNumber(inlineFilters.bookNumber)
        val (startChapter, endChapterValue) = resolveChapterRange(
            bookNumber = bookNumber,
            inlineStartChapter = inlineFilters.startChapter,
            inlineEndChapter = inlineFilters.endChapter
        )

        val request = SearchRequest(
            term = term,
            translation = translation,
            bookNumber = bookNumber,
            startChapter = startChapter,
            endChapter = endChapterValue,
            verses = verses
        )

        val backend = backendProvider ?: { requestedTranslation ->
            SearchBackendSelector(
                bible = bible,
                processRunner = processRunner
            ).backendFor(requestedTranslation.language)
        }

        val output = try {
            backend(translation).search(request)
        } catch (e: SearchBackendException) {
            throw UsageError(e.message ?: "Search failed")
        }

        val hits = try {
            VersePointerJson.decodeList(output.text)
        } catch (e: Exception) {
            throw UsageError("Search backend returned invalid VersePointer JSON: ${e.message ?: "unknown error"}")
        }

        if (hits.isNotEmpty()) {
            echo(renderHits(hits))
        }
    }

    private fun parseInlineFilters(tokens: List<String>): InlineSearchFilters {
        if (tokens.isEmpty()) {
            return InlineSearchFilters("", null, null, null, null)
        }

        var remaining = tokens
        var inlineTranslationCode: String? = null
        var inlineBookNumber: Int? = null
        var inlineStartChapter: Int? = null
        var inlineEndChapter: Int? = null

        val translationSuffixIndex = findTrailingInIndex(remaining)
        if (translationSuffixIndex != null) {
            val candidate = remaining.drop(translationSuffixIndex + 1)
            if (candidate.size == 1) {
                val code = candidate.single().lowercase()
                if (bible.findTranslationByCode(code)) {
                    inlineTranslationCode = code
                    remaining = remaining.take(translationSuffixIndex)
                }
            }
        }

        val locationSuffixIndex = findTrailingInIndex(remaining)
        if (locationSuffixIndex != null) {
            val locationTokens = remaining.drop(locationSuffixIndex + 1)
            val parsedLocation = parseLocationTokens(locationTokens)
            if (parsedLocation != null) {
                inlineBookNumber = parsedLocation.bookNumber
                inlineStartChapter = parsedLocation.startChapter
                inlineEndChapter = parsedLocation.endChapter
                remaining = remaining.take(locationSuffixIndex)
            }
        }

        return InlineSearchFilters(
            term = remaining.joinToString(separator = " ").trim(),
            translationCode = inlineTranslationCode,
            bookNumber = inlineBookNumber,
            startChapter = inlineStartChapter,
            endChapter = inlineEndChapter
        )
    }

    private fun findTrailingInIndex(tokens: List<String>): Int? {
        for (index in tokens.lastIndex downTo 0) {
            if (tokens[index].equals("in", ignoreCase = true)) {
                return index
            }
        }
        return null
    }

    private data class ParsedLocation(
        val bookNumber: Int,
        val startChapter: Int?,
        val endChapter: Int?
    )

    private fun parseLocationTokens(tokens: List<String>): ParsedLocation? {
        if (tokens.isEmpty()) return null

        for (bookTokenCount in tokens.size downTo 1) {
            val bookTokens = tokens.take(bookTokenCount)
            val bookNumber = resolveInlineBookNumber(bookTokens) ?: continue
            val chapterTokens = tokens.drop(bookTokenCount)
            if (chapterTokens.isEmpty()) {
                return ParsedLocation(bookNumber = bookNumber, startChapter = null, endChapter = null)
            }
            if (chapterTokens.size != 1) continue

            val chapterRange = chapterTokens.single().split("-")
            if (chapterRange.isEmpty() || chapterRange.size > 2) continue
            val startChapter = chapterRange[0].toIntOrNull() ?: continue
            val endChapter = if (chapterRange.size == 2) {
                chapterRange[1].toIntOrNull() ?: continue
            } else {
                null
            }

            return ParsedLocation(
                bookNumber = bookNumber,
                startChapter = startChapter,
                endChapter = endChapter
            )
        }

        return null
    }

    private fun resolveInlineBookNumber(tokens: List<String>): Int? {
        val joined = tokens.joinToString(separator = " ").lowercase()
        return joined.toIntOrNull() ?: runCatching { bookNumber(joined) }.getOrNull()
    }

    private fun renderHits(hits: List<VersePointer>): String {
        return hits.joinToString(separator = "\n\n") { pointer ->
            val chapterText = bible.verses(pointer.translation.code, pointer.book, pointer.chapter)
            val verseText = formatSelectedVersesFromChapterText(
                chapterText = chapterText,
                startVerse = pointer.startVerse,
                endVerse = pointer.endVerse
            ).trimEnd()

            renderHit(pointer, verseText)
        }
    }

    private fun renderHit(pointer: VersePointer, verseText: String): String {
        val header = formatHeader(pointer)
        val startVerse = pointer.startVerse ?: return "$header\n$verseText"
        return verseText.replaceFirst(Regex("^$startVerse\\s+"), "$header ")
    }

    private fun resolveTranslation(inlineTranslationCode: String?): Translation {
        val code = translationCode?.lowercase()
            ?: inlineTranslationCode
            ?: bible.defaultTranslationFromSettings().code
        if (!bible.findTranslationByCode(code)) {
            throw UsageError("Translation code '$code' not found")
        }
        return bible.availableTranslations().first { it.code == code }
    }

    private fun resolveBookNumber(inlineBookNumber: Int?): Int? {
        val raw = book ?: return inlineBookNumber
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

    private fun resolveChapterRange(
        bookNumber: Int?,
        inlineStartChapter: Int?,
        inlineEndChapter: Int?
    ): Pair<Int?, Int?> {
        val start = chapter ?: inlineStartChapter
        val end = endChapter ?: inlineEndChapter
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
