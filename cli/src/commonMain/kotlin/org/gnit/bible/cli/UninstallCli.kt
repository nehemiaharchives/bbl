package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
//import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.bible.Bible

class UninstallCli(
    private val bible: Bible
) : CliktCommand(name = "uninstall") {
    //private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Delete a downloaded bible translation pack from your computer"
    }

    private val target by argument(help = "translation code to uninstall (e.g., kttv)")

    override fun run() {
        val translationCodeCandidate = target.lowercase()
        val am = bible.assetManager
        val downloaded = am.downloadedTranslationCodes()
        val translation = downloaded.find { it == translationCodeCandidate }

        if (translation == null) {
            echo("Translation $translationCodeCandidate was not in the list of downloaded translations")
        } else {
            runCatching {
                am.delete(translationCodeCandidate)
            }.onSuccess {
                echo("Uninstalled $translationCodeCandidate")
            }.onFailure {
                echo("Uninstalling $translationCodeCandidate failed")
            }
        }
    }
}
