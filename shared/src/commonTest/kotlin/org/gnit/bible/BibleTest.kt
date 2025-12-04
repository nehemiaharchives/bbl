package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class BibleTest {

    @Test
    fun testSplitChapterToVerses() {
        val versesWebus = Bible.splitChapterToVerses(webusGenesisChapterOne)
        assertEquals(31, versesWebus.size)

        val versesJc = Bible.splitChapterToVerses(jcGenesisChapterOne)
        assertEquals(31, versesJc.size)
    }
}
