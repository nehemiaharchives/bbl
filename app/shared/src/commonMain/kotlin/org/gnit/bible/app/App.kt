package org.gnit.bible.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.gnit.bible.AssetManager
import org.gnit.bible.Bible
import org.gnit.bible.Platform
import org.gnit.bible.app.services.ProvideBibleAppEnvironment
import org.gnit.bible.app.services.currentBibleEnvironment
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.ui.theme.BibleTheme

@Composable
fun currentPlatform(): Platform = currentBibleEnvironment().platform

@Composable
fun currentAssetManager(): AssetManager = currentBibleEnvironment().assetManager

@Composable
fun currentBible(): Bible = currentBibleEnvironment().bible

@Composable
@Preview
fun App(platformContext: Any? = null, initialBibleState: BibleState? = null) {
    ProvideBibleAppEnvironment(platformContext = platformContext) {
        BibleTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                BibleApp(
                    initialBibleState = initialBibleState
                )
            }
        }
    }
}

@Composable
@Preview
fun AppInAutoHideMode() {
    ProvideBibleAppEnvironment {
        BibleTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                BibleApp(initialChromeVisible = false)
            }
        }
    }
}
