package org.gnit.bible

import com.oldguy.common.io.ZipFile
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class ZipBibleTextReader(
    val platform: Platform,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
): BibleTextReader {

    override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        throw UnsupportedOperationException()
    }

    override fun readByPath(path: String): String {
        throw UnsupportedOperationException()
    }

    override fun getChapterText(translation: String, book: Int, chapter: Int): String {
        val sb = StringBuilder()
        withZipFile(translation) { zip ->
            val targetName = zip.entries.firstOrNull { it.name.endsWith("$translation.$book.$chapter.txt") }?.name
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

    private inline fun <T> withZipFile(translationCode: String, crossinline block: suspend (ZipFile) -> T): T {
        val zipPath = platform.packDir.toPath() / "$translationCode.zip"
        require(fileSystem.exists(zipPath)) { "ZipBibleTextReader Zip file not found at $zipPath" }
        val file = com.oldguy.common.io.File(zipPath.toString())

        return runBlocking {
            val zipFile = ZipFile(file)
            try {
                zipFile.open()
                block(zipFile)
            } finally {
                runCatching { zipFile.close() }.getOrNull()
            }
        }
    }
}
