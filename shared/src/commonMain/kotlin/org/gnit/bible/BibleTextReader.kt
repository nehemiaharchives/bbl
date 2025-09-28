package org.gnit.bible

interface BibleTextReader {

    val base: String
        get() = "bblpacks"

    fun chapterFile(translation: String, book: Int, chapter: Int): String

    fun readByPath(path: String): String

    fun getChapterText(translation: String, book: Int, chapter: Int): String {
        val path = chapterFile(translation, book, chapter)
        return readByPath(path)
    }
}
