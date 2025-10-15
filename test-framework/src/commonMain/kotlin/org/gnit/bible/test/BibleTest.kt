package org.gnit.bible.test

import org.gnit.bible.Bible
import kotlin.test.assertContains
import kotlin.test.assertTrue

interface BibleTest {
    abstract val bible: Bible

    fun testVerses(){
        val verses = bible.verses(translation = "webus", book = 1, chapter = 1)
        assertTrue(verses.startsWith("1 In the beginning, God created the heavens and the earth."))
    }

    fun testDownloadedVerses(){
        bible.assetManager.download("https://gnit.org/bblpacks/kttv.zip", "kttv.zip")
        assertContains(bible.availableTranslations(), "kttv")
        val verses = bible.verses(translation = "kttv", book = 1, chapter = 1)
        assertTrue(verses.startsWith("1 Ban đầu Đức Chúa Trời dựng nên trời đất."))
    }
}
