package org.gnit.bible

class Bible() {

    val embeddedTranslations = arrayOf("cunp",
        "delut",
        "jc",
        "kjv",
        "krv",
        "lsg",
        "rdv24",
        "rvr09",
        "sinod",
        "sven",
        "svrj",
        "tb",
        "ubg",
        "ubio",
        "webu",)

    private val platform = getPlatform()

    lateinit var bibleTextReader: BibleTextReader

    fun verses(book: Int = 1, chapter: Int = 1): String {
        return bibleTextReader.getChapterText(translation = "webus", book = book, chapter = chapter)
   }
}