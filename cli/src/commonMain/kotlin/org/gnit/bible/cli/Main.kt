package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer

class Bbl(
    private val bible: Bible = Bible().apply { bibleTextReader = CliBibleTextReader() }
) : CliktCommand() {

    override val invokeWithoutSubcommand = true
    val book: List<String> by argument().multiple(default = listOf("gen"))
    val chapterVerse: String by argument().default("1")
    val versionFlag by option("-v", "--version", help = "prints out software version of this program").flag()

    lateinit var versePointer: VersePointer
    lateinit var chapterText: String
    lateinit var selectedVerses: String

    init {
        subcommands(
            In(bible),
            //SearchCli(env = Environment.PRODUCTION, config),
            //RandCli(config),
            ListCli(bible),
            InstallCli(bible),
            UninstallCli(bible)
        )
    }

    override fun aliases(): Map<String, List<String>> = mapOf(
        "get" to listOf("install"),
        "remove" to listOf("uninstall"),
        "delete" to listOf("uninstall")
    )

    override fun run() {

        if (versionFlag) {
            echo(versionOutput)
        } else {
            versePointer = Bible.parse(translation = Translation.webus/* TODO need to feed configured default translation later  */, book = book, chapterVerse = chapterVerse)

            val subCommand = currentContext.invokedSubcommand
            if (subCommand == null) {
                chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
                selectedVerses = Bible.selectVerses(versePointer, chapterText)
                echo(selectedVerses)
            } else {
                // going to move on subCommand
                currentContext.findOrSetObject { versePointer }
            }
        }
    }
}

class In(
    private val bible: Bible
) : CliktCommand() {

    val translationOverride: String by argument(help = "specify translation with translation code")
    val versePointer by requireObject<VersePointer>()

    lateinit var selectedVerses: String

    override fun run() {
        val found = bible.findTranslationByCode(translationOverride)
        if(!found){
            echo("Translation code not found", err = true)
            return
        }else{
            versePointer.translation = bible.availableTranslations().first { it.code == translationOverride }
            val chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
            selectedVerses = Bible.selectVerses(versePointer, chapterText)
            echo(selectedVerses)
        }
    }
}

fun main(args: Array<String>) {
    Bbl().main(args)
}
