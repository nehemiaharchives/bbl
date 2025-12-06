package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.gnit.bible.Bible

class Bbl(
    private val bible: Bible = Bible().apply { bibleTextReader = CliBibleTextReader() }
) : CliktCommand() {

    override val invokeWithoutSubcommand = true

    init {
        subcommands(ListCli(bible))
    }

    override fun run() {
        echo(bible.verses())
    }
}

fun main(args: Array<String>) {
    Bbl().main(args)
}
