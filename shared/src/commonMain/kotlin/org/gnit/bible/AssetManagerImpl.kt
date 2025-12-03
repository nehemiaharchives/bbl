package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

interface AssetManager {

    val platform: Platform
    fun downloadableTranslationList(listUrl: String): List<Translation>
    fun download(baseUrl: String, fileName: String)
    fun downloadedTranslationCodes(): List<String>
    fun delete(translationCode: String)
}

class AssetManagerImpl(
    val httpClient: HttpClient = createPlatformHttpClient(),
    override val platform: Platform = getPlatform(),
    val fileSystem: FileSystem = FileSystem.SYSTEM
) : AssetManager {

    private val logger = KotlinLogging.logger {}

    override fun downloadableTranslationList(listUrl: String): List<Translation> {
        return try {
            runBlocking {
                val httpResponse = httpClient.get(listUrl)
                val translations: List<Translation> = Json.decodeFromString(httpResponse.bodyAsText())
                logger.debug { "AssetManagerImpl successfully fetched downloadable translation list with ${translations.size} translations" }
                translations
            }
        } catch (e: Exception) {
            logger.error { "AssetManagerImpl failed to fetch downloadable translation list: ${e.message}" }
            emptyList()
        }
    }

    override fun download(
        baseUrl: String,
        fileName: String
    ) {
        val url = "$baseUrl$fileName"
        val packDir = platform.packDir
        val destinationPath = packDir.toPath() / fileName
        fileSystem.createDirectories(destinationPath.parent!!)
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
    }

    override fun downloadedTranslationCodes(): List<String> {
        val packDir = platform.packDir.toPath()
        if (!fileSystem.exists(packDir)) return emptyList()
        return fileSystem.list(packDir).map { it.name.removeSuffix(".zip") }
    }

    override fun delete(translationCode: String) {
        val packDir = platform.packDir.toPath()
        val target = packDir / "$translationCode.zip"
        if (fileSystem.exists(target)) {
            fileSystem.delete(target)
        }
    }
}
