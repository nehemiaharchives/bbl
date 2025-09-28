package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import org.gnit.bible.Bible

class Bbl: CliktCommand() {

    val bible = Bible().apply { bibleTextReader = CliBibleTextReader() }

    override fun run() {
        echo(bible.verses())
    }
}

fun main(args: Array<String>) {
    Bbl().main(args)
}
