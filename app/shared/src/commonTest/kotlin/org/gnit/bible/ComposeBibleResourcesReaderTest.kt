package org.gnit.bible

import org.gnit.bible.app.ComposeBibleResourcesReader
import org.gnit.bible.app.EmbeddedPackRegistry
import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeBibleResourcesReaderTest : ResourcesTestBase() {

    val bibleResourcesReader = ComposeBibleResourcesReader()

    @Test
    fun testReadByPath() {
        val code = EmbeddedPackRegistry.embeddedCodes.first()
        val actual = bibleResourcesReader.readByPath("files/bblpacks/$code/$code.1.1.txt")
        assertTrue(actual.startsWith("1 "), "expected $code Genesis 1 to start with verse 1 but was '$actual'")
    }

    @Test
    fun testGetChapterText() {
        val code = EmbeddedPackRegistry.embeddedCodes.first()
        val actual = bibleResourcesReader.getChapterText(code, 1, 1)
        assertTrue(actual.startsWith("1 "), "expected $code Genesis 1 to start with verse 1 but was '$actual'")
    }

    @Test
    fun testReading5Chapters(){
        val translation = EmbeddedPackRegistry.embeddedCodes.first()
        for (chapter in 1..5) {
            val actual = bibleResourcesReader.getChapterText(translation, 1, chapter)
            assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
        }
    }

    @Test
    fun testEntireFirstEmbeddedTranslation(){
        val translation = EmbeddedPackRegistry.embeddedCodes.first()
        (1..66).forEach { book ->
            val maxChapter = Books.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleResourcesReader.getChapterText(translation, book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testWEBUS(){
        assertTrue("webus" in EmbeddedPackRegistry.embeddedCodes)
        (1..66).forEach { book ->
            val maxChapter = Books.maxChapter(book)
            (1..maxChapter).forEach { chapter ->
                val actual = bibleResourcesReader.getChapterText("webus", book, chapter)
                assertTrue(actual.startsWith("1 "), "expected to start with 1 but was '$actual'")
            }
        }
    }

    @Test
    fun testOtherEmbeddedTranslations() {
        EmbeddedPackRegistry.embeddedCodes.filterNot { it == "webus" }.forEach { translation ->
            (1..66).forEach { book ->
                val maxChapter = Books.maxChapter(book)
                (1..maxChapter).forEach { chapter ->
                    val actual = bibleResourcesReader.getChapterText(translation, book, chapter)
                    assertTrue((actual.startsWith("1 ") || actual.startsWith("1-2 ")), "expected to start with 1 but was '$actual'")
                }
            }
        }
    }

    @Test
    fun testNonEmbeddedTranslationsAreNotBuiltIn() {
        val nonEmbeddedExamples = SupportedTranslation.all.map { it.code } - EmbeddedPackRegistry.embeddedCodes
        assertTrue(nonEmbeddedExamples.isNotEmpty())
        assertFalse("krv" in EmbeddedPackRegistry.embeddedCodes)
    }
}
