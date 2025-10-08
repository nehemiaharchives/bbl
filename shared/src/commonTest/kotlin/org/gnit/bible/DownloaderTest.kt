package org.gnit.bible

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test

class DownloaderTest {

    @Test
    fun testDownload() {
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"

        download(baseUrl, fileName)
    }

    private fun download(
        baseUrl: String,
        fileName: String
    ) {
        val url = "$baseUrl$fileName"
        val httpClient = createPlatformHttpClient()
        val platform = getPlatform()
        val fileSystem = FileSystem.SYSTEM
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
            println("finished writing $destinationPath")
        } finally {
            httpClient.close()
        }
    }
}
