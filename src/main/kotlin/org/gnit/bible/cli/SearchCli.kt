package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.Translation

class SearchCli(val env: Environment, val config: Config) : CliktCommand(name = "search") {

    val numberOfSearchResultVersesOverride by option("-r", "--result", help = "number of search result verses")
    val searchInputs: List<String> by argument().multiple()

    fun getTranslationFrom(searchInput: String): Translation? {

        var overridden: Translation? = null

        Translation.values().forEach { translation ->
            if (searchInput.endsWith("in $translation")) {
                overridden = translation
            }
        }

        return overridden
    }

    lateinit var term: String
    lateinit var translation: Translation
    lateinit var result: List<String>

    override fun run() {

        val searchInput = if (env == Environment.PRODUCTION && isWindows()) {
            WindowsCommandLine().getCommandLineArguments(this.commandName, searchInputs)
        } else {
            searchInputs
        }.joinToString(separator = " ")

        // bbl search God in kjv -> "in kjv" will be detected
        val overrideTranslation = getTranslationFrom(searchInput)
        if (overrideTranslation == null) {
            translation = config.translation
            term = searchInput
        } else {
            translation = overrideTranslation
            term = searchInput.replace("in $overrideTranslation", "").trim()
        }

        // bbl search Gdo in gen, in gen 1, or in gen 2-4 will be detected
        val bookChapterFilter = filterByBookChapter(term)
        term = bookChapterFilter.term

        // bbl search God in kjv in gen 1 -> "in kjv" will be detected
        val overrideTranslationInMiddle = getTranslationFrom(term)
        if (overrideTranslationInMiddle != null) {
            translation = overrideTranslationInMiddle
            term = term.replace("in $overrideTranslationInMiddle", "").trim()
        }

        result = search(
            term = term,
            bookNumber = bookChapterFilter.book,
            startChapter = bookChapterFilter.startChapter,
            endChapter = bookChapterFilter.endChapter,
            verses = numberOfSearchResultVersesOverride?.toInt() ?: config.searchResult,
            translation = translation
        )

        result.forEach { verse ->
            echo(verse)
        }
    }
}