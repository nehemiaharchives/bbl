package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import okio.Path
import okio.Path.Companion.toPath
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalForeignApi::class)
fun homeDir(): Path {
    // Preferred: HOME env var
    val homeFromEnv = getenv("HOME")?.toKString()
    if (!homeFromEnv.isNullOrBlank()) return homeFromEnv.toPath()

    error("Could not determine home directory (env var HOME not set)")
}

class PosixPlatform : Platform() {
    override val name: String = "Posix"

    @OptIn(ExperimentalNativeApi::class)
    override val releaseTarget: ReleaseTarget = requireNotNull(
        detectReleaseTarget(
            osName = NativePlatform.osFamily.name,
            architectureName = NativePlatform.cpuArchitecture.name,
        )
    )

    @OptIn(ExperimentalForeignApi::class)
    override val platformBblDirPath: String by lazy {
        val home = getenv("HOME")?.toKString() ?: error("HOME environment variable not defined")
        "$home/$bblDir"
    }

    override val platformSettings: Settings
        get() = platformConfigSettings

    override val platformConfigSettings: Settings by lazy {
        val home = homeDir()
        val configPath = home / "$bblDir/$CONFIG_FILE_NAME"
        JsonFileSettings(fileSystem = fileSystem, path = configPath)
    }
}

actual fun getPlatform(platformContext: Any?): Platform = PosixPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Curl)
