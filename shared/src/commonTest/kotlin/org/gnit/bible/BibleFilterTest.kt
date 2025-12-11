package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleFilterTest {

    private val g1127 = BookChapterVerse(1, 11, 27)
    private val g2511 = BookChapterVerse(1, 25, 11)

    @Test
    fun bookRange() {
        val f = books(1..39)
        assertTrue(f.contains(BookChapterVerse(1, 1, 1)))
        assertFalse(f.contains(BookChapterVerse(40, 1, 1)))
    }

    @Test
    fun bookSet() {
        val f = books(43, 62, 63)
        assertTrue(f.contains(BookChapterVerse(43, 1, 1)))
        assertFalse(f.contains(BookChapterVerse(44, 1, 1)))
    }

    @Test
    fun passage() {
        val f = passage(g1127, g2511)
        assertTrue(f.contains(BookChapterVerse(1, 12, 1)))
        assertTrue(f.contains(BookChapterVerse(1, 22, 1)))
        assertFalse(f.contains(BookChapterVerse(1, 11, 26)))
        assertFalse(f.contains(BookChapterVerse(1, 25, 12)))
    }

    @Test
    fun union() {
        val f = union(books(1..5), books(40..43))
        assertTrue(f.contains(BookChapterVerse(1, 1, 1)))
        assertTrue(f.contains(BookChapterVerse(40, 1, 1)))
        assertFalse(f.contains(BookChapterVerse(10, 1, 1)))
    }
}
