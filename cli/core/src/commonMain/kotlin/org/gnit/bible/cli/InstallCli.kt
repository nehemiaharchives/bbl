package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gnit.bible.Bible
import org.gnit.bible.SearchModuleId
import org.gnit.bible.SupportedTranslation
import org.gnit.bible.Translation
import org.gnit.bible.BblVersion
import org.gnit.bible.ZipBibleResourcesReader

class InstallCli(
    private val bible: Bible,
    private val releaseDownloadUrl: String = environmentVariable("BBL_RELEASE_DOWNLOAD_URL")
        ?: BblVersion.RELEASE_DOWNLOAD_URL
) : CoreCliktCommand(name = "install") {
    private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Download and install one or more translations by code (e.g. bbl install kjv rvr09, ref: bbl list translation, alias: get|pull)"
    }

    private val targets: List<String> by argument(help = "translation code(s) to download and install (e.g., kjv rvr09)")
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
            .filterNot { isInstalledPackCompatible(bible, it) }
            .toSet()

        incompatibleInstalledCodes.forEach {
            echo("$it installed pack is incompatible with bbl ${BblVersion.VERSION}, reinstalling")
            runCatching { am.delete(it) }
        }

        val installedCodes = installedCodesBeforeValidation - incompatibleInstalledCodes
        val toInstall = requestedCodes.filterNot { it in installedCodes }
        if (toInstall.isEmpty()) {
            requestedCodes.forEach { echo("$it already installed, skipping download") }
            BblHistory.record(bible, BblHistory.command("bbl install", requestedCodes.joinToString(" ")))
            return
        }

        val downloadableByCode = SupportedTranslation.byCode.mapValues { it.value.translation }
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
                runCatching { am.download(releaseDownloadUrl, "${translationCode}.zip") }
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

            validateInstalledPackVersionOrThrow(bible, translationCode)

            echo("Installed $translationCode")
            installSearchBinaryIfNeeded(translation)
        }
        BblHistory.record(bible, BblHistory.command("bbl install", requestedCodes.joinToString(" ")))
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
            runCatching { am.downloadTo(releaseDownloadUrl, binaryName, binDir.toString()) }
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

    private fun installedPackManifestJsonOrNull(bible: Bible, translationCode: String): String? {
        val am = bible.assetManager
        return runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrNull()
    }

    private fun installedPackVersion(manifestJson: String): String {
        val jsonElement = Json.parseToJsonElement(manifestJson)
        return jsonElement.jsonObject["version"]?.jsonPrimitive?.content
            ?: error("Pack manifest is missing version")
    }

    fun isInstalledPackCompatible(bible: Bible, translationCode: String): Boolean {
        val manifestJson = installedPackManifestJsonOrNull(bible, translationCode) ?: return false
        return installedPackVersion(manifestJson) == BblVersion.VERSION
    }

    fun validateInstalledPackVersionOrThrow(bible: Bible, translationCode: String) {
        val am = bible.assetManager
        val manifestJson = runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrElse { error ->
            runCatching { am.delete(translationCode) }
            throw CliktError("Installing $translationCode failed: unable to read pack manifest: ${error.message}")
        }

        val packVersion = installedPackVersion(manifestJson)
        if (packVersion != BblVersion.VERSION) {
            runCatching { am.delete(translationCode) }
            throw CliktError(
                "Installing $translationCode failed: pack manifest version $packVersion is incompatible with bbl ${BblVersion.VERSION}"
            )
        }
    }
}
