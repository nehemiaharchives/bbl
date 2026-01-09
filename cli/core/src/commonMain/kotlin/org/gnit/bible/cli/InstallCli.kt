package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.downloadableTranslationsCli

class InstallCli(
    private val bible: Bible
) : CliktCommand(name = "install") {
    private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Download and install one or more translations by code (e.g. bbl install kttv th1971)"
    }

    private val targets: List<String> by argument(help = "translation code(s) to download and install (e.g., kttv th1971)")
        .multiple()

    override fun run() {
        logger.debug { "InstallCli called" }
        val am = bible.assetManager

        val requestedCodes = targets
            .map { it.lowercase() }
            .distinct()

        val installedCodes = runCatching { am.downloadedTranslationCodes() }
            .getOrDefault(emptyList())
            .toSet()

        val toInstall = requestedCodes.filterNot { it in installedCodes }
        if (toInstall.isEmpty()) {
            requestedCodes.forEach { echo("$it already installed, skipping download") }
            return
        }

        val downloadable = runBlocking {
            runCatching { am.downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL) }
                .onFailure { logger.debug { "InstallCli failed to download latest downloadable translation list, falling back to embedded list" } }
                .getOrDefault(downloadableTranslationsCli)
        }

        val availableDownloadableCodes = downloadable.map { it.code }.toSet()
        val unknownCodes = toInstall.filterNot { it in availableDownloadableCodes }
        if (unknownCodes.isNotEmpty()) {
            throw CliktError(
                "Translation code(s) not found: ${unknownCodes.joinToString()}. " +
                    "Run 'bbl list translations' to see downloadable translations."
            )
        }

        requestedCodes
            .filter { it in installedCodes }
            .forEach { echo("$it already installed, skipping download") }

        for (translationCode in toInstall) {
            logger.debug { "InstallCli downloading $translationCode" }
            val downloadResult = runBlocking {
                runCatching { am.download(DOWNLOADABLE_BIBLE_BASE_URL, "${translationCode}.zip") }
            }

            downloadResult.onFailure { error ->
                throw CliktError("Installing $translationCode failed: ${error.message}")
            }

            val installedNow = runCatching { am.downloadedTranslationCodes() }
                .getOrDefault(emptyList())
                .any { it == translationCode }
            if (!installedNow) {
                throw CliktError("Installing $translationCode failed: file was not found after download")
            }

            echo("Installed $translationCode")
        }
    }
}
