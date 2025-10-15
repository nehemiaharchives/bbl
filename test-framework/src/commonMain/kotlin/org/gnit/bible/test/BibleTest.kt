package org.gnit.bible.test

import org.gnit.bible.Bible
import kotlin.test.assertTrue

interface BibleTest {
    abstract val bible: Bible

    fun testVerses(){
        val verses = bible.bibleTextReader.getChapterText(translation = "webus", book = 1, chapter = 1)
        assertTrue(verses.startsWith("1 In the beginning, God created the heavens and the earth."))
    }
}
