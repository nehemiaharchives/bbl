package org.gnit.bible

import okio.FileSystem
import okio.Path
import okio.SYSTEM

/**
 * Test-only [BibleResourcesReader] for the tiny fixture pack.
 *
 * This implementation is **Kotlin common**: it does not use JVM-only APIs (ClassLoader/java.nio).
 *
 * Because common code can't reliably "discover" the resource root at runtime, the test harness
 * must provide [resourcesRoot] (e.g. the directory that contains `bblpacks/`).
 */
class FixturePackResourcesReader(
    private val resourcesRoot: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : BibleResourcesReader {

    override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        return "$base/$translation/$translation.$book.$chapter.txt"
    }

    override fun readByPath(path: String): String {
        val fullPath = resourcesRoot / path
        return fileSystem.read(fullPath) { readUtf8() }
    }

    override fun listIndexFiles(translation: String): List<String> {
        val manifestPath = "$base/$translation/index/$translation${SearchEngine.INDEX_MANIFEST_FILENAME_POSTFIX}"
        val raw = readByPath(manifestPath)
        return raw
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    override fun readIndexFile(translation: String, name: String): ByteArray {
        require(name.isNotBlank()) { "Index file name is blank" }
        require(!name.contains('/')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains('\\')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains("..")) { "Index file name must not contain '..', got: $name" }

        val rel = "$base/$translation/index/$name"
        val fullPath = resourcesRoot / rel
        return fileSystem.read(fullPath) { readByteArray() }
    }
}