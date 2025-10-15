package org.gnit.bible

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

interface AssetManager {
    fun download(baseUrl: String, fileName: String)
    fun downloadedTranslations(): List<String>
}

class AssetManagerImpl(
    val httpClient: HttpClient = createPlatformHttpClient(),
    val platform: Platform = getPlatform(),
    val fileSystem: FileSystem = FileSystem.SYSTEM) : AssetManager {

    override fun download(
        baseUrl: String,
        fileName: String
    ) {
        val url = "$baseUrl$fileName"
        val packDir = platform.packDir
        val destinationPath = packDir.toPath() / fileName
        fileSystem.createDirectories(destinationPath.parent!!)
        try {
            runBlocking {
                val httpResponse = httpClient.get(url)
                val byteChannel: ByteReadChannel = httpResponse.body()
                fileSystem.write(destinationPath) {
                    while (!byteChannel.isClosedForRead) {
                        val packet = byteChannel.readRemaining()
                        val bytes = packet.readByteArray()
                        write(bytes)
                    }
                }
            }
        } finally {
            httpClient.close()
        }
    }

    override fun downloadedTranslations(): List<String> {
        val packDir = platform.packDir.toPath()
        if (!fileSystem.exists(packDir)) return emptyList()
        return fileSystem.list(packDir).map { it.name.removeSuffix(".zip")  }
    }
}