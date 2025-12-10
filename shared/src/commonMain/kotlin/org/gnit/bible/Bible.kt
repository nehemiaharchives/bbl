package org.gnit.bible

class Bible(val assetManager: AssetManager = AssetManagerImpl()) {

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
            zipBibleTextReader = ZipBibleTextReader(
                assetManager.platform,
                assetManager.fileSystem
            )
        }
        return zipBibleTextReader!!
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when{
            translation == "webus" && book == 1 && chapter == 1 -> webusGenesisChapterOne
            translation == "jc" && book == 1 && chapter == 1 -> jcGenesisChapterOne
            embeddedTranslationCodes.contains(translation) -> bibleTextReader.getChapterText(translation = translation, book = book, chapter = chapter)
            assetManager.downloadedTranslationCodes().contains(translation) -> obtainZipBibleTextReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslationCodes().joinToString(", ")}")
        }
    }

    companion object {
        fun splitChapterToVerses(aChapter: String): Array<String> {
            return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
        }
    }
}
