package org.gnit.bible.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.BibleStateSaver
import org.gnit.bible.app.state.SHARED_PREFERENCE_KEY_BIBLE_STATE
import org.gnit.bible.app.state.rememberBibleState
import org.gnit.bible.app.ui.widgets.TranslationManagerScreen

@Composable
fun BibleApp(
    initialChromeVisible: Boolean = true,
    initialBibleState: BibleState? = null
) {
    val platform = currentPlatform()

    val initialState = initialBibleState ?: rememberBibleState()
    var bibleState by rememberSaveable(stateSaver = BibleStateSaver) {
        mutableStateOf(initialState)
    }
    var showTranslationManager by rememberSaveable { mutableStateOf(false) }
    var hideDropdownForTranslationManager by rememberSaveable { mutableStateOf(false) }
    var closeTranslationManagerAfterDropdownRestored by rememberSaveable { mutableStateOf(false) }

    logger.debug { "Bible Lifecycle by rememberSavable { mutableStateOf(initialState) } called, bibleState:$bibleState" }

    val lifecycleOwner = LocalLifecycleOwner.current
    val latestBibleState by rememberUpdatedState(bibleState)
    LifecycleResumeEffect(key1 = lifecycleOwner) {
        onPauseOrDispose {
            val persistedState = latestBibleState.clearSearch()
            logger.debug { "Bible Lifecycle onPauseOrDispose called, saving bibleState:$persistedState" }
            platform.settings.putString(SHARED_PREFERENCE_KEY_BIBLE_STATE, persistedState.toJson())
        }
    }

    val chrome = rememberChromeAutoHide(initialChromeVisible)
    var wasChromeVisibleBeforeSearch by rememberSaveable { mutableStateOf(initialChromeVisible) }
    val isChromeVisible = chrome.isVisible()
    val shouldShowTopChromeContent = bibleState.isSearchActive || isChromeVisible || wasChromeVisibleBeforeSearch
    val shouldShowReadingChromeControls = !bibleState.isSearchActive && (isChromeVisible || wasChromeVisibleBeforeSearch)
    val density = LocalDensity.current
    var topChromeHeightPx by rememberSaveable { mutableStateOf(0) }
    var bookControlsHeightPx by rememberSaveable { mutableStateOf(0) }
    var bottomChromeHeightPx by rememberSaveable { mutableStateOf(0) }
    val topChromeHeight = with(density) { topChromeHeightPx.toDp() }
    val bookControlsHeight = if (bookControlsHeightPx > 0) {
        with(density) { bookControlsHeightPx.toDp() }
    } else {
        BUTTON_SIZE.dp
    }
    val bottomChromeHeight = with(density) { bottomChromeHeightPx.toDp() }
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topReadingSpacer = if (!bibleState.isSearchActive) bookControlsHeight else 0.dp
    val bottomReadingSpacer = if (bottomChromeHeight > navigationBarHeight) {
        bottomChromeHeight - navigationBarHeight
    } else {
        0.dp
    }

    LaunchedEffect(bibleState.isSearchActive) {
        chrome.setPause(bibleState.isSearchActive)
        if (bibleState.isSearchActive) chrome.forceShow()
    }

    LaunchedEffect(closeTranslationManagerAfterDropdownRestored) {
        if (closeTranslationManagerAfterDropdownRestored) {
            withFrameNanos { }
            showTranslationManager = false
            closeTranslationManagerAfterDropdownRestored = false
        }
    }

    fun startSearch() {
        wasChromeVisibleBeforeSearch = chrome.isVisible()
        bibleState = bibleState.startSearch()
        chrome.forceShow()
    }

    fun cancelSearch() {
        bibleState = bibleState.handleBack() ?: bibleState.clearSearch()
        chrome.setPause(false)
        if (wasChromeVisibleBeforeSearch) {
            chrome.forceShow()
        } else {
            chrome.forceHide()
        }
    }

    fun closeTranslationManager() {
        hideDropdownForTranslationManager = false
        closeTranslationManagerAfterDropdownRestored = true
    }

    PlatformBackHandler(enabled = bibleState.isSearchActive || bibleState.backStack.isNotEmpty()) {
        if (bibleState.isSearchActive) {
            cancelSearch()
        } else {
            bibleState.handleBack()?.let { nextState ->
                bibleState = nextState
                if (nextState.isSearchActive) chrome.forceShow() else chrome.onUserInteraction()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val activeSearchQuery = bibleState.submittedSearchQuery
        if (activeSearchQuery == null) {
            BibleReadingArea(
                state = bibleState,
                onStateChange = { bibleState = it },
                chrome = chrome,
                innerPadding = PaddingValues(0.dp),
                topContentPadding = topReadingSpacer,
                bottomContentPadding = bottomReadingSpacer,
                onSearchRequested = { startSearch() },
                onSearchCancel = { cancelSearch() }
            )
        } else {
            SearchResultsScreen(
                bibleState = bibleState,
                query = activeSearchQuery,
                innerPadding = PaddingValues(
                    top = topChromeHeight,
                    bottom = if (bibleState.isSearchActive) 0.dp else bottomChromeHeight
                ),
                onResultClick = { pointer ->
                    bibleState = bibleState.openSearchResult(pointer)
                    chrome.onUserInteraction()
                }
            )
        }

        AnimatedVisibility(
            visible = isChromeVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { size ->
                    if (size.height > 0) topChromeHeightPx = size.height
                },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 0.dp)) {
                    if (shouldShowTopChromeContent) {
                        TopBarContent(
                            bibleState = bibleState,
                            onStateChange = { bibleState = it },
                            onAnyUserAction = { chrome.onUserInteraction() },
                            onDropdownVisibilityChange = { isOpen ->
                                chrome.setPause(isOpen)
                                if (isOpen) chrome.forceShow() else chrome.onUserInteraction()
                            },
                            onOpenTranslationManager = {
                                hideDropdownForTranslationManager = true
                                showTranslationManager = true
                            },
                            hideDropdown = hideDropdownForTranslationManager,
                            isSearchActive = bibleState.isSearchActive,
                            searchQuery = bibleState.searchQuery,
                            onSearchQueryChange = { bibleState = bibleState.copy(searchQuery = it) },
                            onSearchRequested = { startSearch() },
                            onSearchSubmit = {
                                val trimmedQuery = bibleState.searchQuery.trim()
                                if (trimmedQuery.isNotEmpty()) {
                                    bibleState = bibleState.submitSearch(trimmedQuery)
                                    chrome.forceShow()
                                }
                            },
                            onSearchCancel = { cancelSearch() }
                        )
                    }
                    if (shouldShowReadingChromeControls) {
                        Box(
                            modifier = Modifier.onSizeChanged { size ->
                                if (size.height > 0) bookControlsHeightPx = size.height
                            }
                        ) {
                            BookControlsBar(
                                bibleState = bibleState,
                                onStateChange = { bibleState = it },
                                onAnyUserAction = { chrome.onUserInteraction() }
                            )
                        }
                    }
                }
            }
        }

        if (!bibleState.isSearchActive) {
            AnimatedVisibility(
                visible = isChromeVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.height > 0) bottomChromeHeightPx = size.height
                    },
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    ChapterControlsBar(
                        bibleState = bibleState,
                        onStateChange = { bibleState = it },
                        onAnyUserAction = { chrome.onUserInteraction() }
                    )
                }
            }
        }

        if (showTranslationManager) {
            PlatformBackHandler(enabled = showTranslationManager) {
                closeTranslationManager()
            }

            TranslationManagerScreen(
                bibleState = bibleState,
                onStateChange = { bibleState = it },
                onClose = {
                    closeTranslationManager()
                }
            )
        }
    }
}
