package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default

class ListCli : CliktCommand(name = "list") {

    val inquired: String by argument().default("bible")

    override fun run() {

        when (inquired) {
            "bible", "bibles", "translation", "translations", "version", "versions" -> {
                getTranslationDescriptions().forEach { echo(it) }
            }

            "book", "books" -> {
                (1..66).forEach { book ->
                    echo(bookNameNumberArray[book].joinToString(", "))
                }
            }
        }
    }
}
