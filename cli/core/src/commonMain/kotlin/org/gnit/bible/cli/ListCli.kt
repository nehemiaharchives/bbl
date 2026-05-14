package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.InstallationState
import org.gnit.bible.Translation
import org.gnit.bible.TranslationEntry
import org.gnit.bible.bookNameNumberArray
import org.gnit.bible.Translation.Companion.downloadableTranslationsCmp

internal fun formatTranslationEntries(entries: List<TranslationEntry>): List<String> {
    val codeWidth = 7
    val englishNameWidth = 43
    val nativeNameWidth = 33
    val languageWidth = 11
    val yearWidth = 5
    val installationWidth = 10

    fun String.separator(): String = this.plus("|")

    fun String.byteLength(): Int = this.encodeToByteArray().size

    fun Translation.byteDiff(): Int = when (this.code) {
        // following works on Ghostty
        "th1971" -> -5
        "irvhin" -> -9
        "irvben" -> -10
        "irvmar" -> -8
        "irvtel" -> -13
        "irvtam" -> -12
        "irvguj" -> -9
        "irvurd" -> -8
        "npiulb" -> -3
        else -> 0
    }

    return entries.map { entry ->
        val t = entry.translation
        val code = t.code.uppercase().padEnd(codeWidth).separator()
        val englishName = t.englishName.padEnd(englishNameWidth).separator()
        val byteDiff =
            if (t.language.isCJK) (t.nativeName.byteLength() - t.nativeName.length) / 2 else t.byteDiff()
        val nativeName = t.nativeName.padEnd(nativeNameWidth - byteDiff).separator()
        val language = t.language.englishName.padEnd(languageWidth).separator()
        val year = t.year.toString().padEnd(yearWidth).separator()
        val installation = entry.source.description.padEnd(installationWidth).separator()
        val copyright = t.copyright
        "$code $englishName $nativeName $language $year $installation $copyright"
    }
}

class ListCli(
    private val bible: Bible
) : CliktCommand(name = "list") {

    override fun help(context: Context): String {
        return "List bibles/translations"
    }

    private val logger = KotlinLogging.logger {}

    private val target by argument(help = "What to list: bibles/translations or books")
        .default("bibles")

    override fun run() {

        when (target.lowercase()) {
            "bible", "bibles", "translation", "translations", "version", "versions" -> {
                val am = bible.assetManager
                val downloadable = runBlocking {
                    runCatching { am.downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL) }
                        .onFailure { logger.debug { "ListCli failed to download latest downloadable translation list, falling back to built-in translation catalog" } }
                        .getOrDefault(downloadableTranslationsCmp)
                }
                val mergedDownloadable = (downloadable + downloadableTranslationsCmp)
                    .associateBy { it.code }
                    .values
                    .toList()
                val downloaded = am.downloadedTranslations()

                val downloadedEntries =
                    downloaded.map { TranslationEntry(it, InstallationState.DOWNLOADED) }

                val takenCodes = (downloaded.map { it.code }).toSet()
                val downloadableEntries = mergedDownloadable
                    .filterNot { takenCodes.contains(it.code) }
                    .map { TranslationEntry(it, InstallationState.DOWNLOADABLE) }

                val entries: List<TranslationEntry> =
                    (downloadedEntries + downloadableEntries).sortedBy { entry ->
                        entry.translation.language.order
                    }

                formatTranslationEntries(entries).forEach { line -> echo(line) }
            }

            "book", "books" -> {
                (1..66).forEach { book ->
                    echo(bookNameNumberArray[book].joinToString(", "))
                }
            }

            "category", "categories" -> {
                Books.Category.entries
                    .asSequence()
                    .filterNot { it == Books.Category.ALL }
                    .forEach { category ->
                        echo(category.name + ": " + category.key.joinToString(", "))
                    }
            }

            else -> echo("Unknown list target '$target'. Try one of: bibles, translations.")
        }
    }
}
