package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLogging
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Inflater
import okio.InflaterSource
import okio.buffer
import okio.use

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
        return withZipFile(translation) { zip ->
            val expectedFileName = "$translation.$book.$chapter.txt"
            val targetName = zip.entryNames()
                .firstOrNull { it.substringAfterLast('/') == expectedFileName }
                ?: error("No entry ending with $expectedFileName found")
            zip.readEntryBytes(targetName).decodeToString()
        }
    }

    override fun listIndexFiles(translation: String): List<String> {
        return withZipFile(translation) { zip ->
            zip.entryNames()
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
            val entry = zip.entryNames().firstOrNull { it == target }
                ?: error("Index file not found in zip: $target")
            zip.readEntryBytes(entry)
        }
    }

    fun getTranslationFromManifest(translationCode: String): Translation {
        val json = withZipFile(translationCode) { zip ->
            val manifest = "$translationCode$MANIFEST_JSON_POSTFIX"
            val targetName = zip.entryNames().firstOrNull { it.endsWith(manifest) }
                ?: error("$manifest not found in $zip")
            zip.readEntryBytes(targetName).decodeToString()
        }
        return Translation.fromJson(json)
    }

    private inline fun <T> withZipFile(
        translationCode: String,
        block: (SimpleZip) -> T
    ): T {
        val zipPath = platform.packDir.toPath() / "$translationCode.zip"
        require(fileSystem.exists(zipPath)) { "ZipBibleResourcesReader Zip file not found at $zipPath" }
        val zipBytes = fileSystem.read(zipPath) { readByteArray() }
        val zipFileSystem = SimpleZip(zipBytes)
        logger.debug { "ZipBibleResourcesReader successfully found and opened $zipPath" }
        return block(zipFileSystem)
    }

    private class SimpleZip(private val bytes: ByteArray) {
        private val entries: Map<String, Entry> = parseEntries()

        fun entryNames(): List<String> = entries.keys.toList()

        fun readEntryBytes(name: String): ByteArray {
            val entry = entries[name] ?: error("Zip entry not found: $name")
            val localHeaderOffset = entry.localHeaderOffset.toInt()
            require(readIntLe(localHeaderOffset) == LOCAL_FILE_HEADER_SIGNATURE) {
                "Bad zip local header for $name"
            }
            val nameLength = readShortLe(localHeaderOffset + 26)
            val extraLength = readShortLe(localHeaderOffset + 28)
            val dataOffset = localHeaderOffset + 30 + nameLength + extraLength
            val compressed = bytes.copyOfRange(dataOffset, dataOffset + entry.compressedSize.toInt())

            return when (entry.compressionMethod) {
                COMPRESSION_METHOD_STORED -> compressed
                COMPRESSION_METHOD_DEFLATED -> inflate(compressed, entry.size)
                else -> error("Unsupported zip compression method ${entry.compressionMethod} for $name")
            }
        }

        private fun parseEntries(): Map<String, Entry> {
            val eocdOffset = findEndOfCentralDirectory()
            val entryCount = readShortLe(eocdOffset + 10)
            val centralDirectoryOffset = readUIntLe(eocdOffset + 16).toInt()
            var offset = centralDirectoryOffset
            val parsed = linkedMapOf<String, Entry>()

            repeat(entryCount) {
                require(readIntLe(offset) == CENTRAL_FILE_HEADER_SIGNATURE) {
                    "Bad zip central directory header at $offset"
                }
                val flags = readShortLe(offset + 8)
                require(flags and BIT_FLAG_ENCRYPTED == 0) { "Encrypted zip entries are not supported" }
                val compressionMethod = readShortLe(offset + 10)
                val compressedSize = readUIntLe(offset + 20)
                val size = readUIntLe(offset + 24)
                val nameLength = readShortLe(offset + 28)
                val extraLength = readShortLe(offset + 30)
                val commentLength = readShortLe(offset + 32)
                val localHeaderOffset = readUIntLe(offset + 42)
                val nameStart = offset + 46
                val name = bytes.copyOfRange(nameStart, nameStart + nameLength).decodeToString()

                if (!name.endsWith("/")) {
                    parsed[name] = Entry(
                        name = name,
                        compressionMethod = compressionMethod,
                        compressedSize = compressedSize,
                        size = size,
                        localHeaderOffset = localHeaderOffset,
                    )
                }

                offset = nameStart + nameLength + extraLength + commentLength
            }

            return parsed
        }

        private fun findEndOfCentralDirectory(): Int {
            val minimumOffset = 0.coerceAtLeast(bytes.size - MAX_EOCD_SEARCH)
            for (offset in bytes.size - EOCD_MIN_SIZE downTo minimumOffset) {
                if (readIntLe(offset) == END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
                    return offset
                }
            }
            error("End of central directory not found")
        }

        private fun inflate(compressed: ByteArray, expectedSize: Long): ByteArray {
            val source = Buffer().write(compressed)
            return InflaterSource(source, Inflater(true)).use { inflater ->
                inflater.buffer().readByteArray(expectedSize)
            }
        }

        private fun readShortLe(offset: Int): Int =
            (bytes[offset].toInt() and 0xff) or
                ((bytes[offset + 1].toInt() and 0xff) shl 8)

        private fun readIntLe(offset: Int): Int =
            (bytes[offset].toInt() and 0xff) or
                ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                ((bytes[offset + 2].toInt() and 0xff) shl 16) or
                ((bytes[offset + 3].toInt() and 0xff) shl 24)

        private fun readUIntLe(offset: Int): Long = readIntLe(offset).toLong() and 0xffffffffL

        private data class Entry(
            val name: String,
            val compressionMethod: Int,
            val compressedSize: Long,
            val size: Long,
            val localHeaderOffset: Long,
        )
    }

    private companion object {
        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
        const val CENTRAL_FILE_HEADER_SIGNATURE = 0x02014b50
        const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
        const val COMPRESSION_METHOD_STORED = 0
        const val COMPRESSION_METHOD_DEFLATED = 8
        const val BIT_FLAG_ENCRYPTED = 1
        const val EOCD_MIN_SIZE = 22
        const val MAX_EOCD_SEARCH = EOCD_MIN_SIZE + 65_536
    }
}
