package org.gnit.bible.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getcwd

@OptIn(ExperimentalForeignApi::class)
actual fun currentDir(): Path {
    return getcwd(null, 0)?.toKString()?.toPath() ?: ".".toPath()
}
