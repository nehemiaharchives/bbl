package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.downloadableTranslations

class InstallCli(
    private val bible: Bible
) : CliktCommand(name = "install") {
    private val logger = KotlinLogging.logger {}

    private val target by argument(help = "download and install a translation using code e.g. bbl install kttv")

    override fun run() {
        val translationCodeCandidate = target.lowercase()
        val am = bible.assetManager
        val downloadable = runBlocking {
            runCatching { am.downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL) }
                .onFailure { logger.debug { "ListCli failed to download latest downloadable translation list" } }
                .getOrDefault(downloadableTranslations)
        }
        val translation = downloadable.find { it.code == translationCodeCandidate }

        if (translation == null) {
            echo("Translation $translationCodeCandidate was not in the list of downloadable translations")
        } else {
            runBlocking {
                runCatching {
                    am.download(
                        DOWNLOADABLE_BIBLE_BASE_URL,
                        "${translation.code}.zip"
                    )
                }.onFailure {
                    echo("Downloading $translationCodeCandidate failed")
                }.onSuccess {
                    echo("Installed $translationCodeCandidate")
                }
            }
        }
    }
}
