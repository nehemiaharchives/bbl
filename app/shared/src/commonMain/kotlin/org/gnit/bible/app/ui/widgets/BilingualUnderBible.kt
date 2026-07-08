package org.gnit.bible.app.ui.widgets

import org.gnit.bible.SupportedTranslation

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.app.BibleTextSelection
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
    selectedTextSelection: BibleTextSelection? = null,
    onVerseTap: (Int) -> Unit = {},
    onVerseDoubleTap: (Int) -> Unit = {},
    onVerseLongPress: (Int) -> Unit = {},
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
            val animatedMainBackground = animatedBilingualUnderTranslationBackgroundColor(
                bibleState = bibleState,
                verseIndex = verse,
                highlightedVerse = highlightedVerse,
                isSubTranslation = false
            ).value
            val animatedSubBackground = animatedBilingualUnderTranslationBackgroundColor(
                bibleState = bibleState,
                verseIndex = verse,
                highlightedVerse = highlightedVerse,
                isSubTranslation = true
            ).value
            val animatedTextColor = animatedVerseTextColor(verse, highlightedVerse).value
            val verseNumber = verse + 1
            val isMainSelected = selectedTextSelection?.containsBilingualVersePart(
                verse = verseNumber,
                isSubTranslation = false
            ) == true
            val isSubSelected = selectedTextSelection?.containsBilingualVersePart(
                verse = verseNumber,
                isSubTranslation = true
            ) == true
            val mainBackground = if (isMainSelected) MaterialTheme.colorScheme.primaryContainer else animatedMainBackground
            val subBackground = if (isSubSelected) MaterialTheme.colorScheme.primaryContainer else animatedSubBackground
            val mainTextColor = if (isMainSelected) MaterialTheme.colorScheme.onPrimaryContainer else animatedTextColor
            val subTextColor = if (isSubSelected) MaterialTheme.colorScheme.onPrimaryContainer else animatedTextColor

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
                                heightPx = coordinates.size.height
                            )
                        )
                    }
            ) {
                Text(
                    text = "$verseNumber ${pair.first}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily(),
                        color = mainTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(mainBackground)
                )
                Text(
                    text = "$verseNumber ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily(),
                        color = subTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(subBackground)
                )
                Spacer(modifier = Modifier.height(bibleState.spaceBetweenVerses.dp))
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
