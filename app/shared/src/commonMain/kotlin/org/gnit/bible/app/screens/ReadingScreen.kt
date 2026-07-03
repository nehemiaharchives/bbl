package org.gnit.bible.app

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.state.SHARED_PREFERENCE_KEY_BIBLE_STATE
import org.gnit.bible.app.state.historySaveEventColorTransitionDurationSeconds
import org.gnit.bible.app.ui.widgets.BilingualSideBible
import org.gnit.bible.app.ui.widgets.BilingualUnderBible
import org.gnit.bible.app.ui.widgets.SingleBible
import org.gnit.bible.app.ui.widgets.sansFontFamily
import org.gnit.bible.app.ui.widgets.serifFontFamily
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

private const val AUTO_HIDE_MS: Long = 60_000

@Composable
fun BibleReadingArea(
    state: BibleState,
    onStateChange: (BibleState) -> Unit,
    chrome: ChromeAutoHide,
    innerPadding: PaddingValues,
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(state.fontSize.toFloat()) }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(state.fontSize) {
        zoom = state.fontSize.toFloat()
    }

    val scrollState = rememberScrollState()
    val verseLayouts = remember(
        state.book,
        state.chapter,
        state.readingMode,
        state.fontSize,
        state.spaceBetweenVerses,
        state.isFontFamilySerif
    ) {
        mutableStateMapOf<Int, VerseLayoutInfo>()
    }
    var viewportHeight by remember { mutableStateOf(0) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress ->
            if (inProgress && chrome.isVisible()) {
                chrome.onUserInteraction()
            }
        }
    }

    val pinchZoomModifier = Modifier.pointerInput(Unit) {
        fun applyZoom(multiplier: Float) {
            val oldZoom = zoom
            zoom = (zoom * multiplier).coerceIn(5f, 400f)

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
        }

        awaitEachGesture {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)

                if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isCtrlPressed) {
                    val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (scrollY != 0f) {
                        applyZoom(if (scrollY < 0f) 1.15f else 1f / 1.15f)
                        event.changes.forEach { it.consume() }
                    }
                } else {
                    val zoomChange = event.calculateZoom()
                    if (zoomChange != 1f) {
                        applyZoom(zoomChange)
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }

    val topChromePadding = 0.dp
    val bottomChromePadding = 0.dp
    val onScrollPercentChange: (Float) -> Unit = { scrollPercent ->
        onStateChange(currentState.copy(scrollPercent = scrollPercent))
    }
    val onVerseTap: (Int) -> Unit = {
        if (chrome.isVisible()) {
            chrome.forceHide()
        } else {
            chrome.forceShow()
        }
    }
    val onVerseDoubleTap: (Int) -> Unit = { verse ->
        onStateChange(currentState.recordReadHistory(verse))
        chrome.onUserInteraction()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(innerPadding)
            .padding(top = topChromePadding, bottom = bottomChromePadding)
            .clipToBounds()
            .onSizeChanged { viewportHeight = it.height }
            .then(pinchZoomModifier)
    ) {
        when (state.readingMode) {
            ReadingMode.SINGLE -> SingleBible(
                state,
                scrollState,
                onScrollPercentChange,
                onVersePositioned = { verse, layout -> verseLayouts[verse] = layout },
                highlightedVerse = state.highlightedVerse,
                onVerseTap = onVerseTap,
                onVerseDoubleTap = onVerseDoubleTap,
                topContentPadding = topContentPadding,
                bottomContentPadding = bottomContentPadding
            )
            ReadingMode.BILINGUAL_SIDE -> BilingualSideBible(
                state,
                scrollState,
                onScrollPercentChange,
                onVersePositioned = { verse, layout -> verseLayouts[verse] = layout },
                highlightedVerse = state.highlightedVerse,
                onVerseTap = onVerseTap,
                onVerseDoubleTap = onVerseDoubleTap,
                topContentPadding = topContentPadding,
                bottomContentPadding = bottomContentPadding
            )
            ReadingMode.BILINGUAL_UNDER -> BilingualUnderBible(
                state,
                scrollState,
                onScrollPercentChange,
                onVersePositioned = { verse, layout -> verseLayouts[verse] = layout },
                highlightedVerse = state.highlightedVerse,
                onVerseTap = onVerseTap,
                onVerseDoubleTap = onVerseDoubleTap,
                topContentPadding = topContentPadding,
                bottomContentPadding = bottomContentPadding
            )
        }
    }

    LaunchedEffect(state.highlightedVerse) {
        val verse = state.highlightedVerse ?: return@LaunchedEffect
        delay(((historySaveEventColorTransitionDurationSeconds * 1_000L) / 2).milliseconds)
        onStateChange(currentState.clearHistoryHighlight(verse))
    }

    LaunchedEffect(state.centerVerse, scrollState.maxValue, viewportHeight, verseLayouts.size) {
        val targetVerse = state.centerVerse ?: return@LaunchedEffect
        val layout = verseLayouts[targetVerse] ?: return@LaunchedEffect
        if (viewportHeight <= 0 || scrollState.maxValue <= 0) return@LaunchedEffect

        val scrollPercent = computeCenteredScrollPercent(
            verseTopPx = layout.topPx,
            verseHeightPx = layout.heightPx,
            viewportHeightPx = viewportHeight,
            totalScrollableHeightPx = scrollState.maxValue
        )
        scrollState.scrollTo((scrollState.maxValue * scrollPercent).roundToInt())
        onStateChange(currentState.copy(scrollPercent = scrollPercent, centerVerse = null))
    }
}

data class VerseLayoutInfo(
    val topPx: Int,
    val heightPx: Int
)

fun computeCenteredScrollPercent(
    verseTopPx: Int,
    verseHeightPx: Int,
    viewportHeightPx: Int,
    totalScrollableHeightPx: Int
): Float {
    if (totalScrollableHeightPx <= 0) return 0f
    val targetScroll = verseTopPx + (verseHeightPx / 2f) - (viewportHeightPx / 2f)
    return (targetScroll / totalScrollableHeightPx).coerceIn(0f, 1f)
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
        delay(AUTO_HIDE_MS.milliseconds)
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
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        ReadingTitleHeader(bibleState)
        if (topContentPadding > 0.dp) {
            Spacer(modifier = Modifier.height(topContentPadding))
        }
        content()
        if (bottomContentPadding > 0.dp) {
            Spacer(modifier = Modifier.height(bottomContentPadding))
        }
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
                    delay(200.milliseconds)
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

@Composable
private fun ReadingTitleHeader(bibleState: BibleState) {
    val titleFontFamily = if (bibleState.isFontFamilySerif) {
        bibleState.mainTranslation.language.serifFontFamily()
    } else {
        bibleState.mainTranslation.language.sansFontFamily()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = bibleState.describeBookChapter(),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = titleFontFamily,
                fontSize = (max(min(bibleState.fontSize * 1.4F, 40.0F), 16F)).sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = max(min(bibleState.fontSize, 10), 5).dp)
        )
    }
}

private fun computeScrollPercent(scrollValue: Int, scrollState: ScrollState): Float {
    val totalScrollableHeight = scrollState.maxValue
    if (totalScrollableHeight <= 0) return 0f
    return (scrollValue.toFloat() / totalScrollableHeight).coerceIn(0f, 1f)
}
