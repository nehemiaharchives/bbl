package org.gnit.bible.cli

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getcwd

actual fun currentDir(): Path = memScoped {
    val buffer = ByteArray(4096)
    val result = getcwd(buffer.refTo(0), buffer.size.toULong())
    val dir = result?.toKString() ?: "."
    dir.toPath()
}
