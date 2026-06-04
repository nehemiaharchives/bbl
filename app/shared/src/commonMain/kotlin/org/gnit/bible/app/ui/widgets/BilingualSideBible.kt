package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.Translation
import org.gnit.bible.app.ScrollableColumn
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.ui.theme.BibleTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BilingualSideBible(
    bibleState: BibleState,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {}
) {
    val readingMode = bibleState.readingMode
    require(readingMode == ReadingMode.BILINGUAL_SIDE) { "ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} but trying to put $readingMode" }
    requireNotNull(bibleState.subTranslation) { "ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} so subTranslation is needed but null" }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
            ) {
                Text(
                    text = "${verse + 1} ${pair.first}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${verse + 1} ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val sideView = BibleState(Translation.jc, Translation.webus, ReadingMode.BILINGUAL_SIDE)

@Preview(showBackground = true)
@Composable
fun BilingualSideBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualSideBible(
            bibleState = sideView,
            scrollState = scrollState
        )
    }
}
