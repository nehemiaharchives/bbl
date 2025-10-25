package org.gnit.bible

class Bible(val assetManager: AssetManager = AssetManagerImpl()) {

    companion object{
        val embeddedTranslationCodes = arrayOf(
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

    fun availableTranslationCodes(): Array<String> {
        return embeddedTranslationCodes.plus(assetManager.downloadedTranslationCodes())
    }

    fun availableTranslations(): List<Translation> {
        val embeddedTranslations = Translation.embeddedTranslations
        val downloadedTranslations = assetManager.downloadedTranslationCodes().map { code ->
            obtainZipBibleTextReader().getTranslationFromManifest(code)
        }
        return embeddedTranslations.plus(downloadedTranslations)
    }

    lateinit var bibleTextReader: BibleTextReader

    var zipBibleTextReader: ZipBibleTextReader? = null

    fun obtainZipBibleTextReader(): ZipBibleTextReader {
        if (zipBibleTextReader == null) {
            zipBibleTextReader = ZipBibleTextReader(assetManager.platform)
        }
        return zipBibleTextReader!!
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when{
            embeddedTranslationCodes.contains(translation) -> bibleTextReader.getChapterText(translation = "webus", book = book, chapter = chapter)
            assetManager.downloadedTranslationCodes().contains(translation) -> obtainZipBibleTextReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslationCodes().joinToString(", ")}")
        }
    }
}
