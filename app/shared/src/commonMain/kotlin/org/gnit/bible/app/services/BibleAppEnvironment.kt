package org.gnit.bible.app.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import org.gnit.bible.AssetManager
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Platform
import org.gnit.bible.app.ComposeBibleResourcesReader
import org.gnit.bible.getPlatform

data class BibleAppEnvironment(
    val platform: Platform,
    val assetManager: AssetManager,
    val bible: Bible
)

private fun createBibleAppEnvironment(
    platformContext: Any? = null
): BibleAppEnvironment {
    val platform = getPlatform(platformContext)
    val assetManager = AssetManagerImpl(platform = platform)
    val bible = Bible(assetManager = assetManager).apply {
        bibleResourcesReader = ComposeBibleResourcesReader()
    }
    return BibleAppEnvironment(
        platform = platform,
        assetManager = assetManager,
        bible = bible
    )
}

private val LocalBibleAppEnvironment = staticCompositionLocalOf {
    createBibleAppEnvironment()
}

@Composable
fun ProvideBibleAppEnvironment(
    platformContext: Any? = null,
    content: @Composable () -> Unit
) {
    val environment = remember(platformContext) {
        createBibleAppEnvironment(platformContext)
    }
    CompositionLocalProvider(LocalBibleAppEnvironment provides environment) {
        content()
    }
}

@Composable
fun currentBibleEnvironment(): BibleAppEnvironment = LocalBibleAppEnvironment.current
