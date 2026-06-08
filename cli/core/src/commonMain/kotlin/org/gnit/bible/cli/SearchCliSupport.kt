package org.gnit.bible.cli

import com.github.ajalt.clikt.core.UsageError
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.VersePointerJson
import org.gnit.bible.SearchQueryText

object SearchCliSupport {
    data class InlineSearchFilters(
        val termParts: List<String>,
        val translationCode: String?,
        val bookNumber: Int?,
        val startChapter: Int?,
        val endChapter: Int?,
        val categoryKeys: List<String>,
        val filters: List<BibleFilter>
    )

    private data class ParsedScopeTokens(
        val translationCode: String? = null,
        val bookNumber: Int? = null,
        val startChapter: Int? = null,
        val endChapter: Int? = null,
        val categoryKeys: List<String> = emptyList(),
        val filters: List<BibleFilter> = emptyList()
    )

    private data class ParsedLocation(
        val bookNumber: Int,
        val startChapter: Int?,
        val endChapter: Int?
    )

    fun parseInlineFilters(tokens: List<String>, bible: Bible): InlineSearchFilters {
        if (tokens.isEmpty()) {
            return InlineSearchFilters(emptyList(), null, null, null, null, emptyList(), emptyList())
        }

        var remaining = tokens
        var inlineTranslationCode: String? = null
        var inlineBookNumber: Int? = null
        var inlineStartChapter: Int? = null
        var inlineEndChapter: Int? = null
        val inlineCategoryKeys = mutableListOf<String>()
        val inlineFilters = mutableListOf<BibleFilter>()

        while (true) {
            val suffixIndex = findTrailingInIndex(remaining) ?: break
            if (suffixIndex == remaining.lastIndex) break

            val suffixTokens = remaining.drop(suffixIndex + 1)
            val parsedSuffix = parseScopeTokens(suffixTokens, bible) ?: break

            parsedSuffix.translationCode?.let { code ->
                if (inlineTranslationCode != null && inlineTranslationCode != code) {
                    throw UsageError("Conflicting translation scopes: '$inlineTranslationCode' and '$code'")
                }
                inlineTranslationCode = code
            }
            parsedSuffix.bookNumber?.let { book ->
                if (inlineBookNumber != null && inlineBookNumber != book) {
                    throw UsageError("Conflicting book scopes")
                }
                inlineBookNumber = book
            }
            parsedSuffix.startChapter?.let { chapter ->
                if (inlineStartChapter != null && inlineStartChapter != chapter) {
                    throw UsageError("Conflicting chapter scopes")
                }
                inlineStartChapter = chapter
            }
            parsedSuffix.endChapter?.let { end ->
                if (inlineEndChapter != null && inlineEndChapter != end) {
                    throw UsageError("Conflicting chapter ranges")
                }
                inlineEndChapter = end
            }
            inlineCategoryKeys.addAll(parsedSuffix.categoryKeys)
            inlineFilters.addAll(parsedSuffix.filters)
            remaining = remaining.take(suffixIndex)
        }

        return InlineSearchFilters(
            termParts = remaining,
            translationCode = inlineTranslationCode,
            bookNumber = inlineBookNumber,
            startChapter = inlineStartChapter,
            endChapter = inlineEndChapter,
            categoryKeys = inlineCategoryKeys.distinct(),
            filters = inlineFilters.distinct()
        )
    }

    fun resolveTranslation(
        bible: Bible,
        translationCode: String?,
        inlineTranslationCode: String?
    ): Translation {
        val code = translationCode?.lowercase()
            ?: inlineTranslationCode
            ?: bible.defaultTranslationFromSettings().code

        return bible.availableTranslations().firstOrNull { it.code == code }
            ?: throw UsageError("Translation '$code' not found. Run 'bbl list translations' to see installed translations.")
    }

    fun renderResults(pointers: List<VersePointer>, bible: Bible, jsonOutput: Boolean): String {
        if (pointers.isEmpty()) {
            return if (jsonOutput) "[]" else "No results found."
        }

        if (jsonOutput) {
            return VersePointerJson.encodeList(pointers)
        }

        return pointers.joinToString(separator = "\n") { pointer ->
            val chapterText = bible.verses(pointer.translation.code, pointer.book, pointer.chapter)
            val verseText = Bible.selectVerses(pointer, chapterText)
            renderHit(pointer, verseText)
        }
    }

    fun parseSearchTerm(termParts: List<String>): String = SearchQueryText.searchTermFromArgs(termParts)

    fun resolveBookNumber(book: String?, inlineBookNumber: Int?): Int? {
        return when {
            book == null -> inlineBookNumber
            book.toIntOrNull() != null -> book.toInt()
            else -> Books.bookNumber(book.lowercase())
        }
    }

    fun resolveChapterRange(
        bookNumber: Int?,
        explicitStartChapter: Int?,
        explicitEndChapter: Int?,
        inlineStartChapter: Int?,
        inlineEndChapter: Int?
    ): Pair<Int?, Int?> {
        val startChapter = explicitStartChapter ?: inlineStartChapter
        val endChapter = explicitEndChapter ?: inlineEndChapter

        if (startChapter != null && startChapter < 1) {
            throw UsageError("Chapter numbers must be >= 1.")
        }
        if (endChapter != null && endChapter < 1) {
            throw UsageError("Chapter numbers must be >= 1.")
        }
        if (startChapter != null && endChapter != null && endChapter < startChapter) {
            throw UsageError("End chapter must be >= start chapter.")
        }
        if (bookNumber != null) {
            val maxChapter = Books.maxChapter(bookNumber)
            if (startChapter != null && startChapter > maxChapter) {
                throw UsageError("Chapter $startChapter is out of range for ${Books.bookNameEnglishCapital(bookNumber)}. Valid range: 1..$maxChapter.")
            }
            if (endChapter != null && endChapter > maxChapter) {
                throw UsageError("Chapter $endChapter is out of range for ${Books.bookNameEnglishCapital(bookNumber)}. Valid range: 1..$maxChapter.")
            }
        }

        return startChapter to endChapter
    }

    fun resolveCategoryFilters(categoryKeys: List<String>): Pair<List<String>, List<BibleFilter>> {
        val normalizedKeys = categoryKeys.map { it.lowercase() }
        val filters = normalizedKeys.map { key ->
            Books.Category.filterOrNull(key)
                ?: throw UsageError("Unknown category '$key'. Run 'bbl list categories' to see supported values.")
        }
        return normalizedKeys.distinct() to filters.distinct()
    }

    fun renderHits(bible: Bible, hits: List<VersePointer>): String {
        return renderResults(hits, bible, jsonOutput = false)
    }

    private fun parseScopeTokens(tokens: List<String>, bible: Bible): ParsedScopeTokens? {
        if (tokens.isEmpty()) return null

        val singleToken = tokens.singleOrNull()?.lowercase()
        if (singleToken != null) {
            if (bible.findTranslationByCode(singleToken)) {
                return ParsedScopeTokens(translationCode = singleToken)
            }

            val category = Books.Category.fromKey(singleToken)
            if (category != null) {
                return ParsedScopeTokens(
                    categoryKeys = listOf(singleToken),
                    filters = listOf(category.filter)
                )
            }
        }

        val joinedTokens = tokens.joinToString(" ").lowercase()
        Books.Category.fromKey(joinedTokens)?.let { category ->
            return ParsedScopeTokens(
                categoryKeys = listOf(joinedTokens),
                filters = listOf(category.filter)
            )
        }

        if (tokens.size > 1 && bible.findTranslationByCode(tokens.first().lowercase())) {
            throw UsageError("Translation scope must be separate from book or category scope. Use repeated 'in' scopes, for example: search Jesus in john 3 in kjv")
        }

        val location = parseLocationTokens(tokens)
        if (location != null) {
            return ParsedScopeTokens(
                bookNumber = location.bookNumber,
                startChapter = location.startChapter,
                endChapter = location.endChapter
            )
        }

        return null
    }

    private fun findTrailingInIndex(tokens: List<String>): Int? {
        for (index in tokens.lastIndex downTo 0) {
            if (tokens[index].equals("in", ignoreCase = true)) {
                return index
            }
        }
        return null
    }

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
        return joined.toIntOrNull() ?: runCatching { Books.bookNumber(joined) }.getOrNull()
    }

    private fun renderHit(pointer: VersePointer, verseText: String): String {
        val header = Books.formatHeader(pointer)
        val startVerse = pointer.startVerse ?: return "$header\n$verseText"
        val textWithoutVerseNumber = verseText.replaceFirst(Regex("^$startVerse\\s+"), "")
        return "$header ${textWithoutVerseNumber.trimEnd()}"
    }
}
