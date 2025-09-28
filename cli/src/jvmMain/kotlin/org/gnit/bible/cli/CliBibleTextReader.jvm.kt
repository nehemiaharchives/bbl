package org.gnit.bible.cli

import org.gnit.bible.BibleTextReader

actual class CliBibleTextReader : BibleTextReader {
    actual override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        // TODO("Not yet implemented")
        return ""
    }

    actual override fun readByPath(path: String): String {
        // TODO("Not yet implemented")
        return ""
    }

    actual override fun getChapterText(translation: String, book: Int, chapter: Int): String {
        // TODO("Not yet implemented")
        return "In the beginning God created the heaven and the earth."
    }
}