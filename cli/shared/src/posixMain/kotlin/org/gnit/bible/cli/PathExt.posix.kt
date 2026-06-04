package org.gnit.bible.cli

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getcwd

actual fun currentDir(): Path = memScoped {
    val bufferSize = 4096u.toULong()
    val buffer: CPointer<ByteVar> = allocArray(bufferSize.toInt())
    val cwd = getcwd(buffer, bufferSize)
    val pathStr = cwd?.toKString() ?: "."
    pathStr.toPath()
}
