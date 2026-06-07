package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class BibleTest {

    @Test
    fun testSplitChapterToVerses() {
        val versesWebus = Bible.splitChapterToVerses(webusGenesisChapterOne)
        assertEquals("In the beginning, God created the heavens and the earth.", versesWebus.first())
        assertEquals(31, versesWebus.size)

        val versesJc = Bible.splitChapterToVerses(jcGenesisChapterOne)
        assertEquals(31, versesJc.size)
    }

    val versePointerGen1 = VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 1, chapter = 1)
    val versePointerJohn3v16 = VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 43, chapter = 3, startVerse = 16)
    val versePointerMatt28v18to20 = VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 28, startVerse = 18, endVerse = 20)

    @Test
    fun parseTest() {
        val actualGen1 = Bible.parse(translation = SupportedTranslation.WEBUS.translation, book = listOf("gen"), chapterVerse = "1")
        assertEquals(versePointerGen1, actualGen1)

        val versePointerGen2 = VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 1, chapter = 2)
        val actualGen2 = Bible.parse(translation = SupportedTranslation.WEBUS.translation, book = listOf("gen"), chapterVerse = "2")
        assertEquals(versePointerGen2, actualGen2)

        val actualJohn3v16 = Bible.parse(translation = SupportedTranslation.WEBUS.translation, book = listOf("john"), chapterVerse = "3:16")
        assertEquals(versePointerJohn3v16, actualJohn3v16)

        val actualMatt28v18to20 = Bible.parse(translation = SupportedTranslation.WEBUS.translation, book = listOf("matt"), chapterVerse = "28:18-20")
        assertEquals(versePointerMatt28v18to20, actualMatt28v18to20)
    }

    @Test
    fun selectVersesTest() {
        val selectedVersesGen1 = Bible.selectVerses(versePointer = versePointerGen1, aChapter = webusGenesisChapterOne)
        assertEquals(webusGenesisChapterOne, selectedVersesGen1)

        val selectedVersesJohn3v16 = Bible.selectVerses(versePointer = versePointerJohn3v16, aChapter = webusJohnChapter3)
        val webusJohn3v16 = "For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life."
        assertEquals(webusJohn3v16, selectedVersesJohn3v16)

        val selectedVersesMatt28v18to20 = Bible.selectVerses(versePointer = versePointerMatt28v18to20, aChapter = webusMatthewChapter28)
        val webusMatt28v18to20 = """
            Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
            Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
            teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
        """.trimIndent()
        assertEquals(webusMatt28v18to20, selectedVersesMatt28v18to20)
    }
}
