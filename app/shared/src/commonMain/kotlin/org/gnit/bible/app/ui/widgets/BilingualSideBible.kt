package org.gnit.bible.app.ui.widgets

import org.gnit.bible.SupportedTranslation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import org.gnit.bible.app.ScrollableColumn
import org.gnit.bible.app.VerseLayoutInfo
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.ui.theme.BibleTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BilingualSideBible(
    bibleState: BibleState,
    versePairs: List<Pair<String, String>>,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    onVersePositioned: (Int, VerseLayoutInfo) -> Unit = { _, _ -> },
    highlightedVerse: Int? = null,
    onVerseTap: (Int) -> Unit = {},
    onVerseDoubleTap: (Int) -> Unit = {},
    onVerseLongPress: ((Int) -> Unit)? = null,
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    onTitleTap: () -> Unit = {}
) {
    val readingMode = bibleState.readingMode
    require(readingMode == ReadingMode.BILINGUAL_SIDE) { "ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} but trying to put $readingMode" }
    requireNotNull(bibleState.subTranslation) { "ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} so subTranslation is needed but null" }
    val verseSpacingPx = with(LocalDensity.current) { bibleState.spaceBetweenVerses.dp.roundToPx() }

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onTitleTap = onTitleTap
    ) {
        SelectionContainer {
            Column {
                versePairs.forEachIndexed { verse, pair ->
                    val animatedBackground = animatedVerseBackgroundColor(bibleState, verse, highlightedVerse).value
                    val animatedTextColor = animatedVerseTextColor(verse, highlightedVerse).value
                    val verseNumber = verse + 1

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verseTapGestures(
                                verse = verseNumber,
                                onVerseTap = onVerseTap,
                                onVerseDoubleTap = onVerseDoubleTap,
                                onVerseLongPress = onVerseLongPress
                            )
                            .onGloballyPositioned { coordinates ->
                                onVersePositioned(
                                    verseNumber,
                                    VerseLayoutInfo(
                                        topPx = coordinates.positionInParent().y.toInt(),
                                        heightPx = (coordinates.size.height - verseSpacingPx).coerceAtLeast(0)
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$verseNumber ${pair.first}",
                                style = TextStyle(
                                    fontSize = bibleState.fontSize.sp,
                                    fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily(),
                                    color = animatedTextColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(animatedBackground)
                            )
                            Text(
                                text = "$verseNumber ${pair.second}",
                                style = TextStyle(
                                    fontSize = bibleState.fontSize.sp,
                                    fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily(),
                                    color = animatedTextColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(animatedBackground)
                            )
                        }
                        Spacer(modifier = Modifier.height(bibleState.spaceBetweenVerses.dp))
                    }
                }
            }
        }
    }
}

private val sideView = BibleState(
    SupportedTranslation.JC.translation,
    SupportedTranslation.WEBUS.translation,
    ReadingMode.BILINGUAL_SIDE
)

@Preview(showBackground = true)
@Composable
fun BilingualSideBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualSideBible(
            bibleState = sideView,
            versePairs = listOf("Sample main verse." to "Sample sub verse."),
            scrollState = scrollState
        )
    }
}
