package org.gnit.bible.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import platform.posix.getenv
import platform.posix.system

@OptIn(ExperimentalForeignApi::class)
actual fun markExecutable(path: Path) {
    val escaped = path.toString().replace("'", "'\"'\"'")
    system("chmod 755 '$escaped'")
}

@OptIn(ExperimentalForeignApi::class)
actual fun environmentVariable(name: String): String? {
    return getenv(name)?.toKString()?.takeIf { it.isNotBlank() }
}
