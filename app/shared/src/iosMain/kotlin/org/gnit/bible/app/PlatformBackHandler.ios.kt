package org.gnit.bible.app

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS provides no hardware back; no-op placeholder.
}
