package org.gnit.bible.app.ui.widgets

import org.gnit.bible.SupportedTranslation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.Translation
import org.gnit.bible.app.ScrollableColumn
import org.gnit.bible.app.VerseLayoutInfo
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.ui.theme.BibleTheme
import androidx.compose.ui.tooling.preview.Preview

// serifFontFamily and sansFontFamily are in this package, no import needed

@Composable
fun BilingualUnderBible(
    bibleState: BibleState,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    onVersePositioned: (Int, VerseLayoutInfo) -> Unit = { _, _ -> }
) {
    val readingMode = bibleState.readingMode
    require(readingMode == ReadingMode.BILINGUAL_UNDER) { "ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} but trying to put $readingMode" }
    requireNotNull(bibleState.subTranslation) { "ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} so subTranslation is needed but null" }

    val versePairs = getVersePairs(bibleState)

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange
    ) {
        versePairs.forEachIndexed { verse, pair ->
            val background = if (bibleState.isZebraBackground && verse.isEven()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.background
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
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
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily()
                    ),
                )
                Text(
                    text = "${verse + 1} ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
                )
            }
        }
    }
}

private val downView = BibleState(SupportedTranslation.JC.translation, SupportedTranslation.WEBUS.translation, ReadingMode.BILINGUAL_UNDER)

@Preview(showBackground = true)
@Composable
fun BilingualUnderBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualUnderBible(
            bibleState = downView,
            scrollState = scrollState
        )
    }
}
