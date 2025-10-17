package org.gnit.bible

class Bible(val assetManager: AssetManager = AssetManagerImpl()) {

    companion object{
        val embeddedTranslations = arrayOf(
            "cunp",
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
            "webus",
        )
    }

    fun availableTranslations(): Array<String> {
        return embeddedTranslations.plus(assetManager.downloadedTranslations())
    }

    lateinit var bibleTextReader: BibleTextReader
        set

    var zipBibleTextReader: ZipBibleTextReader? = null

    fun obtainZipBibleTextReader(): ZipBibleTextReader {
        if (zipBibleTextReader == null) {
            zipBibleTextReader = ZipBibleTextReader(assetManager.platform)
        }
        return zipBibleTextReader!!
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when{
            embeddedTranslations.contains(translation) -> bibleTextReader.getChapterText(translation = "webus", book = book, chapter = chapter)
            assetManager.downloadedTranslations().contains(translation) -> obtainZipBibleTextReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslations().joinToString(", ")}")
        }
    }
}
