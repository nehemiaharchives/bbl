package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.RandPicker

class RandCli(
    private val bible: Bible,
    private val picker: RandPicker = RandPicker(
        readChapter = { translation, book, chapter -> bible.verses(translation, book, chapter) }
    )
) : CoreCliktCommand(name = "rand") {

    override fun help(context: Context) = """
        Show a random verse or chapter (shortcut: bbl r)

        bbl rand                  Random selection from the whole Bible
        bbl rand <category>       Random selection from a category

        Examples: bbl rand nt, bbl rand ot, bbl rand gospels, bbl rand g
        Run `bbl list categories` for available categories.

        Set what is shown:
        bbl config randomlyShow verse|chapter        Default: verse
    """.trimIndent()

    private val narrowDown: String? by argument(
        help = "Narrow down to a category (e.g., nt, ot, prophets, paul, abraham, gospels, ref: bbl list category)"
    ).optional()

    override fun run() {
        val filter = narrowDown?.let { resolveFilter(it) } ?: BibleFilter.All

        val result = picker.random(
            translation = bible.defaultTranslationFromSettings(),
            filter = filter,
            randomlyShow = bible.randomlyShowFromSettings()
        )

        val pointer = result.pointer

        if (bible.showHeaderFromSettings()) {
            val header = Books.formatHeader(pointer)
            echo(header)
        }
        echo(result.selection)
        BblHistory.record(bible, BblHistory.command("bbl rand", narrowDown))
    }

    private fun resolveFilter(key: String): BibleFilter {
        val category = Books.Category.fromKey(key)
            ?: throw IllegalArgumentException("Unknown category '$key'")
        return category.filter
    }
}
