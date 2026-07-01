package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.app.ScrollableColumn
import org.gnit.bible.app.VerseLayoutInfo
import org.gnit.bible.app.currentBible
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.ui.theme.BibleTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SingleBible(
    bibleState: BibleState,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    onVersePositioned: (Int, VerseLayoutInfo) -> Unit = { _, _ -> },
    highlightedVerse: Int? = null,
    onVerseTap: (Int) -> Unit = {},
    onVerseDoubleTap: (Int) -> Unit = {}
) {
    val bible = currentBible()
    val translation = bibleState.mainTranslation
    val book = bibleState.book
    val chapter = bibleState.chapter
    val chapterText = bible.verses(translation = translation.code, book = book, chapter = chapter)
    val verses = splitChapterToVerses(chapterText)

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange
    ) {
        verses.forEachIndexed { verse, text ->
            val background = animatedVerseBackgroundColor(bibleState, verse, highlightedVerse).value
            val textColor = animatedVerseTextColor(verse, highlightedVerse).value

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .verseTapGestures(
                        verse = verse + 1,
                        onVerseTap = onVerseTap,
                        onVerseDoubleTap = onVerseDoubleTap
                    )
                    .onGloballyPositioned { coordinates ->
                        onVersePositioned(
                            verse + 1,
                            VerseLayoutInfo(
                                topPx = coordinates.positionInParent().y.toInt(),
                                heightPx = coordinates.size.height
                            )
                        )
                    }
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
            ) {
                Text(
                    text = "${verse + 1} $text",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) {
                            translation.language.serifFontFamily()
                        } else {
                            translation.language.sansFontFamily()
                        },
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SingleBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        SingleBible(
            bibleState = BibleState(),
            scrollState = scrollState
        )
    }
}
