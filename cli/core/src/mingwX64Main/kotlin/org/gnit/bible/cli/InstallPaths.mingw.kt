package org.gnit.bible.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import platform.posix.getenv

actual fun markExecutable(path: Path) {
    // Windows executability is determined by the .exe extension.
}

@OptIn(ExperimentalForeignApi::class)
actual fun environmentVariable(name: String): String? {
    return getenv(name)?.toKString()?.takeIf { it.isNotBlank() }
}
