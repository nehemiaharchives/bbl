package org.gnit.bible

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

class NativePlatform: Platform() {
    override val name: String = "Native"

    @OptIn(ExperimentalForeignApi::class)
    override val packDir: String by lazy {
        val home = getenv("HOME")?.toKString() ?: error("HOME environment variable not defined")
        "$home/$bblDir/$packBaseDir"
    }

}

actual fun getPlatform(platformContext: Any?): Platform = NativePlatform()
