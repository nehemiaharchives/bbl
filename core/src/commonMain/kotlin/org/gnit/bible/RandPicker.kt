package org.gnit.bible

import kotlin.math.min
import kotlin.random.Random

data class RandResult(
    val pointer: VersePointer,
    val selection: String,
    val selectionType: RandomlyShow
)

/**
 * Platform-agnostic random picker for verses/chapters.
 * @param choose deterministic selector mainly for tests; defaults to Random.
 */
class RandPicker(
    private val readChapter: (translation: String, book: Int, chapter: Int) -> String,
    private val selector: (IntRange) -> Int = { range -> Random.nextInt(range.first, range.last + 1) },
) {

    fun random(
        translation: Translation = SupportedTranslation.WEBUS.translation,
        filter: BibleFilter = BibleFilter.All,
        randomlyShow: RandomlyShow = RandomlyShow.verse
    ): RandResult {

        val book = pickBook(filter)
        val chapter = pickChapter(filter, book)
        val chapterText = readChapter(translation.code, book, chapter)

        val selection: String
        val selectionType: RandomlyShow
        val verseNumber: Int?

        if (randomlyShow == RandomlyShow.chapter) {
            selectionType = RandomlyShow.chapter
            selection = chapterText
            verseNumber = null
        } else {
            val verses = Bible.splitChapterToVerses(chapterText)
            val verse = pick(verseRange(filter, book, chapter, verses.size))
            selectionType = RandomlyShow.verse
            selection = verses[verse - 1]
            verseNumber = verse
        }

        val pointer = VersePointer(
            translation = translation,
            book = book,
            chapter = chapter,
            startVerse = verseNumber,
            endVerse = verseNumber
        )

        return RandResult(pointer, selection, selectionType)
    }

    private fun pickBook(filter: BibleFilter): Int = when (filter) {
        is BibleFilter.BookRange -> pick(filter.range)
        is BibleFilter.BookSet -> pick(fromSet(filter.books))
        is BibleFilter.Passage -> pick(filter.start.book..filter.endInclusive.book)
        is BibleFilter.Union -> pickBook(filter.filters[pick(0 until filter.filters.size)])
        BibleFilter.All -> pick(1..66)
    }

    private fun pickChapter(filter: BibleFilter, book: Int): Int = when (filter) {
        is BibleFilter.Passage -> {
            if (filter.start.book == book && filter.endInclusive.book == book) {
                pick(filter.start.chapter..filter.endInclusive.chapter)
            } else {
                pick(1..Books.maxChapter(book))
            }
        }
        else -> pick(1..Books.maxChapter(book))
    }

    private fun verseRange(filter: BibleFilter, book: Int, chapter: Int, maxVerse: Int): IntRange {
        if (filter !is BibleFilter.Passage || filter.start.book != book || filter.endInclusive.book != book) {
            return 1..maxVerse
        }

        val startVerse = if (chapter == filter.start.chapter) filter.start.verse else 1
        val endVerse = if (chapter == filter.endInclusive.chapter) min(filter.endInclusive.verse, maxVerse) else maxVerse
        return startVerse..endVerse
    }

    private fun pick(range: IntRange): Int = selector.invoke(range)

    private fun fromSet(books: Set<Int>): IntRange {
        val sorted = books.sorted()
        // choose by index then map
        val idx = pick(0 until sorted.size)
        return sorted[idx]..sorted[idx]
    }
}
