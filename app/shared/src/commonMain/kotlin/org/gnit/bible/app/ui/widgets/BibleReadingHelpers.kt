package org.gnit.bible.app.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import org.gnit.bible.Bible
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.app.currentBible
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.historySaveEventColorTransitionDurationSeconds

internal fun Int.isEven() = this % 2 == 0

private fun isHighlightedVerse(verseIndex: Int, highlightedVerse: Int?): Boolean {
    return highlightedVerse == verseIndex + 1
}

@Composable
private fun normalVerseBackgroundColor(bibleState: BibleState, verseIndex: Int): Color {
    return if (bibleState.isZebraBackground && verseIndex.isEven()) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.background
    }
}

internal fun Modifier.verseTapGestures(
    verse: Int,
    onVerseTap: (Int) -> Unit,
    onVerseDoubleTap: (Int) -> Unit
): Modifier {
    return pointerInput(verse) {
        detectTapGestures(
            onTap = { onVerseTap(verse) },
            onDoubleTap = { onVerseDoubleTap(verse) }
        )
    }
}

@Composable
internal fun animatedVerseBackgroundColor(
    bibleState: BibleState,
    verseIndex: Int,
    highlightedVerse: Int?
): State<Color> {
    val target = if (isHighlightedVerse(verseIndex, highlightedVerse)) {
        MaterialTheme.colorScheme.primary
    } else {
        normalVerseBackgroundColor(bibleState, verseIndex)
    }
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(historySaveEventColorTransitionDurationSeconds * 1_000 / 2),
        label = "verseHistoryBackground"
    )
}

@Composable
internal fun animatedVerseTextColor(
    verseIndex: Int,
    highlightedVerse: Int?
): State<Color> {
    val target = if (isHighlightedVerse(verseIndex, highlightedVerse)) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(historySaveEventColorTransitionDurationSeconds * 1_000 / 2),
        label = "verseHistoryText"
    )
}

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
