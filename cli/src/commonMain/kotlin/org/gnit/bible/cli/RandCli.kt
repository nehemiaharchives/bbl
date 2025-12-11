package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.RandPicker
import org.gnit.bible.RandomlyShow
import org.gnit.bible.Translation
import org.gnit.bible.bookNameCapital

data class Config(
    val translation: Translation = Translation.webus,
    val randomlyShow: RandomlyShow = RandomlyShow.verse
)

class RandCli(
    private val bible: Bible,
    private val config: Config = Config(),
    private val picker: RandPicker = RandPicker(
        readChapter = { translation, book, chapter -> bible.verses(translation, book, chapter) }
    )
) : CliktCommand(name = "rand") {

    override fun help(context: com.github.ajalt.clikt.core.Context) = "Display a random verse or chapter from the Bible"

    private val narrowDown: String? by argument(
        help = "Narrow down to a category (e.g., nt, ot, prophets, paul, abraham, gospels)"
    ).optional()

    override fun run() {
        val filter = narrowDown?.let { resolveFilter(it) } ?: BibleFilter.All

        val result = picker.random(
            translation = config.translation,
            filter = filter,
            randomlyShow = config.randomlyShow
        )

        val pointer = result.pointer
        val header = when (result.selectionType) {
            RandomlyShow.chapter -> "${bookNameCapital(pointer.book)} ${pointer.chapter}"
            RandomlyShow.verse -> "${bookNameCapital(pointer.book)} ${pointer.chapter}:${pointer.startVerse}"
        }

        echo(header)
        echo(result.selection)
    }

    private fun resolveFilter(key: String): BibleFilter {
        val category = Books.Category.fromKey(key)
            ?: throw IllegalArgumentException("Unknown category '$key'")
        return category.filter
    }
}
