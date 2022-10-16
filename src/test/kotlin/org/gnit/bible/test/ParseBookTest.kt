package org.gnit.bible.test

import org.gnit.bible.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseBookTest {

    @Test
    fun bookNameCapitalTest(){
        assertEquals("Genesis", bookNameCapital(1))
        assertEquals("1 John", bookNameCapital(62))
        assertEquals("Song of Solomon", bookNameCapital(22))
    }

    @Test
    fun filterByBookTest() {
        assertEquals(BookChapterFilter(book = 1, term = "God"), filterByBookChapter("God in gen"))
        (1..66).forEach { bookNumber ->
            assertEquals(
                BookChapterFilter(book = bookNumber, term = "God"),
                filterByBookChapter("God in ${bookName(bookNumber)}")
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
}