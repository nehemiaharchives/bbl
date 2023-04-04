package org.gnit.bible.test

import org.gnit.bible.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class CountTest {

    @Test
    fun countGenesis1() {
        val gen1 = countChapterLength(1, 1)
        println(gen1)
        assertEquals(1412, gen1)
    }
}

fun main() {

    var totalBibleLength = 0

    (1..66).forEach { book ->
        val maxChapter = Chapters.maxChapter(book)
        (1..maxChapter).forEach { chapter ->
            totalBibleLength += countChapterLength(book, chapter)
        }
    }

    println("total char length of entire bible is: $totalBibleLength")
}

fun countChapterLength(book: Int, chapter: Int): Int {
    val translation = Translation.jc
    val versePointer = VersePointer(translation = translation, book = book, chapter = chapter)
    val path = chapterTextPath(versePointer)
    val aChapter = File("src/main/resources/$path").readText()
    val split = splitChapterToVerses(aChapter)
    var count = 0

    split.forEach { verse ->
        count += verse.length
    }

    return count
}