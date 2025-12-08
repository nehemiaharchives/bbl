package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.gnit.bible.Bible
import org.gnit.bible.InstallationState

class Bbl(
    private val bible: Bible = Bible().apply { bibleTextReader = CliBibleTextReader() }
) : CliktCommand() {

    override val invokeWithoutSubcommand = true

    init {
        subcommands(
            //In(),
            //SearchCli(env = Environment.PRODUCTION, config),
            //RandCli(config),
            ListCli(bible),
            InstallCli(bible)
        )
    }

    override fun run() {

        val subCommand = currentContext.invokedSubcommand

        if(subCommand == null) {
            echo(bible.verses())
        } else {
            // going to move on subCommand
            currentContext.findOrSetObject { /*versePointer*/ }
        }
    }

    override fun aliases(): Map<String, List<String>> = mapOf(
        "get" to listOf("install")
    )
}

fun main(args: Array<String>) {
    Bbl().main(args)
}
