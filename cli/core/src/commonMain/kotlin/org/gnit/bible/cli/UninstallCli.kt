package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
//import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.bible.Bible

class UninstallCli(
    private val bible: Bible
) : CliktCommand(name = "uninstall") {
    //private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Delete one or more downloaded bible translation packs from your computer"
    }

    private val targets: List<String> by argument(help = "translation code(s) to uninstall (e.g., kttv th1971)")
        .multiple()

    override fun run() {
        val am = bible.assetManager
        val requestedCodes = targets
            .map { it.lowercase() }
            .distinct()

        val downloaded = runCatching { am.downloadedTranslationCodes() }
            .getOrDefault(emptyList())
            .toMutableSet()

        for (translationCode in requestedCodes) {
            if (translationCode !in downloaded) {
                echo("Translation $translationCode was not in the list of downloaded translations")
                continue
            }

            runCatching { am.delete(translationCode) }
                .onSuccess {
                    downloaded.remove(translationCode)
                    echo("Uninstalled $translationCode")
                }
                .onFailure {
                    echo("Uninstalling $translationCode failed")
                }
        }
    }
}
