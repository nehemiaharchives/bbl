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

    }
}