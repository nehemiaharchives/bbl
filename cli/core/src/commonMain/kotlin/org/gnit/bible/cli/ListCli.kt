package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.InstallationState
import org.gnit.bible.Translation
import org.gnit.bible.TranslationEntry

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
) : CoreCliktCommand(name = "list") {

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
                val downloadedEntries = CliTranslationCatalog.downloadedTranslationEntries(bible)
                val downloadableEntries = CliTranslationCatalog.downloadableTranslationEntries(bible)

                val entries: List<TranslationEntry> =
                    (downloadedEntries + downloadableEntries).sortedBy { entry ->
                        entry.translation.language.order
                    }

                formatTranslationEntries(entries).forEach { line -> echo(line) }
            }

            "book", "books" -> {
                (1..66).forEach { book ->
                    echo(Books.allBookNames[book].joinToString(", "))
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
