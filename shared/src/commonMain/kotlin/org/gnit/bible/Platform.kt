package org.gnit.bible

import io.ktor.client.HttpClient

abstract class Platform {
    abstract val name: String

    protected val bblDir: String
        get() = ".bbl"

    protected val packBaseDir: String
        get() = "packs"

    /**
     * resolves to $HOME/.bbl/packs on posix (linux/macos)
     * platform specific data dir for iOS and Android
     */
    abstract val platformPackDir: String

    var overridePlatformPackDir: String? = null

    val packDir: String
        get() = overridePlatformPackDir ?: platformPackDir
}

expect fun getPlatform(platformContext: Any? = null): Platform

fun isIos() = getPlatform().name.startsWith("iOS")

expect fun createPlatformHttpClient(): HttpClient
