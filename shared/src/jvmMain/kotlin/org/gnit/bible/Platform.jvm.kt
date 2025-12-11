package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okio.Path.Companion.toPath
import java.nio.file.FileSystems

class JVMPlatform : Platform() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override val platformPackDir: String by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val s = FileSystems.getDefault().separator ?: error("file.separator not defined")
        "$home${s}$bblDir${s}$packBaseDir"
    }
    override val settings: Settings by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val settingsPath = "$home${FileSystems.getDefault().separator}$bblDir${FileSystems.getDefault().separator}settings.properties".toPath()
        JvmFileSettings(fileSystem = fileSystem, path = settingsPath)
    }
}

actual fun getPlatform(platformContext: Any?): Platform = JVMPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp)
