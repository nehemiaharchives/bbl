package org.gnit.bible.app.ui.widgets

import org.gnit.bible.SupportedTranslation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
fun BilingualUnderBible(
    bibleState: BibleState,
    versePairs: List<Pair<String, String>>,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    onVersePositioned: (Int, VerseLayoutInfo) -> Unit = { _, _ -> },
    highlightedVerse: Int? = null,
    onVerseTap: (Int) -> Unit = {},
    onVerseDoubleTap: (Int) -> Unit = {},
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    onTitleTap: () -> Unit = {}
) {
    val readingMode = bibleState.readingMode
    require(readingMode == ReadingMode.BILINGUAL_UNDER) { "Expected ${ReadingMode.BILINGUAL_UNDER}, got $readingMode" }
    requireNotNull(bibleState.subTranslation) { "subTranslation is required for ${ReadingMode.BILINGUAL_UNDER}" }

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange,
        topContentPadding = topContentPadding,
        bottomContentPadding = bottomContentPadding,
        onTitleTap = onTitleTap
    ) {
        versePairs.forEachIndexed { verse, pair ->
            val background = animatedVerseBackgroundColor(bibleState, verse, highlightedVerse).value
            val textColor = animatedVerseTextColor(verse, highlightedVerse).value

            Column(
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
            ) {
                Text(
                    text = "${verse + 1} ${pair.first}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily(),
                        color = textColor
                    ),
                )
                Text(
                    text = "${verse + 1} ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily(),
                        color = textColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
                )
            }
        }
    }
}

private val downView = BibleState(
    SupportedTranslation.JC.translation,
    SupportedTranslation.WEBUS.translation,
    ReadingMode.BILINGUAL_UNDER
)

@Preview(showBackground = true)
@Composable
fun BilingualUnderBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualUnderBible(
            bibleState = downView,
            versePairs = listOf("Main" to "Sub"),
            scrollState = scrollState
        )
    }
}