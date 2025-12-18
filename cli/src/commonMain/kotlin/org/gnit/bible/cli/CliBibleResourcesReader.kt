package org.gnit.bible.cli

import org.gnit.bible.BibleResourcesReader

expect class CliBibleResourcesReader() : BibleResourcesReader {
    override fun chapterFile(translation: String, book: Int, chapter: Int): String
    override fun readByPath(path: String): String
    override fun getChapterText(translation: String, book: Int, chapter: Int): String
}