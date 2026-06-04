package org.gnit.bible

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

class IOSPlatform : Platform() {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val platformPackDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        val base = (paths.firstOrNull() as? String) ?: NSHomeDirectory()
        "$base/$bblDir/$packBaseDir"
    }

    override val platformSettings: Settings by lazy {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults())
    }
}

actual fun getPlatform(platformContext: Any?): Platform = IOSPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin)
