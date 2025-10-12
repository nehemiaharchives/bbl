package org.gnit.bible

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

class PosixPlatform : Platform() {
    override val name: String = "Posix"

    @OptIn(ExperimentalForeignApi::class)
    override val platformPackDir: String by lazy {
        val home = getenv("HOME")?.toKString() ?: error("HOME environment variable not defined")
        "$home/$bblDir/$packBaseDir"
    }
}

actual fun getPlatform(platformContext: Any?): Platform = PosixPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Curl)
