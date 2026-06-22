package org.gnit.bible

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

enum class ReleasePlatform(val assetName: String) {
    LINUX("linux"),
    MACOS("macos"),
    WINDOWS("windows"),
}

enum class ReleaseArchitecture(val assetName: String) {
    X64("x64"),
    ARM64("arm64"),
}

data class ReleaseTarget(
    val platform: ReleasePlatform,
    val architecture: ReleaseArchitecture,
) {
    fun assetName(localBinaryName: String): String {
        val extension = if (platform == ReleasePlatform.WINDOWS) ".exe" else ""
        val baseName = localBinaryName.removeSuffix(".exe")
        return "$baseName-${platform.assetName}-${architecture.assetName}$extension"
    }
}

fun detectReleaseTarget(osName: String, architectureName: String): ReleaseTarget? {
    val platform = when (osName.lowercase().replace(Regex("[^a-z0-9]"), "")) {
        "linux" -> ReleasePlatform.LINUX
        "mac", "macos", "macosx", "darwin" -> ReleasePlatform.MACOS
        "windows", "windows10", "windows11", "mingw" -> ReleasePlatform.WINDOWS
        else -> return null
    }
    val architecture = when (architectureName.lowercase().replace(Regex("[^a-z0-9_]"), "")) {
        "x64", "x8664", "x86_64", "amd64" -> ReleaseArchitecture.X64
        "arm64", "aarch64" -> ReleaseArchitecture.ARM64
        else -> return null
    }
    return ReleaseTarget(platform, architecture)
}

abstract class Platform {
    abstract val name: String

    open val releaseTarget: ReleaseTarget? = null

    fun releaseAssetName(localBinaryName: String): String {
        val target = requireNotNull(releaseTarget) {
            "GitHub release binaries are not available for platform $name"
        }
        return target.assetName(localBinaryName)
    }

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
