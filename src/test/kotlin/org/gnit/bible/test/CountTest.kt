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

    @Test
    fun countCharactersInGenesis1(){
        val book = 1
        val chapter = 1
        val distinctCharSet = distinctChars(book, chapter)
        println("total number of chars of book: $book chapter: $chapter is ${distinctCharSet.size}")
        distinctCharSet.forEach {
            print(it)
        }
    }
}

fun countTotalBibleLength() {

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

fun distinctChars(book: Int, chapter: Int): Set<Char> {
    val translation = Translation.krv
    val versePointer = VersePointer(translation = translation, book = book, chapter = chapter)
    val path = chapterTextPath(versePointer)
    val aChapter = File("src/main/resources/$path").readText()

    //convert String into Set of Characters
    return aChapter.toCharArray().distinct().toSet()
}

fun main(){

    val totalBibleCharSet = mutableSetOf<Char>()

    (1..66).forEach { book ->
        val maxChapter = Chapters.maxChapter(book)
        (1..maxChapter).forEach { chapter ->
            val distinctChars = distinctChars(book, chapter)
            totalBibleCharSet.addAll(distinctChars)
        }
    }

    val content = totalBibleCharSet.toList().sorted().map { it.toString() }.joinToString(separator = "")
    println(content)
}