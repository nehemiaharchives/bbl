package org.gnit.bible

class Bible(
    val assetManager: AssetManager = AssetManagerImpl()
) {

    private fun hasEmbeddedReader(): Boolean = this::bibleResourcesReader.isInitialized

    fun availableTranslationCodes(): Array<String> {
        val embedded = if (hasEmbeddedReader()) SupportedTranslation.embeddedCodes else emptyArray()
        return embedded.plus(assetManager.downloadedTranslationCodes())
    }

    fun availableTranslations(): List<Translation> {
        val embeddedTranslations = if (hasEmbeddedReader()) SupportedTranslation.embeddedTranslations else emptyList()
        val downloadedTranslations = assetManager.downloadedTranslations()
        return embeddedTranslations.plus(downloadedTranslations)
    }

    fun findTranslationByCode(code: String): Boolean {
        val foundInEmbedded = if (hasEmbeddedReader()) SupportedTranslation.embeddedTranslations.find { it.code == code } else null
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

    private val defaultAnalyzerProvider = SimpleAnalyzerProvider()

    private val embeddedTranslationSearchEngines = mutableMapOf<AnalyzerProvider, SearchEngine>()

    private val zipTranslationSearchEngines = mutableMapOf<AnalyzerProvider, SearchEngine>()

    fun obtainSearchEngine(
        isEmbedded: Boolean = true,
        analyzerProvider: AnalyzerProvider = defaultAnalyzerProvider
    ): SearchEngine {
        val embedded = hasEmbeddedReader() && isEmbedded
        return if (embedded) {
            embeddedTranslationSearchEngines.getOrPut(analyzerProvider) {
                SearchEngine(bibleResourcesReader, analyzerProvider)
            }
        } else {
            zipTranslationSearchEngines.getOrPut(analyzerProvider) {
                SearchEngine(obtainZipBibleResourcesReader(), analyzerProvider)
            }
        }
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when {
            hasEmbeddedReader() && translation == "webus" && book == 1 && chapter == 1 -> webusGenesisChapterOne
            hasEmbeddedReader() && translation == "jc" && book == 1 && chapter == 1 -> jcGenesisChapterOne
            hasEmbeddedReader() && SupportedTranslation.embeddedCodes.contains(translation) ->
                bibleResourcesReader.getChapterText(translation = translation, book = book, chapter = chapter)
            assetManager.downloadedTranslationCodes().contains(translation) ->
                obtainZipBibleResourcesReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslationCodes().joinToString(", ")}")
        }
    }

    fun defaultTranslationFromSettings(): Translation{
        val translationCode = assetManager.platform.configSettings.getStringOrNull(ConfigKey.TRANSLATION.value) ?: ConfigKey.TRANSLATION.defaultValue
        val translation = availableTranslations().firstOrNull { it.code == translationCode }
            ?: availableTranslations().firstOrNull()
            ?: SupportedTranslation.WEBUS.translation
        return translation
    }

    fun searchResultFromSettings(): Int {
        return assetManager.platform.configSettings.getInt(
            ConfigKey.SEARCH_RESULT.value,
            ConfigKey.SEARCH_RESULT.defaultValue.toInt()
        )
    }

    fun randomlyShowFromSettings(): RandomlyShow {
        val randomlyShowString = assetManager.platform.configSettings.getStringOrNull(ConfigKey.RANDOMLY_SHOW.value)
            ?: ConfigKey.RANDOMLY_SHOW.defaultValue
        return RandomlyShow.valueOf(randomlyShowString)
    }

    fun compareByFromSettings(): CompareBy {
        val compareByString = assetManager.platform.configSettings.getStringOrNull(ConfigKey.COMPARE_BY.value)
            ?: ConfigKey.COMPARE_BY.defaultValue
        return CompareBy.valueOf(compareByString)
    }

    fun historyEnabledFromSettings(): Boolean {
        val raw = assetManager.platform.configSettings.getStringOrNull(ConfigKey.HISTAORY_ENABLED.value)
            ?: ConfigKey.HISTAORY_ENABLED.defaultValue
        return raw.toBooleanStrictOrNull() ?: true
    }

    fun historyFormatFromSettings(): HistoryFormat {
        val raw = assetManager.platform.configSettings.getStringOrNull(ConfigKey.HISTAORY_FROMAT.value)
            ?: ConfigKey.HISTAORY_FROMAT.defaultValue
        return HistoryFormat.entries.firstOrNull { it.name == raw } ?: HistoryFormat.command
    }

    fun showHeaderFromSettings(): Boolean {
        val raw = assetManager.platform.configSettings.getStringOrNull(ConfigKey.HEADER.value) ?: ConfigKey.HEADER.defaultValue
        return raw.toBooleanStrictOrNull() ?: false
    }

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        filter: BibleFilter,
        translation: Translation,
        analyzerProvider: AnalyzerProvider = defaultAnalyzerProvider
    ): List<VersePointer>{
        return search(term, bookNumber, startChapter, endChapter, verses, listOf(filter), translation, analyzerProvider)
    }

    fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        verses: Int = 100,
        filters: List<BibleFilter> = emptyList(),
        translation: Translation,
        analyzerProvider: AnalyzerProvider = defaultAnalyzerProvider
    ): List<VersePointer> {
        val isEmbedded = hasEmbeddedReader() && SupportedTranslation.embeddedCodes.contains(translation.code)
        val searchEngine = obtainSearchEngine(isEmbedded, analyzerProvider)
        return searchEngine.search(term, bookNumber, startChapter, endChapter, verses, filters, translation)
    }

    companion object {

        /**
         * Split a chapter into an array of verses removing verse numbers.
         * @param aChapter The chapter to split, the format is verse number then space then verse text.
         * @return the aray of verses without the verse number.
         */
        fun splitChapterToVerses(aChapter: String): Array<String> {
            return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
        }

        fun parse(translation: Translation, book: List<String>, chapterVerse: String): VersePointer {

            val bookString = book.joinToString(separator = " ") { it.lowercase() }

            val bookNumber = Books.bookNumber(bookString)

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

                // Bible packs already include the verse number at the start of each verse line,
                // so we don't need to add it again here.
                selected = if (end == null) {
                    verses[start - 1]
                } else {
                    verses.slice((start - 1)..(end - 1)).joinToString("\n")
                }
            }

            return selected
        }
    }
}
