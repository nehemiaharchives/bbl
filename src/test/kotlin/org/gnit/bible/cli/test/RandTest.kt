package org.gnit.bible.cli.test

import org.gnit.bible.*
import org.gnit.bible.cli.*
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RandChapterTest {

    private val randCli = RandCli(Config(randomlyShow = RandomlyShow.chapter))

    @Test
    fun `config randomlyShow=chapter narrowDown=null`() {
        randCli.parse(emptyArray())
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..66)
        assertTrue(vp.chapter in 1..Chapters.maxChapter(vp.book))

        //should have more verses than the smallest chapter in the bible (Psalm 117 with 2 verses)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=ot`() {
        randCli.parse(arrayOf("ot"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..39)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=nt`() {
        randCli.parse(arrayOf("nt"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=g`() {
        randCli.parse(arrayOf("g"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..43)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=ot verse`() {
        randCli.parse(arrayOf("ot verse"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..39)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=nt verse`() {
        randCli.parse(arrayOf("nt verse"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=g verse`() {
        randCli.parse(arrayOf("g verse"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..43)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=chapter narrowDown=verse`() {
        randCli.parse(arrayOf("nt verse"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }
}

class RandVerseTest {

    private val randCli = RandCli(Config(randomlyShow = RandomlyShow.verse))

    @Test
    fun `config randomlyShow=verse narrowDown=null`() {
        randCli.parse(emptyArray())
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..66)
        assertTrue(vp.chapter in 1..Chapters.maxChapter(vp.book))
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=ot`() {
        randCli.parse(arrayOf("ot"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..39)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=nt`() {
        randCli.parse(arrayOf("nt"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=g`() {
        randCli.parse(arrayOf("g"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..43)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size == 1)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=ot chapter`() {
        randCli.parse(arrayOf("ot chapter"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..39)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=nt chapter`() {
        randCli.parse(arrayOf("nt chapter"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=g chapter`() {
        randCli.parse(arrayOf("g chapter"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 40..43)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }

    @Test
    fun `config randomlyShow=verse narrowDown=chapter`() {
        randCli.parse(arrayOf("chapter"))
        val vp = randCli.versePointer
        assertTrue(vp.book in 1..66)
        assertTrue(splitChapterToVerses(randCli.selectedVerses).size >= 2)
    }
}