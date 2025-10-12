package org.gnit.bible

import io.ktor.client.HttpClient

abstract class Platform {
    abstract val name: String

    protected val bblDir: String
        get() = ".bbl"

    protected val packBaseDir: String
        get() = "packs"

    abstract val packDir: String
}

expect fun getPlatform(platformContext: Any? = null): Platform

fun isIos() = getPlatform().name.startsWith("iOS")

expect fun createPlatformHttpClient(): HttpClient
