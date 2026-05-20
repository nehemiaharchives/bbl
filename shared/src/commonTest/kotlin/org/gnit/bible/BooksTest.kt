package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun testCategoryKeysDoNotConflictWithBookAliases() {
        val bookAliases = bookNameNumberArray
            .flatMap { it.asList() }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val categoryAliases = Books.Category.entries
            .flatMap { it.key }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val overlap = categoryAliases.intersect(bookAliases)

        assertEquals(emptySet(), overlap, "Category keys must not overlap with canonical book aliases: $overlap")
    }
}
