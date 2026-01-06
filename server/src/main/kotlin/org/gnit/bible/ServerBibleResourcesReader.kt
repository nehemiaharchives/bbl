package org.gnit.bible

import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * JVM-only reader for server: loads zip packs from classpath under /files/bblpacks/<code>.zip.
 */
class ServerBibleResourcesReader : BibleResourcesReader {

    private fun openZip(translation: String): ZipInputStream {
        val path = "/files/bblpacks/$translation.zip"
        val stream = javaClass.getResourceAsStream(path)
            ?: error("ServerBibleResourcesReader could not find $path on classpath")
        return ZipInputStream(stream)
    }

    override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        return "$translation.$book.$chapter.txt"
    }

    override fun readByPath(path: String): String =
        readEntry(path) { it.readBytes().decodeToString() }

    override fun getChapterText(translation: String, book: Int, chapter: Int): String {
        val name = chapterFile(translation, book, chapter)
        return readEntry(name) { it.readBytes().decodeToString() }
    }

    override fun listIndexFiles(translation: String): List<String> {
        val manifestName = "index/$translation${SearchEngine.INDEX_MANIFEST_FILENAME_POSTFIX}"
        val manifest = readEntry(manifestName) { it.readBytes().decodeToString() }
        return manifest.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    override fun readIndexFile(translation: String, name: String): ByteArray {
        require(name.isNotBlank()) { "Index file name is blank" }
        require(!name.contains('/')) { "Index file name must be a flat filename" }
        require(!name.contains("..")) { "Index file name must not contain '..'" }
        val entryName = "index/$name"
        return readEntry(entryName) { it.readBytes() }
    }

    private fun <T> readEntry(name: String, block: (InputStream) -> T): T {
        openZip(name.substringBefore('.')).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry?.name == name || entry?.name?.endsWith("/$name") == true) {
                    return block(zis)
                }
            }
        }
        error("Entry $name not found in zip")
    }
}

