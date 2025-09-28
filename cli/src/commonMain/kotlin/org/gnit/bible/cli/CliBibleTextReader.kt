package org.gnit.bible.cli

import org.gnit.bible.BibleTextReader

expect class CliBibleTextReader() : BibleTextReader {
    override fun chapterFile(translation: String, book: Int, chapter: Int): String
    override fun readByPath(path: String): String
    override fun getChapterText(translation: String, book: Int, chapter: Int): String
}