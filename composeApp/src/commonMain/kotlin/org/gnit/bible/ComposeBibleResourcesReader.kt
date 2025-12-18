package org.gnit.bible

import org.gnit.bible.cmp.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.InternalResourceApi

class ComposeBibleResourcesReader : BibleResourcesReader {

    private val cache = HashMap<String, String>()

    override fun chapterFile(translation: String, book: Int, chapter: Int): String =
        "files/$base/$translation/$translation.$book.$chapter.txt"

    @OptIn(InternalResourceApi::class)
    override fun readByPath(path: String): String {
        cache[path]?.let { return it }
        val text = runBlocking {
            withContext(Dispatchers.IO) {
                Res.readBytes(path).decodeToString()
            }
        }

        cache[path] = text
        return text
    }

    override fun listIndexFiles(translation: String): List<String> {
        val manifestPath = "files/$base/$translation/index/$translation${SearchEngine.INDEX_MANIFEST_FILENAME_POSTFIX}"
        val text = readByPath(manifestPath)
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    override fun readIndexFile(translation: String, name: String): ByteArray {
        require(name.isNotBlank()) { "Index file name is blank" }
        require(!name.contains('/')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains('\\')) { "Index file name must be a flat filename, got: $name" }
        require(!name.contains("..")) { "Index file name must not contain '..', got: $name" }

        val path = "files/$base/$translation/index/$name"

        return runBlocking {
            withContext(Dispatchers.IO) {
                Res.readBytes(path)
            }
        }
    }
}
