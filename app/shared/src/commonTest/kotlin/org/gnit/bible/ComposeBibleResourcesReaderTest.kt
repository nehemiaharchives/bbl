package org.gnit.bible

import org.gnit.bible.app.ComposeBibleResourcesReader
import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertTrue

class ComposeBibleResourcesReaderTest : ResourcesTestBase() {

    val bibleResourcesReader = ComposeBibleResourcesReader()

    @Test
    fun testReadByPath() {
        val actual = bibleResourcesReader.readByPath("files/bblpacks/kjv/kjv.1.1.txt")
        assertTrue(actual.startsWith("1 In the beginning God created the heaven and the earth."))
    }

    @Test
    fun testGetChapterText() {
        val actual = bibleResourcesReader.getChapterText("kjv", 1, 1)
        assertTrue(actual.startsWith("1 In the beginning God created the heaven and the earth."))
    }

    @Test
    fun testReading5Chapters(){
        for (chapter in 1..5) {
            val actual = bibleResourcesReader.getChapterText("kjv", 1, chapter)
            assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
        }
    }

    @Test
    fun testEntireKJV(){
        (1..66).forEach { book ->
            val maxChapter = Books.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleResourcesReader.getChapterText("kjv", book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testWEBUS(){
        (1..66).forEach { book ->
            val maxChapter = Books.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleResourcesReader.getChapterText("webus", book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testOtherTranslations() {
        val translations = listOf("rvr09", "tb", "delut", "lsg", "sinod", "svrj", "rdv24", "ubg", "ubio", "sven", "cunp", "krv", "jc").forEach { translation ->
            (1..66).forEach { book ->
                val maxChapter = Books.maxChapter(book)
                (1..maxChapter).forEach { chapter ->
                    val actual = bibleResourcesReader.getChapterText(translation, book, chapter)
                    assertTrue((actual.startsWith("1 ") || actual.startsWith("1-2 ")), "expected to start with 1 but was '$actual'")
                }
            }
        }
    }
}
