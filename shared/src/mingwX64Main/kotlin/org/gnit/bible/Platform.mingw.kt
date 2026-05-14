package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
private fun env(name: String): String? = getenv(name)?.toKString()?.takeIf { it.isNotBlank() }

private fun windowsHomeDir(): Path {
    env("USERPROFILE")?.let { return it.toPath() }

    val drive = env("HOMEDRIVE")
    val path = env("HOMEPATH")
    if (!drive.isNullOrBlank() && !path.isNullOrBlank()) {
        return "$drive$path".toPath()
    }

    error("Could not determine home directory (USERPROFILE/HOMEDRIVE/HOMEPATH not set)")
}

class MingwPlatform : Platform() {
    override val name: String = "Windows"

    override val platformPackDir: String by lazy {
        (windowsHomeDir() / bblDir / packBaseDir).toString()
    }

    override val settings: Settings by lazy {
        CommonFileSettings(fileSystem = fileSystem, path = windowsHomeDir() / bblDir / SETTINGS_FILE_NAME)
    }
}

actual fun getPlatform(platformContext: Any?): Platform = MingwPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
