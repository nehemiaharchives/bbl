package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

abstract class Platform {
    abstract val name: String

    protected val bblDir: String
        get() = ".bbl"

    protected val packBaseDir: String
        get() = "packs"

    /**
     * resolves to $HOME/.bbl on posix (linux/macos)
     * platform specific data dir for iOS and Android
     */
    abstract val platformBblDirPath: String

    var overridePlatformBblDirPath: String? = null

    /**
     * Allows tests to replace the default pack directory.
     * When set, packDir uses this value directly instead of deriving from bblDirPath.
     */
    var overridePlatformPackDir: String? = null

    /**
     * Allows tests to replace the default file system (which is usually FileSystem.SYSTEM).
     */
    var overrideFileSystem: FileSystem? = null

    val fileSystem: FileSystem
        get() = overrideFileSystem ?: FileSystem.SYSTEM

    val bblDirPath: String
        get() = overridePlatformBblDirPath ?: platformBblDirPath

    val packDir: String
        get() = overridePlatformPackDir ?: (bblDirPath.toPath() / packBaseDir).toString()

    var overrideSettings: Settings? = null

    open val settings: Settings
        get() = overrideSettings ?: platformSettings

    protected abstract val platformSettings: Settings

    var overrideConfigSettings: Settings? = null

    open val configSettings: Settings
        get() = overrideConfigSettings ?: overrideSettings ?: platformConfigSettings

    protected open val platformConfigSettings: Settings
        get() = platformSettings

    fun isIos() = name.startsWith("iOS")
    fun isAndroid() = name.startsWith("Android")
}

expect fun getPlatform(platformContext: Any? = null): Platform

expect fun createPlatformHttpClient(): HttpClient
