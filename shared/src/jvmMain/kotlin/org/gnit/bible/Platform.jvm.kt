package org.gnit.bible

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.nio.file.FileSystems

class JVMPlatform : Platform() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override val packDir: String by lazy {
        val home = System.getProperty("user.home") ?: error("user.home not defined")
        val s = FileSystems.getDefault().separator ?: error("file.separator not defined")
        "$home${s}$bblDir${s}$packBaseDir"
    }
}

actual fun getPlatform(platformContext: Any?): Platform = JVMPlatform()

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
