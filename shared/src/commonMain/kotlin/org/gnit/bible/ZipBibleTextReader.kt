package org.gnit.bible

import com.oldguy.common.io.File
import com.oldguy.common.io.ZipFile
import kotlinx.coroutines.runBlocking

class ZipBibleTextReader(val platform: Platform): BibleTextReader {

    override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        throw UnsupportedOperationException()
    }

    override fun readByPath(path: String): String {
        throw UnsupportedOperationException()
    }

    override fun getChapterText(translation: String, book: Int, chapter: Int): String {

        val path = "${platform.packDir}/$translation.zip"

        val file = File(path)
        val sb = StringBuilder()

        val zipFile = ZipFile(file)

        runBlocking{
            zipFile.use { zip ->
                val targetName = zip.entries.firstOrNull { it.name.endsWith("$translation.$book.$chapter.txt") }?.name
                    ?: error("No entry ending with $translation.$book.$chapter.txt found")
                zip.readTextEntry(targetName) { text, _ -> sb.append(text) }
            }
        }
        return sb.toString()
    }

    fun getTranslationFromManifest(translationCode: String): Translation {
        val path = "${platform.packDir}/$translationCode.zip"

        val file = File(path)
        val zipFile = ZipFile(file)

        val manifestJson = StringBuilder()

        runBlocking{
            zipFile.use { zip ->
                val manifest = "$translationCode$MANIFEST_JSON_POSTFIX"
                val targetName = zip.entries.firstOrNull { it.name.endsWith(manifest) }?.name
                    ?: error("$manifest not found in $zip")
                zip.readTextEntry(targetName) { text, _ -> manifestJson.append(text) }
            }
        }
        return Translation.fromJson(manifestJson.toString())
    }
}
