package org.gnit.bible

import org.gnit.bible.cmp.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.InternalResourceApi

class ComposeBibleTextReader : BibleTextReader {

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
}
