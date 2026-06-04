package org.gnit.bible.app.ui.widgets

import androidx.compose.runtime.Composable
import org.gnit.bible.Bible
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.app.currentBible
import org.gnit.bible.app.state.BibleState

internal fun Int.isEven() = this % 2 == 0

fun addEmptyEntryToMakeSameSize(
    listA: List<String>,
    listB: List<String>
): Pair<List<String>, List<String>> {
    val longerList = if (listA.size > listB.size) listA else listB
    val shorterList = if (listA.size < listB.size) listA else listB

    val paddedShorterList = shorterList.toMutableList()
    repeat(longerList.size - shorterList.size) {
        paddedShorterList.add("")
    }

    val newListA = if (listA == longerList) longerList else paddedShorterList
    val newListB = if (listA == longerList) paddedShorterList else longerList

    return newListA to newListB
}

@Composable
fun getVersePairs(bibleState: BibleState): List<Pair<String, String>> {
    val bible = currentBible()
    return getVersePairs(bible, bibleState)
}

fun getVersePairs(bible: Bible, bibleState: BibleState): List<Pair<String, String>> {
    val mainTranslation = bibleState.mainTranslation
    val subTranslation = bibleState.subTranslation
        ?: throw IllegalArgumentException("subTranslation is required but null")

    val book = bibleState.book
    val chapter = bibleState.chapter

    val mainChapterText = bible.verses(translation = mainTranslation.code, book = book, chapter = chapter)
    val subChapterText = bible.verses(translation = subTranslation.code, book = book, chapter = chapter)

    val mainVerses = splitChapterToVerses(mainChapterText)
    val subVerses = splitChapterToVerses(subChapterText)

    return if (mainVerses.size == subVerses.size) {
        mainVerses.zip(subVerses).toList()
    } else {
        val newPair = addEmptyEntryToMakeSameSize(mainVerses.toList(), subVerses.toList())
        newPair.first.zip(newPair.second)
    }
}
