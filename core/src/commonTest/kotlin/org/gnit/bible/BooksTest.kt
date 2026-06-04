package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BooksTest {

    @Test
    fun testMaxChapter() {
        assertEquals(50, Books.maxChapter(1)) //gen
        assertEquals(12, Books.maxChapter(21)) //ecc
        assertEquals(28, Books.maxChapter(40)) //matt
        assertEquals(16, Books.maxChapter(45)) //rom
        assertEquals(22, Books.maxChapter(66)) //rev
    }

    @Test
    fun testCategory(){
        fun bookContains(cat: Books.Category, book: Int) = cat.filter.contains(BookChapterVerse(book, 1, 1))

        assertEquals(true, bookContains(Books.Category.OLD_TESTAMENT, 1))
        assertEquals(false, bookContains(Books.Category.OLD_TESTAMENT, 40))

        assertEquals(true, bookContains(Books.Category.NEW_TESTAMENT, 66))
        assertEquals(false, bookContains(Books.Category.NEW_TESTAMENT, 39))

        val abraham = Books.Category.ABRAHAM.filter
        assertEquals(false, abraham.contains(BookChapterVerse(1, 11, 20))) // before start
        assertEquals(true, abraham.contains(BookChapterVerse(1, 15, 1)))   // inside
        assertEquals(false, abraham.contains(BookChapterVerse(1, 25, 12))) // after end
    }

    @Test
    fun bookNameCapitalTest(){
        assertEquals("Genesis", Books.bookNameEnglishCapital(1))
        assertEquals("1 John", Books.bookNameEnglishCapital(62))
        assertEquals("Song of Solomon", Books.bookNameEnglishCapital(22))
    }

    @Test
    fun filterByBookTest() {
        assertEquals(BookChapterFilter(book = 1, term = "God"), Books.filterByBookChapter("God in gen"))
        (1..66).forEach { bookNumber ->
            assertEquals(
                BookChapterFilter(book = bookNumber, term = "God"),
                Books.filterByBookChapter("God in ${Books.bookNameEnglish(bookNumber)}")
            )
        }
    }

    @Test
    fun filterByBookAndAChapter() {

        val term = "pray in ps 150"
        assertTrue(term.matches(".+ in ps ([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex()))

        (1..150).forEach { chapter ->
            assertEquals(
                BookChapterFilter(book = 19, startChapter = chapter, term = "pray"),
                Books.filterByBookChapter("pray in ps $chapter")
            )
        }
    }

    @Test
    fun filterByBookAndARangeOfChapters() {
        val term = "pray in ps 99-101"
        assertTrue(term.matches(".+ in ps ([1-9]|[1-9][0-9]|1[0-5][0-9])-([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex()))

        (2..150).forEach { endChapter ->
            assertEquals(
                BookChapterFilter(book = 19, startChapter = 1, endChapter = endChapter, term = "pray"),
                Books.filterByBookChapter("pray in ps 1-$endChapter")
            )
        }
    }

    @Test
    fun formatHeaderTest(){
        assertEquals("Genesis 1", Books.formatHeader(VersePointer(book = 1, chapter = 1)))
        assertEquals("John 3:16", Books.formatHeader(VersePointer(book = 43, chapter = 3, startVerse = 16)))
        assertEquals("Matthew 28:18-20", Books.formatHeader(VersePointer(book = 40, chapter = 28, startVerse = 18, endVerse = 20)))

        assertEquals("創世記 1", Books.formatHeader(VersePointer(translation = Translation.jc, book = 1, chapter = 1)))
        assertEquals("ヨハネによる福音書 3:16", Books.formatHeader(VersePointer(translation = Translation.jc, book = 43, chapter = 3, startVerse = 16)))
        assertEquals("マタイによる福音書 28:18-20", Books.formatHeader(VersePointer(translation = Translation.jc, book = 40, chapter = 28, startVerse = 18, endVerse = 20)))
    }

    @Test
    fun bookNameForTest() {
        val translation = Translation.webus
        val bookNames = translation.language.bookNames()
        (1..66).forEach { bookNumber ->
            assertEquals(bookNames[bookNumber - 1], Books.bookNameFor(bookNumber, translation))
        }
    }

    @Test
    fun bookNameForJapaneseTest() {
        val translation = Translation.jc
        val bookNames = translation.language.bookNames()
        (1..66).forEach { bookNumber ->
            assertEquals(bookNames[bookNumber - 1], Books.bookNameFor(bookNumber, translation))
        }
    }
}
