package org.gnit.bible.test

import com.github.ajalt.clikt.core.subcommands
import org.gnit.bible.*
import org.junit.jupiter.api.Test
import java.lang.Exception
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MainTest {

    val defaultConfig = Config()
    val bbl = Bbl(defaultConfig)

    @Test
    fun default() {
        bbl.parse(emptyArray())
        assertEquals(VersePointer(book = 1, chapter = 1), bbl.versePointer)
    }

    @Test
    fun `genesis 1`() {
        bbl.parse(arrayOf("genesis", "1"))
        assertEquals(listOf("genesis"), bbl.book)
        assertEquals("1", bbl.chapterVerse)
    }

    @Test
    fun `genesis 3`() {
        bbl.parse(arrayOf("genesis", "3"))
        assertEquals(listOf("genesis"), bbl.book)
        assertEquals("3", bbl.chapterVerse)
        assertEquals(webusGen3, bbl.selectedVerses)
    }

    @Test
    fun `1 corinthians 2`() {
        bbl.parse(arrayOf("1", "corinthians", "2"))
        assertEquals(listOf("1", "corinthians"), bbl.book)
        assertEquals("2", bbl.chapterVerse)
        assertEquals(VersePointer(book = 46, chapter = 2), bbl.versePointer)
    }

    @Test
    fun `1 corinthians 2,2`() {
        bbl.parse(arrayOf("1", "co", "2:2"))
        assertEquals(listOf("1", "co"), bbl.book)
        assertEquals("2:2", bbl.chapterVerse)
        assertEquals(VersePointer(book = 46, chapter = 2, startVerse = 2), bbl.versePointer)
        assertEquals(
            "2 For I determined not to know anything among you except Jesus Christ and him crucified.",
            bbl.selectedVerses
        )
    }

    @Test
    fun `1 corinthians 2,3-5`() {
        bbl.parse(arrayOf("1", "co", "2:3-5"))
        assertEquals(listOf("1", "co"), bbl.book)
        assertEquals("2:3-5", bbl.chapterVerse)
        assertEquals(VersePointer(book = 46, chapter = 2, startVerse = 3, endVerse = 5), bbl.versePointer)
        assertEquals(
            """
                3 I was with you in weakness, in fear, and in much trembling.
                4 My speech and my preaching were not in persuasive words of human wisdom, but in demonstration of the Spirit and of power,
                5 that your faith wouldn’t stand in the wisdom of men, but in the power of God.
               """.trimIndent(),
            bbl.selectedVerses
        )
    }

    @Test
    fun readFromResourcesTest() {

        val versePointer = VersePointer(book = 1, chapter = 3)
        val text = readFromResources(versePointer)

        assertEquals(text, webusGen3)
    }

    @Test
    fun splitChapterToVerseseTest() {
        val verses: Array<String> = splitChapterToVerses(webusGen3)
        assertEquals(
            verses[18], """You will eat bread by the sweat of your face until you return to the ground,
                          | for you were taken out of it.
                          | For you are dust,
                          | and you shall return to dust.”""".trimMargin()
        )
    }

    @Test
    fun selectTranslationTest() {
        val subCommand = In()
        bbl.subcommands(subCommand).parse(arrayOf("john", "8:15", "in", "kjv"))

        assertEquals("15 Ye judge after the flesh; I judge no man.", subCommand.selectedVerses)
    }

    @Test
    fun selectKrvTest() {
        val subCommand = In()
        bbl.subcommands(subCommand).parse(arrayOf("john", "8:15", "in", "krv"))

        assertEquals("15 너희는 육체를 따라 판단하나 나는 아무도 판단치 아니하노라", subCommand.selectedVerses)
    }

    @Test
    fun selectCunpTest() {
        val subCommand = In()
        bbl.subcommands(subCommand).parse(arrayOf("john", "8:15", "in", "cunp"))

        assertEquals("15 你们是以外貌 判断人，我却不判断人。", subCommand.selectedVerses)
    }

    @Test
    fun selectJcTest(){
        val subCommand = In()
        bbl.subcommands(subCommand).parse(arrayOf("john", "8:15", "in", "jc"))

        assertEquals("15 あなたがたは肉によって人をさばくが、わたしはだれもさばかない。", subCommand.selectedVerses)
    }

    @Test
    fun wrongBookNameError(){
        assertFailsWith<Exception>("book '1col' not found in the list of book names") {
            bbl.parse(arrayOf("1col", "1"))
        }
    }
}