package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
//import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.bible.Bible
import org.gnit.bible.SearchModuleId
import org.gnit.bible.Translation
import org.gnit.bible.Translation.Companion.downloadableTranslationsCmp
import okio.Path.Companion.toPath

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
        val downloadableByCode = downloadableTranslationsCmp.associateBy { it.code }

        for (translationCode in requestedCodes) {
            if (translationCode !in downloaded) {
                echo("Translation $translationCode was not in the list of downloaded translations")
                continue
            }

            val translation = downloadableByCode[translationCode]
            runCatching { am.delete(translationCode) }
                .onSuccess {
                    downloaded.remove(translationCode)
                    echo("Uninstalled $translationCode")
                    uninstallSearchBinaryIfUnused(translation, downloaded, downloadableByCode)
                }
                .onFailure {
                    echo("Uninstalling $translationCode failed")
                }
        }
    }

    private fun uninstallSearchBinaryIfUnused(
        removedTranslation: Translation?,
        remainingDownloadedCodes: Set<String>,
        downloadableByCode: Map<String, Translation>
    ) {
        val moduleId = removedTranslation?.language?.searchModuleId ?: return
        if (moduleId == SearchModuleId.COMMON) return

        val stillNeeded = remainingDownloadedCodes.any { code ->
            downloadableByCode[code]?.language?.searchModuleId == moduleId
        }
        if (stillNeeded) return

        val am = bible.assetManager
        val binaryName = "bbl-search-${moduleId.name.lowercase()}${if (am.platform.name == "Windows") ".exe" else ""}"
        val binaryPath = am.platform.packDir.toPath().parent!! / "bin" / binaryName
        if (!am.fileSystem.exists(binaryPath)) return

        runCatching { am.fileSystem.delete(binaryPath) }
            .onSuccess { echo("Uninstalled $binaryName") }
            .onFailure { echo("Uninstalling $binaryName failed") }
    }
}
