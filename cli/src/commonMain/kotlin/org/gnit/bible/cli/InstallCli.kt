package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
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

    override fun help(context: Context): String {
        return "Download and install a translation using code e.g. bbl install kttv"
    }

    private val target by argument(help = "translation code to download and install (e.g., kttv)")

    override fun run() {
        logger.debug { "InstallCli called" }
        val translationCodeCandidate = target.lowercase()
        val am = bible.assetManager

        val alreadyInstalled = runCatching { am.downloadedTranslationCodes() }
            .getOrDefault(emptyList())
            .any { it == translationCodeCandidate }
        if (alreadyInstalled) {
            echo("$translationCodeCandidate already installed, skipping download")
            return
        }

        val downloadable = runBlocking {
            runCatching { am.downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL) }
                .onFailure { logger.debug { "InstallCli failed to download latest downloadable translation list, falling back to embedded list" } }
                .getOrDefault(downloadableTranslations)
        }
        val translation = downloadable.find { it.code == translationCodeCandidate }

        if (translation == null) {
            throw CliktError("Translation $translationCodeCandidate was not in the list of downloadable translations: $downloadable")
        } else {
            logger.debug { "InstallCli downloading $translationCodeCandidate" }
            val downloadResult = runBlocking {
                runCatching {
                    am.download(
                        DOWNLOADABLE_BIBLE_BASE_URL,
                        "${translation.code}.zip"
                    )
                }
            }
            downloadResult.onFailure { error ->
                throw CliktError("Installing $translationCodeCandidate failed: ${error.message}")
            }

            val installedNow = runCatching { am.downloadedTranslationCodes() }
                .getOrDefault(emptyList())
                .any { it == translationCodeCandidate }
            if (!installedNow) {
                throw CliktError("Installing $translationCodeCandidate failed: file was not found after download")
            }

            echo("Installed $translationCodeCandidate")
        }
    }
}
