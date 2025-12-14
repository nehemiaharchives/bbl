package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

enum class InstallationState { EMBEDDED, DOWNLOADED, DOWNLOADABLE }

data class TranslationEntry(
    val translation: Translation,
    val source: InstallationState
)

interface AssetManager {

    val platform: Platform
    val fileSystem: FileSystem

    /**
     * @param baseUrl url of the translation list json file e.g. "https://bbl.pack.server.local/files/bblpacklist.json"
     */
    suspend fun downloadableTranslationList(listUrl: String): List<Translation>

    /**
     * @param baseUrl url of the base dir of the zip file e.g. "https://bbl.pack.server.local/files/bblpack"
     * @param fileName "${translationCode}.zip" e.g. "kttv.zip"
     */
    suspend fun download(baseUrl: String, fileName: String)
    fun downloadedTranslationCodes(): List<String>

    fun downloadedTranslations(): List<Translation>
    fun delete(translationCode: String)
}

class AssetManagerImpl(
    val httpClient: HttpClient = createPlatformHttpClient(),
    override val platform: Platform = getPlatform(),
    override val fileSystem: FileSystem = platform.fileSystem
) : AssetManager {

    private val logger = KotlinLogging.logger {}

    override suspend fun downloadableTranslationList(listUrl: String): List<Translation> {
        val cacheFileName = "downloadable_translations_cache.json"
        val cachePath = platform.cacheDir.toPath() / cacheFileName

        fun readCachedTranslations(): List<Translation>? = runCatching {
            if (!fileSystem.exists(cachePath)) return null
            val json = fileSystem.source(cachePath).buffer().use { it.readUtf8() }
            Json.decodeFromString<List<Translation>>(json)
        }.onFailure { error ->
            logger.error { "AssetManagerImpl failed to read cached translation list: ${error.message}" }
        }.getOrNull()

        fun writeCachedTranslations(translations: List<Translation>) {
            runCatching {
                fileSystem.createDirectories(cachePath.parent!!)
                val tmpPath = cachePath.parent!! / "$cacheFileName.part"
                fileSystem.sink(tmpPath).buffer().use { it.writeUtf8(Json.encodeToString(translations)) }
                runCatching { fileSystem.delete(cachePath) }
                fileSystem.atomicMove(tmpPath, cachePath)
            }.onFailure { error ->
                logger.error { "AssetManagerImpl failed to write cached translation list: ${error.message}" }
            }
        }

        return runCatching {
            val httpResponse = httpClient.get(listUrl) {
                timeout { requestTimeoutMillis = 15_000 }
            }
            if (!httpResponse.status.isSuccess()) error("HTTP ${httpResponse.status}")
            val translations: List<Translation> = Json.decodeFromString(httpResponse.bodyAsText())
            writeCachedTranslations(translations)
            logger.debug { "AssetManagerImpl fetched downloadable translation list (${translations.size})" }
            translations
        }.getOrElse { error ->
            logger.error { "AssetManagerImpl failed to fetch downloadable translation list: ${error.message}" }
            val cached = readCachedTranslations()
            if (cached != null) logger.debug { "AssetManagerImpl served cached translation list (${cached.size})" }
            cached ?: emptyList()
        }
    }

    override suspend fun download(
        baseUrl: String,
        fileName: String
    ) {
        val url = "${baseUrl.trimEnd('/')}/$fileName"
        val packDir = platform.packDir
        val destinationPath = packDir.toPath() / fileName
        val tempPath = destinationPath.parent!! / "${fileName}.part"
        fileSystem.createDirectories(destinationPath.parent!!)

        val existingSize = runCatching { fileSystem.metadata(tempPath).size ?: 0L }.getOrDefault(0L)

        val httpResponse = httpClient.get(url) {
            if (existingSize > 0) {
                header("Range", "bytes=$existingSize-")
            }
            timeout { requestTimeoutMillis = 30_000 }
        }

        val append = existingSize > 0 && httpResponse.status.value == 206
        if (existingSize > 0 && !append) {
            // server did not honor range; restart fresh
            runCatching { fileSystem.delete(tempPath) }
        }

        if (!httpResponse.status.isSuccess()) error("HTTP ${httpResponse.status}")

        val byteChannel: ByteReadChannel = httpResponse.body()
        runCatching {
            if (!append) runCatching { fileSystem.delete(tempPath) }
            val sink = if (append) fileSystem.appendingSink(tempPath) else fileSystem.sink(tempPath)
            sink.buffer().use { buffered ->
                while (!byteChannel.isClosedForRead) {
                    val packet = byteChannel.readRemaining()
                    val bytes = packet.readByteArray()
                    buffered.write(bytes)
                }
                buffered.flush()
            }
            // basic integrity: ensure file is not empty
            val size = fileSystem.metadata(tempPath).size ?: 0
            if (size == 0L) error("Empty download for $fileName")
            // atomic move into place
            runCatching { fileSystem.delete(destinationPath) }
            fileSystem.atomicMove(tempPath, destinationPath)
            logger.debug { "AssetManagerImpl downloaded $fileName to destination: $destinationPath (${size} bytes)" }
        }.onFailure {
            logger.error { "AssetManagerImpl failed to download $fileName: ${it.message}" }
            runCatching { fileSystem.delete(tempPath) }
            throw it
        }
    }

    override fun downloadedTranslationCodes(): List<String> {
        val packDir = platform.packDir.toPath()
        if (!fileSystem.exists(packDir)) return emptyList()
        return fileSystem.list(packDir).map { it.name.removeSuffix(".zip") }
    }

    override fun downloadedTranslations(): List<Translation> {
        // Use ZipBibleTextReader to read the manifest from each downloaded zip and return the parsed Translation.
        val codes = downloadedTranslationCodes()
        if (codes.isEmpty()) return emptyList()
        val reader = ZipBibleTextReader(platform, fileSystem)
        return codes.mapNotNull { code ->
            runCatching { reader.getTranslationFromManifest(code) }
                .onFailure { logger.error { "AssetManagerImpl failed to read manifest for $code: ${it.message}" } }
                .getOrNull()
        }
    }

    override fun delete(translationCode: String) {
        val packDir = platform.packDir.toPath()
        val target = packDir / "$translationCode.zip"
        if (fileSystem.exists(target)) {
            logger.debug { "AssetManagerImpl found existing $target, deleting" }
            fileSystem.delete(target)
        }else{
            logger.debug { "AssetManagerImpl was asked to delete but did not find $target" }
        }
    }
}
