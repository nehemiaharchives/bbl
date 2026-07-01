package org.gnit.bible.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

    fun closeTranslationManager() {
        hideDropdownForTranslationManager = false
        closeTranslationManagerAfterDropdownRestored = true
    }

    PlatformBackHandler(enabled = bibleState.isSearchActive || bibleState.backStack.isNotEmpty()) {
        bibleState.handleBack()?.let { nextState ->
            bibleState = nextState
            if (nextState.isSearchActive) chrome.forceShow() else chrome.onUserInteraction()
        }
    }

    Box {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = chrome.isVisible(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = androidx.compose.ui.Modifier.padding(vertical = 0.dp)) {
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
                            onSearchRequested = {
                                bibleState = bibleState.startSearch()
                                chrome.forceShow()
                            },
                            onSearchSubmit = {
                                val trimmedQuery = bibleState.searchQuery.trim()
                                if (trimmedQuery.isNotEmpty()) {
                                    bibleState = bibleState.submitSearch(trimmedQuery)
                                    chrome.forceShow()
                                }
                            },
                            onSearchCancel = {
                                bibleState = bibleState.handleBack() ?: bibleState.clearSearch()
                                chrome.onUserInteraction()
                            }
                        )
                        AnimatedVisibility(
                            visible = !bibleState.isSearchActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            BookControlsBar(
                                bibleState = bibleState,
                                onStateChange = { bibleState = it },
                                onAnyUserAction = { chrome.onUserInteraction() }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = chrome.isVisible() && !bibleState.isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = androidx.compose.ui.Modifier.navigationBarsPadding()
                    ) {
                        ChapterControlsBar(
                            bibleState = bibleState,
                            onStateChange = { bibleState = it },
                            onAnyUserAction = { chrome.onUserInteraction() }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val activeSearchQuery = bibleState.submittedSearchQuery
            if (activeSearchQuery == null) {
                BibleReadingArea(
                    state = bibleState,
                    onStateChange = { bibleState = it },
                    chrome = chrome,
                    innerPadding = innerPadding
                )
            } else {
                SearchResultsScreen(
                    bibleState = bibleState,
                    query = activeSearchQuery,
                    innerPadding = innerPadding,
                    onResultClick = { pointer ->
                        bibleState = bibleState.openSearchResult(pointer)
                        chrome.onUserInteraction()
                    }
                )
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
