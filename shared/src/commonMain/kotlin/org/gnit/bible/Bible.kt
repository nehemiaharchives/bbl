package org.gnit.bible

import kotlin.collections.get

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
