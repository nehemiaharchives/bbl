package org.gnit.bible

enum class RandomlyShow { verse, chapter }

enum class ConfigKey(val value: String, val defaultValue: String, val description: String){
    TRANSLATION("translation", Translation.webus.code, "default translation of bible, use code e.g. webus, jc"),
    RANDOMLY_SHOW("randomlyShow", RandomlyShow.verse.toString(), "[bbl rand] option to show a verse or a chapter"),
    HEADER("header", false.toString(), "bbl, bbl rand, bbl search option to show header, such as Genesis 1 or John 3:16 above the verses or not")
}

class Bible(val assetManager: AssetManager = AssetManagerImpl()) {

    fun availableTranslationCodes(): Array<String> {
        return embeddedTranslationCodes.plus(assetManager.downloadedTranslationCodes())
    }

    fun availableTranslations(): List<Translation> {
        val embeddedTranslations = Translation.embeddedTranslations
        val downloadedTranslations = assetManager.downloadedTranslations()
        return embeddedTranslations.plus(downloadedTranslations)
    }

    fun findTranslationByCode(code: String): Boolean {
        val foundInEmbedded = Translation.embeddedTranslations.find { it.code == code }
        if (foundInEmbedded != null) {
            return true
        }
        val foundInDownloaded = assetManager.downloadedTranslationCodes().find { it == code }
        return foundInDownloaded != null
    }

    lateinit var bibleResourcesReader: BibleResourcesReader

    var zipBibleResourcesReader: ZipBibleResourcesReader? = null

    fun obtainZipBibleResourcesReader(): ZipBibleResourcesReader {
        if (zipBibleResourcesReader == null) {
            zipBibleResourcesReader = ZipBibleResourcesReader(
                assetManager.platform,
                assetManager.fileSystem
            )
        }
        return zipBibleResourcesReader!!
    }

    var embeddedTranslationSearchEngine: SearchEngine? = null

    var zipTranslationSearchEngine: SearchEngine? = null

    fun obtainSearchEngine(isEmbedded: Boolean = true): SearchEngine {
        if(isEmbedded){
            if (embeddedTranslationSearchEngine == null) {
                embeddedTranslationSearchEngine = SearchEngine(bibleResourcesReader)
            }
            return embeddedTranslationSearchEngine!!
        }else {
            if (zipTranslationSearchEngine == null) {
                zipTranslationSearchEngine = SearchEngine(obtainZipBibleResourcesReader())
            }
            return zipTranslationSearchEngine!!
        }
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when{
            translation == "webus" && book == 1 && chapter == 1 -> webusGenesisChapterOne
            translation == "jc" && book == 1 && chapter == 1 -> jcGenesisChapterOne
            embeddedTranslationCodes.contains(translation) -> bibleResourcesReader.getChapterText(translation = translation, book = book, chapter = chapter)
            assetManager.downloadedTranslationCodes().contains(translation) -> obtainZipBibleResourcesReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslationCodes().joinToString(", ")}")
        }
    }

    fun defaultTranslationFromSettings(): Translation{
        val translationCode = assetManager.platform.settings.getStringOrNull(ConfigKey.TRANSLATION.value) ?: "webus"
        val translation = availableTranslations().firstOrNull { it.code == translationCode }?: Translation.webus
        return translation
    }

    fun randomlyShowFromSettings(): RandomlyShow {
        val randomlyShowString = assetManager.platform.settings.getStringOrNull(ConfigKey.RANDOMLY_SHOW.value) ?: "verse"
        return RandomlyShow.valueOf(randomlyShowString)
    }

    fun showHeaderFromSettings(): Boolean {
        val raw = assetManager.platform.settings.getStringOrNull(ConfigKey.HEADER.value) ?: ConfigKey.HEADER.defaultValue
        return raw.toBooleanStrictOrNull() ?: false
    }

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        translation: Translation
    ): List<String> {
        val isEmbedded = embeddedTranslationCodes.contains(translation.code)
        val searchEngine = obtainSearchEngine(isEmbedded)
        return searchEngine.search(term, bookNumber, startChapter, endChapter, verses, translation)
    }

    companion object {
        fun splitChapterToVerses(aChapter: String): Array<String> {
            return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
        }

        fun parse(translation: Translation, book: List<String>, chapterVerse: String): VersePointer {

            val bookString = book.joinToString(separator = " ") { it.lowercase() }

            val bookNumber = bookNumber(bookString)

            val chapterVerseSplit = chapterVerse.split(":")

            val chapterNumber = chapterVerseSplit[0].toInt()

            val startVerse = if (chapterVerseSplit.size == 2) chapterVerseSplit[1].split("-")[0].toInt() else null

            val endVerse = if (chapterVerseSplit.size == 2 && chapterVerse.contains("-")) chapterVerseSplit[1].split("-")[1].toInt() else null

            return VersePointer(
                translation = translation,
                book = bookNumber,
                chapter = chapterNumber,
                startVerse = startVerse,
                endVerse = endVerse
            )
        }

        fun selectVerses(versePointer: VersePointer, aChapter: String): String {

            val start = versePointer.startVerse
            val end = versePointer.endVerse

            var selected = aChapter

            if (start != null) {

                val verses = splitChapterToVerses(aChapter)

                if (end == null) {
                    selected = start.toString() + " " + verses[start - 1]
                } else {
                    val list = mutableListOf<String>()

                    (start..end).forEach { verseNumber ->
                        list.add(verseNumber.toString() + " " + verses[verseNumber - 1])
                    }

                    selected = list.joinToString("\n")
                }
            }

            return selected
        }
    }
}
