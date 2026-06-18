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
        A random verse or chapter, optionally filtered by category (e.g. bbl rand nt)
        
        Examples:
        
        # random verse from entire bible
        bbl rand
        
        # random verse from New Testament
        bbl rand nt
        
        # random verse from Old Testament
        bbl rand ot
        
        # random verse from four gospels
        bbl rand gospels
        bbl rand g

        # show random chapter
        bbl config randomlyShow chapter
        
        # show random verse (default config)
        bbl config randomlyShow verse
        
        # shortcut 
        bbl r
        bbl r g       
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
