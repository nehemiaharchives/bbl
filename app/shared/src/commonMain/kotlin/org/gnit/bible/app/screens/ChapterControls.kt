package org.gnit.bible.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.ui.widgets.BibleButton
import org.gnit.bible.app.ui.widgets.BibleSlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterControlsBar(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit
) {
    var chapterSliderPosition by remember { mutableFloatStateOf(bibleState.chapter.toFloat()) }

    LaunchedEffect(bibleState.book, bibleState.chapter) {
        chapterSliderPosition = bibleState.chapter.toFloat()
    }

    Row(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(BUTTON_SIZE.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BibleButton(
            buttonText = "-",
            onClick = {
                if (bibleState.chapter != 1) {
                    chapterSliderPosition--
                    onStateChange(bibleState.prevChapter())
                    onAnyUserAction()
                    logger.debug { "BibleButton chapter changed ${bibleState.prevChapter()}" }
                }
            }
        )

        Spacer(modifier = Modifier.width(SPACE_BETWEEN_BUTTON_WITH_SLIDER.dp))

        BibleSlider(
            value = chapterSliderPosition,
            onValueChange = { newValue ->
                chapterSliderPosition = newValue
                onStateChange(bibleState.copy(chapter = newValue.roundToInt()))
                onAnyUserAction()
                logger.debug { "Slider chapter slider value changed to ${newValue.roundToInt()}" }
            },
            steps = (bibleState.lastChapter() - 2).coerceAtLeast(0),
            valueRange = 1f..bibleState.lastChapter().toFloat(),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.width(SPACE_BETWEEN_BUTTON_WITH_SLIDER.dp))

        BibleButton(
            buttonText = "+",
            onClick = {
                if (!bibleState.isLastChapter()) {
                    chapterSliderPosition++
                    onStateChange(bibleState.nextChapter())
                    onAnyUserAction()
                    logger.debug { "BibleButton chapter changed ${bibleState.nextChapter()}" }
                }
            }
        )
    }
}
