package org.gnit.bible.cli

import com.github.ajalt.clikt.core.UsageError
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.CompareBy
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.VersePointerJson
import org.gnit.bible.SearchQueryText

object SearchCliSupport {
    data class InlineSearchFilters(
        val termParts: List<String>,
        val translationCodes: List<String>,
        val bookNumber: Int?,
        val startChapter: Int?,
        val endChapter: Int?,
        val categoryKeys: List<String>,
        val filters: List<BibleFilter>
    )

    private data class ParsedScopeTokens(
        val translationCodes: List<String> = emptyList(),
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
            return InlineSearchFilters(emptyList(), emptyList(), null, null, null, emptyList(), emptyList())
        }

        var remaining = tokens
        val inlineTranslationCodes = mutableListOf<String>()
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

            if (parsedSuffix.translationCodes.isNotEmpty()) {
                if (inlineTranslationCodes.isNotEmpty() && inlineTranslationCodes != parsedSuffix.translationCodes) {
                    throw UsageError(
                        "Conflicting translation scopes: '${inlineTranslationCodes.joinToString(" ")}' and '${parsedSuffix.translationCodes.joinToString(" ")}'"
                    )
                }
                inlineTranslationCodes.clear()
                inlineTranslationCodes.addAll(parsedSuffix.translationCodes)
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
            translationCodes = inlineTranslationCodes.distinct(),
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

    fun resolveTranslations(
        bible: Bible,
        translationCode: String?,
        inlineTranslationCodes: List<String>
    ): List<Translation> {
        val codes = translationCode?.let { listOf(it.lowercase()) }
            ?: inlineTranslationCodes.ifEmpty { listOf(bible.defaultTranslationFromSettings().code) }

        return codes.distinct().map { code ->
            bible.availableTranslations().firstOrNull { it.code == code }
                ?: throw UsageError("Translation '$code' not found. Run 'bbl list translations' to see installed translations.")
        }
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

    fun parseLimitOverride(termParts: List<String>): Pair<List<String>, Int?> {
        if (termParts.size >= 2) {
            val last = termParts.last()
            val secondLast = termParts[termParts.lastIndex - 1]
            if (secondLast.equals("limit", ignoreCase = true)) {
                val limitValue = last.toIntOrNull()
                if (limitValue != null && limitValue > 0) {
                    return termParts.dropLast(2) to limitValue
                }
                throw UsageError("Invalid limit value '$last'. Limit must be a positive integer")
            }
        }
        return termParts to null
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

    fun renderComparisonHits(
        bible: Bible,
        hits: List<VersePointer>,
        translations: List<Translation>
    ): String {
        if (translations.size <= 1) {
            return renderHits(bible, hits)
        }

        val comparedHits = when (bible.compareByFromSettings()) {
            CompareBy.block -> hits.flatMap { hit ->
                translations.map { translation -> hit.copy(translation = translation) }
            }
            CompareBy.verse -> hits.flatMap { hit ->
                val startVerse = hit.startVerse ?: 1
                val endVerse = hit.endVerse ?: hit.startVerse ?: startVerse
                (startVerse..endVerse).flatMap { verseNumber ->
                    translations.map { translation ->
                        hit.copy(translation = translation, startVerse = verseNumber, endVerse = null)
                    }
                }
            }
        }

        return renderHits(bible, comparedHits.filter { bible.hasVerse(it) })
    }

    private fun parseScopeTokens(tokens: List<String>, bible: Bible): ParsedScopeTokens? {
        if (tokens.isEmpty()) return null

        val singleToken = tokens.singleOrNull()?.lowercase()
        if (singleToken != null) {
            if (bible.findTranslationByCode(singleToken)) {
                return ParsedScopeTokens(translationCodes = listOf(singleToken))
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

        val translationCodes = tokens.map { it.lowercase() }
        if (translationCodes.all { bible.findTranslationByCode(it) }) {
            return ParsedScopeTokens(translationCodes = translationCodes.distinct())
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

    private fun Bible.hasVerse(pointer: VersePointer): Boolean {
        val start = pointer.startVerse ?: return true
        val chapterText = runCatching {
            verses(pointer.translation.code, pointer.book, pointer.chapter)
        }.getOrNull() ?: return false
        val verseCount = Bible.splitChapterToVerses(chapterText).size
        val end = pointer.endVerse ?: start
        return start in 1..verseCount && end in start..verseCount
    }
}
