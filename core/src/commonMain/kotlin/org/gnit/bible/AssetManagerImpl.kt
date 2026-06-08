package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

enum class InstallationState(val description: String) {
    EMBEDDED("Embedded"),
    DOWNLOADED("Installed"),
    DOWNLOADABLE("Available")
}

data class TranslationEntry(
    val translation: Translation,
    val source: InstallationState
)

interface AssetManager {

    val platform: Platform
    val fileSystem: FileSystem

    /**
     * @param baseUrl url of the base dir of the zip file e.g. "https://bbl.pack.server.local/files/bblpack"
     * @param fileName "${translationCode}.zip" e.g. "kttv.zip"
     */
    suspend fun download(baseUrl: String, fileName: String)
    suspend fun downloadTo(baseUrl: String, fileName: String, destinationDir: String)
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

    override suspend fun download(
        baseUrl: String,
        fileName: String
    ) {
        downloadTo(baseUrl, fileName, platform.packDir)
    }

    override suspend fun downloadTo(
        baseUrl: String,
        fileName: String,
        destinationDir: String
    ) {
        var lastFailure: Throwable? = null
        val candidateBaseUrls = buildList {
            add(baseUrl)
            val legacyBaseUrl = baseUrl.replace(
                BblVersion.BBL_REPOSITORY,
                BblVersion.BBL_REPOSITORY_LEGACY
            )
            if (legacyBaseUrl != baseUrl) add(legacyBaseUrl)
        }
        for (candidateBaseUrl in candidateBaseUrls) {
            val result = runCatching {
                val url = "${candidateBaseUrl.trimEnd('/')}/$fileName"
                val destinationPath = destinationDir.toPath() / fileName
                val tempPath = destinationPath.parent!! / "${fileName}.part"
                fileSystem.createDirectories(destinationPath.parent!!)

                if (candidateBaseUrl.startsWith("file://")) {
                    val sourcePath = candidateBaseUrl.removePrefix("file://").trimEnd('/').toPath() / fileName
                    runCatching { fileSystem.delete(tempPath) }
                    fileSystem.source(sourcePath).buffer().use { source ->
                        fileSystem.sink(tempPath).buffer().use { sink ->
                            sink.writeAll(source)
                        }
                    }
                    val size = fileSystem.metadata(tempPath).size ?: 0
                    if (size == 0L) error("Empty download for $fileName")
                    runCatching { fileSystem.delete(destinationPath) }
                    fileSystem.atomicMove(tempPath, destinationPath)
                    logger.debug { "AssetManagerImpl copied $fileName to destination: $destinationPath (${size} bytes)" }
                    return@runCatching
                }

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
            if (result.isSuccess) {
                if (candidateBaseUrl != baseUrl) {
                    logger.debug { "AssetManagerImpl downloaded $fileName using fallback base URL $candidateBaseUrl" }
                }
                return
            }
            lastFailure = result.exceptionOrNull()
        }
        throw lastFailure ?: error("Unable to download $fileName")
    }

    override fun downloadedTranslationCodes(): List<String> {
        val packDirPath = platform.packDir.toPath()
        return runCatching {
            if (!fileSystem.exists(packDirPath)) return emptyList()
            fileSystem.list(packDirPath)
                .mapNotNull { path ->
                    val name = path.name
                    if (!name.endsWith(".zip")) return@mapNotNull null
                    name.removeSuffix(".zip")
                }
                .sorted()
        }.getOrElse {
            logger.debug { "AssetManagerImpl failed to list downloadedTranslationCodes: ${it.message}" }
            emptyList()
        }
    }

    /**
     * Use [ZipBibleResourcesReader] to read the manifest from each downloaded zip and return the parsed Translation.
     */
    override fun downloadedTranslations(): List<Translation> {
        val codes = downloadedTranslationCodes()
        if (codes.isEmpty()) return emptyList()

        val reader = ZipBibleResourcesReader(platform = platform, fileSystem = fileSystem)

        return codes.mapNotNull { code ->
            runCatching { reader.getTranslationFromManifest(code) }
                .onFailure { error ->
                    logger.debug { "AssetManagerImpl failed to read manifest for installed pack '$code': ${error.message}" }
                }
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
