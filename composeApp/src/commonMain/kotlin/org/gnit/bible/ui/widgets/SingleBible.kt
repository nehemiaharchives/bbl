package org.gnit.bible.ui.widgets

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
import org.gnit.bible.Bible.Companion.splitChapterToVerses
import org.gnit.bible.BibleState
import org.gnit.bible.ScrollableColumn
import org.gnit.bible.bible
import org.gnit.bible.ui.theme.BibleTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SingleBible(
    bibleState: BibleState,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {}
) {
    val translation = bibleState.mainTranslation
    val book = bibleState.book
    val chapter = bibleState.chapter
    val chapterText = bible().verses(translation = translation.code, book = book, chapter = chapter)
    val verses = splitChapterToVerses(chapterText)

    ScrollableColumn(
        bibleState = bibleState,
        scrollState = scrollState,
        onScrollPercentChange = onScrollPercentChange
    ) {
        verses.forEachIndexed { verse, text ->
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
                    text = "${verse + 1} $text",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) {
                            translation.language.serifFontFamily()
                        } else {
                            translation.language.sansFontFamily()
                        }
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
