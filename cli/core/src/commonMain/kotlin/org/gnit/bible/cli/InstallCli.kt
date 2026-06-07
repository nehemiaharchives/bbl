package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.SearchModuleId
import org.gnit.bible.Translation
import okio.Path.Companion.toPath
import org.gnit.bible.BblVersion

class InstallCli(
    private val bible: Bible,
    private val packBaseUrl: String = environmentVariable("BBL_PACK_BASE_URL") ?: DOWNLOADABLE_BIBLE_BASE_URL,
    private val searchBinaryBaseUrl: String = environmentVariable("BBL_SEARCH_BINARY_BASE_URL")
        ?: BblVersion.releaseDownloadBaseUrl
) : CoreCliktCommand(name = "install") {
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

        val installedCodesBeforeValidation = runCatching { am.downloadedTranslationCodes() }
            .getOrDefault(emptyList())
            .toSet()
        val incompatibleInstalledCodes = requestedCodes
            .filter { it in installedCodesBeforeValidation }
            .filterNot { InstallCliSupport.isInstalledPackCompatible(bible, it) }
            .toSet()

        incompatibleInstalledCodes.forEach {
            echo("$it installed pack is incompatible with bbl ${BblVersion.artifactCompatibilityVersion}, reinstalling")
            runCatching { am.delete(it) }
        }

        val installedCodes = installedCodesBeforeValidation - incompatibleInstalledCodes
        val toInstall = requestedCodes.filterNot { it in installedCodes }
        if (toInstall.isEmpty()) {
            requestedCodes.forEach { echo("$it already installed, skipping download") }
            return
        }

        val downloadableByCode = CliTranslationCatalog.downloadableTranslationsByCode()
        val availableDownloadableCodes = downloadableByCode.keys
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
            val translation = downloadableByCode.getValue(translationCode)
            logger.debug { "InstallCli downloading $translationCode" }
            val downloadResult = runBlocking {
                runCatching { am.download(packBaseUrl, "${translationCode}.zip") }
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

            InstallCliSupport.validateInstalledPackVersionOrThrow(bible, translationCode)

            echo("Installed $translationCode")
            installSearchBinaryIfNeeded(translation)
        }
    }

    private fun installSearchBinaryIfNeeded(translation: Translation) {
        val moduleId = translation.language.searchModuleId
        if (moduleId == SearchModuleId.COMMON) return

        val am = bible.assetManager
        val binDir = CliBinaryPaths.binDir(am.platform.packDir)
        val binaryName = CliBinaryPaths.binaryName(moduleId, am.platform.name)
        val binaryPath = binDir / binaryName
        if (am.fileSystem.exists(binaryPath)) {
            echo("$binaryName already installed, skipping download")
            return
        }

        logger.debug { "InstallCli downloading search helper $binaryName" }
        val downloadResult = runBlocking {
            runCatching { am.downloadTo(searchBinaryBaseUrl, binaryName, binDir.toString()) }
        }

        downloadResult.onFailure { error ->
            throw CliktError("Installing $binaryName failed: ${error.message}")
        }

        if (!am.fileSystem.exists(binaryPath)) {
            throw CliktError("Installing $binaryName failed: file was not found after download")
        }
        markExecutable(binaryPath)
        echo("Installed $binaryName")
    }
}
