package org.gnit.bible.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
fun BookControlsBar(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit
) {
    var bookSliderPosition by remember { mutableFloatStateOf(bibleState.book.toFloat()) }

    LaunchedEffect(bibleState.book) {
        bookSliderPosition = bibleState.book.toFloat()
    }

    Row(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal
                )
            )
            .height(BUTTON_SIZE.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BibleButton(
            buttonText = "-",
            onClick = {
                if (bibleState.book != 1) {
                    bookSliderPosition--
                    onStateChange(bibleState.prevBook())
                    onAnyUserAction()
                    logger.debug { "BibleButton book changed ${bibleState.prevBook()}" }
                }
            }
        )

        Spacer(modifier = Modifier.width(SPACE_BETWEEN_BUTTON_WITH_SLIDER.dp))

        BibleSlider(
            value = bookSliderPosition,
            onValueChange = { newValue ->
                bookSliderPosition = newValue
                onStateChange(bibleState.changeBook(newValue.roundToInt()))
                onAnyUserAction()
                logger.debug { "Slider book changed ${bibleState.changeBook(newValue.roundToInt())}" }
            },
            steps = 64,
            valueRange = 1f..66f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.width(SPACE_BETWEEN_BUTTON_WITH_SLIDER.dp))

        BibleButton(
            buttonText = "+",
            onClick = {
                if (bibleState.book != 66) {
                    bookSliderPosition++
                    onStateChange(bibleState.nextBook())
                    onAnyUserAction()
                    logger.debug { "BibleButton book changed ${bibleState.nextBook()}" }
                }
            }
        )
    }
}
