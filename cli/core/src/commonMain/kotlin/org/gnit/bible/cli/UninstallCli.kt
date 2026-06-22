package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.gnit.bible.Bible
import org.gnit.bible.SearchModuleId
import org.gnit.bible.Translation

class UninstallCli(
    private val bible: Bible
) : CoreCliktCommand(name = "uninstall") {
    override fun help(context: Context): String {
        return "Delete one or more downloaded bible translation packs from your computer (alias: rm|remove|del|delete)"
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
        val downloadedByCode = runCatching { am.downloadedTranslations().associateBy { it.code } }
            .getOrDefault(emptyMap())

        for (translationCode in requestedCodes) {
            if (translationCode !in downloaded) {
                echo("Translation $translationCode was not in the list of downloaded translations")
                continue
            }

            runCatching { am.delete(translationCode) }
                .onSuccess {
                    downloaded.remove(translationCode)
                    echo("Uninstalled $translationCode")
                    uninstallSearchBinaryIfUnused(downloadedByCode[translationCode], downloaded, downloadedByCode)
                }
                .onFailure {
                    echo("Uninstalling $translationCode failed")
                }
        }
        BblHistory.record(bible, BblHistory.command("bbl uninstall", requestedCodes.joinToString(" ")))
    }

    private fun uninstallSearchBinaryIfUnused(
        removedTranslation: Translation?,
        remainingDownloadedCodes: Set<String>,
        downloadedByCode: Map<String, Translation>,
    ) {
        val moduleId = removedTranslation?.language?.searchModuleId ?: return

        val stillNeeded = remainingDownloadedCodes.any { code ->
            downloadedByCode[code]?.language?.searchModuleId == moduleId
        }
        if (stillNeeded) return

        val am = bible.assetManager
        val binaryName = CliBinaryPaths.binaryName(moduleId, am.platform.name)
        val binaryPath = CliBinaryPaths.binDir(am.platform.packDir) / binaryName
        if (!am.fileSystem.exists(binaryPath)) return

        runCatching { am.fileSystem.delete(binaryPath) }
            .onSuccess { echo("Uninstalled $binaryName") }
            .onFailure { echo("Uninstalling $binaryName failed") }
    }
}
