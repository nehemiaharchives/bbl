package org.gnit.bible.app

import org.gnit.bible.Translation
import org.gnit.bible.app.state.BibleState

data class BibleTextSelection(
    val firstUnit: Int,
    val lastUnit: Int,
    val unitCount: Int,
    val unitsPerVerse: Int,
    val anchorUnit: Int = firstUnit
) {
    val anchorVerse: Int
        get() {
            if (unitCount <= 0) return 1
            return (anchorUnit.coerceIn(0, unitCount - 1) / unitsPerVerse) + 1
        }

    fun containsSingleVerse(verse: Int): Boolean {
        if (unitsPerVerse != SINGLE_UNITS_PER_VERSE) return false
        return (verse - 1) in selectedUnitRange()
    }

    fun containsBilingualVersePart(verse: Int, isSubTranslation: Boolean): Boolean {
        if (unitsPerVerse != BILINGUAL_UNITS_PER_VERSE) return false
        val unit = ((verse - 1) * BILINGUAL_UNITS_PER_VERSE) + if (isSubTranslation) 1 else 0
        return unit in selectedUnitRange()
    }

    fun selectAll(): BibleTextSelection {
        if (unitCount <= 0) return this
        return copy(firstUnit = 0, lastUnit = unitCount - 1)
    }

    fun copySingleText(bibleState: BibleState, verses: Array<String>): String {
        val selectedVerses = selectedUnitRange()
            .map { unit -> unit + 1 }
            .filter { verse -> verse in 1..verses.size }
        if (selectedVerses.isEmpty()) return ""

        return buildString {
            appendLine("${bibleState.describeBookChapter()} ${bibleState.mainTranslation.code.uppercase()}")
            selectedVerses.forEach { verse ->
                appendLine("$verse ${verses[verse - 1]}")
            }
        }.trimEnd()
    }

    fun copyBilingualText(bibleState: BibleState, versePairs: List<Pair<String, String>>): String {
        val subTranslation = bibleState.subTranslation ?: return ""
        val selectedUnits = selectedUnitRange()
            .filter { unit -> unit / BILINGUAL_UNITS_PER_VERSE in versePairs.indices }
        if (selectedUnits.isEmpty()) return ""

        return buildString {
            appendLine(
                "${bibleState.describeBookChapter()} " +
                    "${bibleState.mainTranslation.code.uppercase()} / ${subTranslation.code.uppercase()}"
            )
            selectedUnits.forEach { unit ->
                val verseIndex = unit / BILINGUAL_UNITS_PER_VERSE
                val isSubTranslation = unit % BILINGUAL_UNITS_PER_VERSE == 1
                val verseNumber = verseIndex + 1
                val translation = if (isSubTranslation) subTranslation else bibleState.mainTranslation
                val verseText = if (isSubTranslation) versePairs[verseIndex].second else versePairs[verseIndex].first
                appendLine("$verseNumber ${translation.copyLabel()} $verseText")
            }
        }.trimEnd()
    }

    private fun selectedUnitRange(): IntRange {
        if (unitCount <= 0) return IntRange.EMPTY
        val first = minOf(firstUnit, lastUnit).coerceIn(0, unitCount - 1)
        val last = maxOf(firstUnit, lastUnit).coerceIn(0, unitCount - 1)
        return first..last
    }

    private fun Translation.copyLabel(): String = "[${code.uppercase()}]"

    companion object {
        private const val SINGLE_UNITS_PER_VERSE = 1
        private const val BILINGUAL_UNITS_PER_VERSE = 2

        fun singleVerse(verse: Int, verseCount: Int): BibleTextSelection {
            val unitCount = verseCount.coerceAtLeast(0)
            val unit = (verse - 1).coerceIn(0, (unitCount - 1).coerceAtLeast(0))
            return BibleTextSelection(
                firstUnit = unit,
                lastUnit = unit,
                unitCount = unitCount,
                unitsPerVerse = SINGLE_UNITS_PER_VERSE,
                anchorUnit = unit
            )
        }

        fun bilingualMainVerse(verse: Int, verseCount: Int): BibleTextSelection {
            val unitCount = (verseCount * BILINGUAL_UNITS_PER_VERSE).coerceAtLeast(0)
            val unit = ((verse - 1) * BILINGUAL_UNITS_PER_VERSE).coerceIn(0, (unitCount - 1).coerceAtLeast(0))
            return BibleTextSelection(
                firstUnit = unit,
                lastUnit = unit,
                unitCount = unitCount,
                unitsPerVerse = BILINGUAL_UNITS_PER_VERSE,
                anchorUnit = unit
            )
        }
    }
}
