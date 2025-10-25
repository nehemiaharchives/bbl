package org.gnit.bible

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.nio.file.FileSystems
import java.util.prefs.Preferences

class JVMPlatform : Platform() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override val platformPackDir: String by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val s = FileSystems.getDefault().separator ?: error("file.separator not defined")
        "$home${s}$bblDir${s}$packBaseDir"
    }
    override val settings: Settings by lazy {
        PreferencesSettings(Preferences.userRoot())
    }
}

actual fun getPlatform(platformContext: Any?): Platform = JVMPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp)
