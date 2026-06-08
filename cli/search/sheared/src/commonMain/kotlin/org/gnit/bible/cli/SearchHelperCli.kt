package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.BblVersion
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.SearchQueryText
import org.gnit.bible.VersePointerJson

open class SearchHelperCli(
    val searchHelperBinaryName: String,
    protected val bible: Bible,
    val analyzerProvider: AnalyzerProvider
) : CoreCliktCommand(name = searchHelperBinaryName) {
    private val termParts by argument(help = "search term").multiple()
    private val versionFlag by option("-v", "--version", help = "prints out software version of this program").flag()
    protected val translationCode by option("-t", "--translation", help = "translation code (e.g. webus)")
    protected val bookNumber by option("--book", help = "book number").convert { it.toInt() }
    protected val startChapter by option("--chapter", help = "chapter number").convert { it.toInt() }
    protected val endChapter by option("--end-chapter", help = "end chapter number").convert { it.toInt() }
    private val categoryKeys by option("--category", help = "category key").multiple()
    protected val verses by option("--verses", help = "max number of verses").convert { it.toInt() }.default(100)

    override fun run() {
        if (versionFlag) {
            echo(BblVersion.version)
            return
        }

        val term = SearchQueryText.searchTermFromArgs(termParts)
        if (term.isBlank()) throw UsageError("Missing search term")

        val translation = resolveTranslationOrThrow()
        val (start, end) = validateAndResolveChapterRange(bookNumber)
        val filters = Books.Category.resolveAllOrThrow(categoryKeys) {
            UsageError("Category key '$it' not found. Run 'bbl list categories' to see supported category names.")
        }

        val results = bible.search(
            term = term,
            bookNumber = bookNumber,
            startChapter = start,
            endChapter = end,
            verses = verses,
            filters = filters,
            translation = translation,
            analyzerProvider = analyzerProvider
        )

        if (results.isNotEmpty()) {
            echo(VersePointerJson.encodeList(results))
        }
    }

    protected open fun resolveTranslationOrThrow() =
        (translationCode ?: throw UsageError("Missing required option -t/--translation")).let { code ->
            val lowerCode = code.lowercase()
            if (!bible.findTranslationByCode(lowerCode)) {
                throw UsageError("Translation code '$lowerCode' not found (is the pack installed?)")
            }
            bible.availableTranslations().first { it.code == lowerCode }
        }

    protected open fun validateAndResolveChapterRange(bookNumber: Int?): Pair<Int?, Int?> {
        val start = startChapter
        val end = endChapter
        if (start != null && bookNumber == null) throw UsageError("--chapter requires --book")
        if (end != null && bookNumber == null) throw UsageError("--end-chapter requires --book")
        if (end != null && start == null) throw UsageError("--end-chapter requires --chapter")
        if (start != null && end != null && end < start) throw UsageError("--end-chapter must be >= --chapter")
        if (verses <= 0) throw UsageError("--verses must be > 0")
        return start to end
    }
}
