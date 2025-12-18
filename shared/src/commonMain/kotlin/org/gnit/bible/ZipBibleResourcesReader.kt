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
            val targetName =
                zip.entries.firstOrNull { it.name.endsWith("$translation.$book.$chapter.txt") }?.name
                    ?: error("No entry ending with $translation.$book.$chapter.txt found")
            zip.readTextEntry(targetName) { text, _ -> sb.append(text) }
        }
        return sb.toString()
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
