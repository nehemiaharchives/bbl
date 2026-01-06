package org.gnit.bible

import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

class ZipBibleResourcesReader(
    val platform: Platform,
    private val fileSystem: FileSystem = platform.fileSystem
) : BibleResourcesReader {

    val logger = KotlinLogging.logger {}

    override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        throw UnsupportedOperationException()
    }

    override fun readByPath(path: String): String {
        throw UnsupportedOperationException()
    }

    override fun getChapterText(translation: String, book: Int, chapter: Int): String {
        val sb = StringBuilder()
        withZipFile(translation) { zip ->
            val expectedFileName = "$translation.$book.$chapter.txt"
            val targetName = zip.entries
                .firstOrNull { it.name.substringAfterLast('/') == expectedFileName }
                ?.name
                ?: error("No entry ending with $expectedFileName found")

            zip.readTextEntry(targetName) { text, _ -> sb.append(text) }
        }
        return sb.toString()
    }

    override fun listIndexFiles(translation: String): List<String> {
        return withZipFile(translation) { zip ->
            zip.entries
                .map { it.name }
                .filter { it.startsWith("index/") && !it.endsWith("/") }
                .map { it.removePrefix("index/") }
                .toList()
        }
    }

    override fun readIndexFile(translation: String, name: String): ByteArray {
        require(name.isNotBlank()) { "Index file name is blank" }
        require(!name.contains('/')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains('\\')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains("..")) { "Index file name must not contain '..', got: $name" }

        val target = "index/$name"
        return withZipFile(translation) { zip ->
            val entry = zip.entries.firstOrNull { it.name == target }
                ?: error("Index file not found in zip: $target")
            val chunks = ArrayList<ByteArray>(4)
            zip.readEntry(entry) { _, content, count, last ->
                if (count.toInt() > 0) {
                    chunks.add(content.copyOfRange(0, count.toInt()))
                }
                // `last` is intentionally unused; we just buffer all chunks and merge at the end.
            }
            val total = chunks.sumOf { it.size }
            val merged = ByteArray(total)
            var offset = 0
            chunks.forEach { bytes ->
                bytes.copyInto(merged, offset)
                offset += bytes.size
            }
            merged
        }
    }

    fun getTranslationFromManifest(translationCode: String): Translation {
        val manifestJson = StringBuilder()
        withZipFile(translationCode) { zip ->
            val manifest = "$translationCode$MANIFEST_JSON_POSTFIX"
            val targetName = zip.entries.firstOrNull { it.name.endsWith(manifest) }?.name
                ?: error("$manifest not found in $zip")
            zip.readTextEntry(targetName) { text, _ -> manifestJson.append(text) }
        }
        return Translation.fromJson(manifestJson.toString())
    }

    private inline fun <T> withZipFile(
        translationCode: String,
        crossinline block: suspend (ZipFile) -> T
    ): T {
        val zipPath = platform.packDir.toPath() / "$translationCode.zip"
        require(fileSystem.exists(zipPath)) { "ZipBibleResourcesReader Zip file not found at $zipPath" }
        val file = com.oldguy.common.io.File(zipPath.toString())

        return runBlocking {
            var zipFile: ZipFile? = null

            var fileOpenError: Throwable? = null

            runCatching { zipFile = ZipFile(file) }
                .onSuccess { logger.debug { "ZipBibleResourcesReader successfully found and opened $zipPath" } }
                .onFailure { fileOpenError = it }

            if (zipFile != null) {
                try {
                    zipFile.open()
                    block(zipFile)
                } finally {
                    runCatching { zipFile.close() }.getOrNull()
                }
            } else {
                error("ZipBibleResourcesReader failed to open $zipPath with error: ${fileOpenError?.message}")
            }
        }
    }
}
