package org.gnit.bible.cli

import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getcwd

actual fun currentDir(): Path = memScoped {
    val bufferSize = 4096u.toULong()  // typical max path length
    val buffer: CPointer<ByteVar> = allocArray(bufferSize.toInt())
    val cwd = getcwd(buffer, bufferSize)
    val pathStr = cwd?.toKString() ?: "."
    pathStr.toPath()
}
