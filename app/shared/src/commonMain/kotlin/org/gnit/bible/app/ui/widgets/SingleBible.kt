package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.app.BibleTextSelection
import org.gnit.bible.app.ScrollableColumn
import org.gnit.bible.app.VerseLayoutInfo
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.ui.theme.BibleTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SingleBible(
    bibleState: BibleState,
    verses: Array<String>,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    onVersePositioned: (Int, VerseLayoutInfo) -> Unit = { _, _ -> },
    highlightedVerse: Int? = null,
    selectedTextSelection: BibleTextSelection? = null,
    onVerseTap: (Int) -> Unit = {},
    onVerseDoubleTap: (Int) -> Unit = {},
    onVerseLongPress: (Int) -> Unit = {},
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    onTitleTap: () -> Unit = {}
) {
    val translation = bibleState.mainTranslation
    val verseSpacingPx = with(LocalDensity.current) { bibleState.spaceBetweenVerses.dp.roundToPx() }

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onTitleTap = onTitleTap
    ) {
        verses.forEachIndexed { verse, text ->
            val animatedBackground = animatedVerseBackgroundColor(bibleState, verse, highlightedVerse).value
            val animatedTextColor = animatedVerseTextColor(verse, highlightedVerse).value
            val isSelected = selectedTextSelection?.containsSingleVerse(verse + 1) == true
            val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else animatedBackground
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else animatedTextColor

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verseTapGestures(
                        verse = verse + 1,
                        onVerseTap = onVerseTap,
                        onVerseDoubleTap = onVerseDoubleTap,
                        onVerseLongPress = onVerseLongPress
                    )
                    .onGloballyPositioned { coordinates ->
                        onVersePositioned(
                            verse + 1,
                            VerseLayoutInfo(
                                topPx = coordinates.positionInParent().y.toInt(),
                                heightPx = (coordinates.size.height - verseSpacingPx).coerceAtLeast(0)
                            )
                        )
                    }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                )
                Spacer(modifier = Modifier.height(bibleState.spaceBetweenVerses.dp))
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
            verses = arrayOf("In the beginning God created the heaven and the earth."),
            scrollState = scrollState
        )
    }
}
