package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class ChaptersTest {

    @Test
    fun testMaxChapters() {
        assertEquals(50, Books.maxChapter(1)) // Genesis
        assertEquals(150, Books.maxChapter(19)) // Psalms
    }
}