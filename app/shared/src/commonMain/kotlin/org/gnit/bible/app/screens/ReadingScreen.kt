package org.gnit.bible.app

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.state.SHARED_PREFERENCE_KEY_BIBLE_STATE
import org.gnit.bible.app.ui.widgets.BilingualSideBible
import org.gnit.bible.app.ui.widgets.BilingualUnderBible
import org.gnit.bible.app.ui.widgets.SingleBible
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val AUTO_HIDE_MS: Long = 60_000

@Composable
fun BibleReadingArea(
    state: BibleState,
    onStateChange: (BibleState) -> Unit,
    chrome: ChromeAutoHide,
    innerPadding: PaddingValues
) {
    var zoom by remember { mutableFloatStateOf(state.fontSize.toFloat()) }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(state.fontSize) {
        zoom = state.fontSize.toFloat()
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress ->
            if (inProgress && chrome.isVisible()) {
                chrome.onUserInteraction()
            }
        }
    }

    val tapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                if (chrome.isVisible()) {
                    chrome.forceHide()
                } else {
                    chrome.forceShow()
                }
            }
        )
    }

    val pinchZoomModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            do {
                val event = awaitPointerEvent()
                val oldZoom = zoom

                if (zoom in 5f..400f) {
                    zoom *= event.calculateZoom()

                    if (oldZoom != zoom) {
                        val intZoomValue = zoom.roundToInt().coerceIn(5, 400)
                        if (currentState.fontSize != intZoomValue) {
                            onStateChange(currentState.copy(fontSize = intZoomValue))
                            if (chrome.isVisible()) {
                                chrome.onUserInteraction()
                            }
                        }
                        zoom = intZoomValue.toFloat()
                    }
                } else if (zoom > 400f) {
                    zoom = 399.9f
                } else if (zoom < 5f) {
                    zoom = 5.1f
                }
            } while (event.changes.any { it.pressed })
        }
    }

    val topChromePadding = 0.dp
    val bottomChromePadding = 0.dp
    val onScrollPercentChange: (Float) -> Unit = { scrollPercent ->
        onStateChange(currentState.copy(scrollPercent = scrollPercent))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(top = topChromePadding, bottom = bottomChromePadding)
            .then(tapModifier)
            .then(pinchZoomModifier)
    ) {
        when (state.readingMode) {
            ReadingMode.SINGLE -> SingleBible(state, scrollState, onScrollPercentChange)
            ReadingMode.BILINGUAL_SIDE -> BilingualSideBible(state, scrollState, onScrollPercentChange)
            ReadingMode.BILINGUAL_UNDER -> BilingualUnderBible(state, scrollState, onScrollPercentChange)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun rememberChromeAutoHide(initiallyVisible: Boolean = true): ChromeAutoHide {
    var visible by remember { mutableStateOf(initiallyVisible) }
    var lastInteraction by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var pauseHide by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteraction, pauseHide) {
        if (pauseHide) return@LaunchedEffect
        val started = lastInteraction
        delay(AUTO_HIDE_MS)
        if (!pauseHide && lastInteraction == started) visible = false
    }

    fun bump() {
        lastInteraction = Clock.System.now().toEpochMilliseconds()
        if (!visible) visible = true
    }

    fun hide() {
        visible = false
    }

    return remember {
        ChromeAutoHide(
            isVisible = { visible },
            onUserInteraction = { bump() },
            forceShow = { visible = true; bump() },
            forceHide = { hide() },
            setPause = { pause ->
                pauseHide = pause
                if (pause) visible = true
            }
        )
    }
}

class ChromeAutoHide(
    val isVisible: () -> Boolean,
    val onUserInteraction: () -> Unit,
    val forceShow: () -> Unit,
    val forceHide: () -> Unit,
    val setPause: (Boolean) -> Unit
)

@Composable
fun ScrollableColumn(
    bibleState: BibleState,
    scrollState: ScrollState,
    onScrollPercentChange: (Float) -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        content()
    }
    LaunchedEffect(bibleState.book) { scrollState.scrollTo(0) }
    LaunchedEffect(bibleState.chapter) { scrollState.scrollTo(0) }
    LaunchedEffect(bibleState.readingMode) {
        val scrollValue = (scrollState.maxValue * bibleState.scrollPercent).toInt()
        scrollState.scrollTo(scrollValue)
    }

    val sharedPreferences = currentPlatform().settings
    LaunchedEffect(scrollState) {
        val lastScrollValue = scrollState.value
        snapshotFlow { scrollState.value }
            .collectLatest { newValue ->
                if (newValue != lastScrollValue) {
                    delay(200)
                    if (!scrollState.isScrollInProgress) {
                        val scrollPercent = computeScrollPercent(newValue, scrollState)
                        onScrollPercentChange(scrollPercent)
                        sharedPreferences.putString(
                            SHARED_PREFERENCE_KEY_BIBLE_STATE,
                            bibleState.copy(scrollPercent = scrollPercent).toJson()
                        )
                        logger.debug { "ScrollableColumn Saved scroll scrollPercent: $scrollPercent" }
                    }
                }
            }
    }
}

private fun computeScrollPercent(scrollValue: Int, scrollState: ScrollState): Float {
    val totalScrollableHeight = scrollState.maxValue
    if (totalScrollableHeight <= 0) return 0f
    return (scrollValue.toFloat() / totalScrollableHeight).coerceIn(0f, 1f)
}
