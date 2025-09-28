package org.gnit.bible

class Bible() {
    private val platform = getPlatform()

    lateinit var bibleTextReader: BibleTextReader

    fun verses(book: Int = 1, chapter: Int = 1): String {
        return bibleTextReader.getChapterText(translation = "webus", book = book, chapter = chapter)
   }
}