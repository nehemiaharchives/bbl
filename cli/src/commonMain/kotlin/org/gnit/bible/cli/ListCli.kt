package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.InstallationState
import org.gnit.bible.Translation
import org.gnit.bible.TranslationEntry
import org.gnit.bible.bookNameNumberArray
import org.gnit.bible.downloadableTranslations

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
                        .onFailure { logger.debug { "ListCli failed to download latest downloadable translation list" } }
                        .getOrDefault(downloadableTranslations)
                }
                val downloaded = am.downloadedTranslations()

                val embedded = Translation.embeddedTranslations

                val embeddedEntries = embedded.map { TranslationEntry(it, InstallationState.EMBEDDED) }
                val downloadedEntries = downloaded.map { TranslationEntry(it, InstallationState.DOWNLOADED) }

                val takenCodes = (embedded.map { it.code } + downloaded.map { it.code }).toSet()
                val downloadableEntries = downloadable
                    .filterNot { takenCodes.contains(it.code) }
                    .map { TranslationEntry(it, InstallationState.DOWNLOADABLE) }

                val entries: List<TranslationEntry> = embeddedEntries + downloadedEntries + downloadableEntries

                val codeWidth = 6
                val englishNameWidth = 43
                val languageWidth = 11
                val yearWidth = 4
                val installationWidth = 13

                val border = if(bible.showBorderFromSettings()) Borders.ALL else Borders.NONE

                val totalTableWidth = codeWidth + englishNameWidth + languageWidth + yearWidth + installationWidth
                logger.debug { "ListCli terminal size is ${terminal.size}, total table width is $totalTableWidth" }
                val table = table {
                    cellBorders = border

                    column(0) { width = ColumnWidth.Fixed(codeWidth) } 
                    column(1) { width = ColumnWidth.Fixed(englishNameWidth) }
                    column(2) { width = ColumnWidth.Fixed(languageWidth) }
                    column(3) { width = ColumnWidth.Fixed(yearWidth) }
                    column(4) { width = ColumnWidth.Fixed(installationWidth) }
                    header {
                        row("Code", "English Name", "Language", "Year", "Status")
                    }
                    body {
                        entries.forEach { entry ->
                            val t = entry.translation
                            val state = when (entry.source) {
                                InstallationState.EMBEDDED -> "Embedded"
                                InstallationState.DOWNLOADED -> "Installed"
                                InstallationState.DOWNLOADABLE -> "To Download"
                            }
                            row(
                                t.code.uppercase(),
                                t.englishName,
                                t.language.englishName,
                                t.year.toString(),
                                state
                            )
                        }
                    }
                }
                //echo(terminal.render(table))
                echo(table)
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
                        echo("${category.name}: ${category.key.joinToString(", ")}")
                    }
            }

            else -> echo("Unknown list target '$target'. Try one of: bibles, translations.")
        }
    }
}
