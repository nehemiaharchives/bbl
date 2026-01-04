package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ParseBookTest {

    @Test
    fun bookNameCapitalTest(){
        assertEquals("Genesis", bookNameEnglishCapital(1))
        assertEquals("1 John", bookNameEnglishCapital(62))
        assertEquals("Song of Solomon", bookNameEnglishCapital(22))
    }

    @Test
    fun filterByBookTest() {
        assertEquals(BookChapterFilter(book = 1, term = "God"), filterByBookChapter("God in gen"))
        (1..66).forEach { bookNumber ->
            assertEquals(
                BookChapterFilter(book = bookNumber, term = "God"),
                filterByBookChapter("God in ${bookNameEnglish(bookNumber)}")
            )
        }
    }

    @Test
    fun `filter by book and a chapter`() {

        val term = "pray in ps 150"
        assertTrue(term.matches(".+ in ps ([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex()))

        (1..150).forEach { chapter ->
            assertEquals(
                BookChapterFilter(book = 19, startChapter = chapter, term = "pray"),
                filterByBookChapter("pray in ps $chapter")
            )
        }
    }

    @Test
    fun `filter by book and a range of chapters`() {
        val term = "pray in ps 99-101"
        assertTrue(term.matches(".+ in ps ([1-9]|[1-9][0-9]|1[0-5][0-9])-([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex()))

        (2..150).forEach { endChapter ->
            assertEquals(
                BookChapterFilter(book = 19, startChapter = 1, endChapter = endChapter, term = "pray"),
                filterByBookChapter("pray in ps 1-$endChapter")
            )
        }
    }

    @Test
    fun formatHeaderTest(){
        assertEquals("Genesis 1", formatHeader(VersePointer(book = 1, chapter = 1)))
        assertEquals("John 3:16", formatHeader(VersePointer(book = 43, chapter = 3, startVerse = 16)))
        assertEquals("Matthew 28:18-20", formatHeader(VersePointer(book = 40, chapter = 28, startVerse = 18, endVerse = 20)))

        assertEquals("創世記 1", formatHeader(VersePointer(translation = Translation.jc, book = 1, chapter = 1)))
        assertEquals("ヨハネによる福音書 3:16", formatHeader(VersePointer(translation = Translation.jc, book = 43, chapter = 3, startVerse = 16)))
        assertEquals("マタイによる福音書 28:18-20", formatHeader(VersePointer(translation = Translation.jc, book = 40, chapter = 28, startVerse = 18, endVerse = 20)))
    }

    @Test
    fun bookNameForTest() {
        val translation = Translation.webus
        val bookNames = translation.language.bookNames()
        (1..66).forEach { bookNumber ->
            assertEquals(bookNames[bookNumber - 1], bookNameFor(bookNumber, translation))
        }
    }

    @Test
    fun bookNameForJapaneseTest() {
        val translation = Translation.jc
        val bookNames = translation.language.bookNames()
        (1..66).forEach { bookNumber ->
            assertEquals(bookNames[bookNumber - 1], bookNameFor(bookNumber, translation))
        }
    }
}
