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

    override fun help(context: Context) = "Display a random verse or chapter from the Bible"

    private val narrowDown: String? by argument(
        help = "Narrow down to a category (e.g., nt, ot, prophets, paul, abraham, gospels) for the list of categories, run bbl list category"
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
    }

    private fun resolveFilter(key: String): BibleFilter {
        val category = Books.Category.fromKey(key)
            ?: throw IllegalArgumentException("Unknown category '$key'")
        return category.filter
    }
}
