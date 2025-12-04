package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import okio.Path
import okio.Path.Companion.toPath

@OptIn(ExperimentalForeignApi::class)
fun homeDir(): Path {
    // Preferred: HOME env var
    val homeFromEnv = getenv("HOME")?.toKString()
    if (!homeFromEnv.isNullOrBlank()) return homeFromEnv.toPath()

    error("Could not determine home directory (env var HOME not set)")
}

class PosixPlatform : Platform() {
    override val name: String = "Posix"

    @OptIn(ExperimentalForeignApi::class)
    override val platformPackDir: String by lazy {
        val home = getenv("HOME")?.toKString() ?: error("HOME environment variable not defined")
        "$home/$bblDir/$packBaseDir"
    }
    override val settings: Settings by lazy {
        val home = homeDir()
        val settingsPath = home / "$bblDir/settings.properties"
        PosixSettings(fileSystem = okio.FileSystem.SYSTEM, path = settingsPath)
    }
}

actual fun getPlatform(platformContext: Any?): Platform = PosixPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Curl)
