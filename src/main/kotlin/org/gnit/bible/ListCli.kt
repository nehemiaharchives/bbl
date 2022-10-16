package org.gnit.bible

import com.github.ajalt.clikt.core.CliktCommand

class ListCli : CliktCommand(name = "list") {
    lateinit var translationDescriptions: kotlin.collections.List<String>
    override fun run() {
        translationDescriptions = getTranslationDescriptions()
        translationDescriptions.forEach { echo(it) }
    }
}