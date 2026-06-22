package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import java.nio.file.FileSystems

class JVMPlatform : Platform() {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val releaseTarget: ReleaseTarget? = detectReleaseTarget(
        osName = System.getProperty("os.name").orEmpty(),
        architectureName = System.getProperty("os.arch").orEmpty(),
    )

    override val platformBblDirPath: String by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val s = FileSystems.getDefault().separator ?: error("file.separator not defined")
        "$home${s}$bblDir"
    }

    override val platformSettings: Settings
        get() = platformConfigSettings

    override val platformConfigSettings: Settings by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val configPath = "$home${FileSystems.getDefault().separator}$bblDir${FileSystems.getDefault().separator}$CONFIG_FILE_NAME".toPath()
        JsonFileSettings(fileSystem = fileSystem, path = configPath)
    }
}

private val cachedPlatform: JVMPlatform by lazy { JVMPlatform() }

actual fun getPlatform(platformContext: Any?): Platform = cachedPlatform

actual fun createPlatformHttpClient(): HttpClient = HttpClient()
