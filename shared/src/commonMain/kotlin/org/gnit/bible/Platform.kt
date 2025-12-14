package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.SYSTEM

abstract class Platform {
    abstract val name: String

    protected val bblDir: String
        get() = ".bbl"

    protected val packBaseDir: String
        get() = "packs"

    protected val cacheBaseDir: String
        get() = "cache"

    /**
     * resolves to $HOME/.bbl/packs on posix (linux/macos)
     * platform specific data dir for iOS and Android
     */
    abstract val platformPackDir: String

    var overridePlatformPackDir: String? = null

    /**
     * resolves to $HOME/.bbl/cache on posix (linux/macos)
     * platform specific data dir for iOS and Android
     */
    abstract val platformCacheDir: String

    var overridePlatformCacheDir: String? = null

    /**
     * Allows tests to replace the default file system (which is usually FileSystem.SYSTEM).
     */
    var overrideFileSystem: FileSystem? = null

    val fileSystem: FileSystem
        get() = overrideFileSystem ?: FileSystem.SYSTEM

    val packDir: String
        get() = overridePlatformPackDir ?: platformPackDir

    val cacheDir: String
        get() = overridePlatformCacheDir ?: platformCacheDir

    abstract val settings: Settings

    fun isIos() = name.startsWith("iOS")
    fun isAndroid() = name.startsWith("Android")
}

expect fun getPlatform(platformContext: Any? = null): Platform

expect fun createPlatformHttpClient(): HttpClient
