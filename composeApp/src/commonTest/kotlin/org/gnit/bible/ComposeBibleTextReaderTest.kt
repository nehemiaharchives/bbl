package org.gnit.bible

import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertTrue

class ComposeBibleTextReaderTest : ResourcesTestBase() {

    val bibleTextReader = ComposeBibleTextReader()

    @Test
    fun testReadByPath() {
        val actual = bibleTextReader.readByPath("files/bblpacks/kjv/kjv.1.1.txt")
        assertTrue(actual.startsWith("1 In the beginning God created the heaven and the earth."))
    }

    @Test
    fun testGetChapterText() {
        val actual = bibleTextReader.getChapterText("kjv", 1, 1)
        assertTrue(actual.startsWith("1 In the beginning God created the heaven and the earth."))
    }

    @Test
    fun testReading50Chapters(){
        for (chapter in 1..50) {
            val actual = bibleTextReader.getChapterText("kjv", 1, chapter)
            assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
        }
    }

    @Test
    fun testEntireKJV(){
        (1..66).forEach { book ->
            val maxChapter = Chapters.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleTextReader.getChapterText("kjv", book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testWEBUS(){
        (1..66).forEach { book ->
            val maxChapter = Chapters.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleTextReader.getChapterText("webus", book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testOtherTranslations() {
        val translations = listOf("rvr09", "tb", "delut", "lsg", "sinod", "svrj", "rdv24", "ubg", "ubio", "sven", "cunp", "krv", "jc").forEach { translation ->
            (1..66).forEach { book ->
                val maxChapter = Chapters.maxChapter(book)
                (1..maxChapter).forEach { chapter ->
                    val actual = bibleTextReader.getChapterText(translation, book, chapter)
                    assertTrue((actual.startsWith("1 ") || actual.startsWith("1-2 ")), "expected to start with 1 but was '$actual'")
                }
            }
        }
    }
}
