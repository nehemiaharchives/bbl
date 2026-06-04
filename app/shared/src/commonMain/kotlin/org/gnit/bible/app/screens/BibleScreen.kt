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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var reopenDropdownAfterManager by rememberSaveable { mutableStateOf(false) }

    logger.debug { "Bible Lifecycle by rememberSavable { mutableStateOf(initialState) } called, bibleState:$bibleState" }

    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleResumeEffect(key1 = lifecycleOwner) {
        onPauseOrDispose {
            logger.debug { "Bible Lifecycle onPauseOrDispose called, saving bibleState:$bibleState" }
            platform.settings.putString(SHARED_PREFERENCE_KEY_BIBLE_STATE, bibleState.toJson())
        }
    }

    val chrome = rememberChromeAutoHide(initialChromeVisible)

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
                            onOpenTranslationManager = { showTranslationManager = true },
                            hideDropdown = showTranslationManager,
                            reopenDropdown = reopenDropdownAfterManager,
                            onDropdownReopened = { reopenDropdownAfterManager = false }
                        )
                        BookControlsBar(
                            bibleState = bibleState,
                            onStateChange = { bibleState = it },
                            onAnyUserAction = { chrome.onUserInteraction() }
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = chrome.isVisible(),
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
            BibleReadingArea(
                state = bibleState,
                onStateChange = { bibleState = it },
                chrome = chrome,
                innerPadding = innerPadding
            )
        }

        if (showTranslationManager) {
            PlatformBackHandler(enabled = showTranslationManager) {
                showTranslationManager = false
                reopenDropdownAfterManager = true
            }

            TranslationManagerScreen(
                bibleState = bibleState,
                onStateChange = { bibleState = it },
                onClose = {
                    showTranslationManager = false
                    reopenDropdownAfterManager = true
                }
            )
        }
    }
}
