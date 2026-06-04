package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.gnit.bible.Bible
import org.gnit.bible.SearchModuleId

class UninstallCli(
    private val bible: Bible
) : CoreCliktCommand(name = "uninstall") {
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
        val downloadableByCode = CliTranslationCatalog.downloadableTranslationsByCode(bible)

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
        removedTranslation: org.gnit.bible.Translation?,
        remainingDownloadedCodes: Set<String>,
        downloadableByCode: Map<String, org.gnit.bible.Translation>
    ) {
        val moduleId = removedTranslation?.language?.searchModuleId ?: return
        if (moduleId == SearchModuleId.COMMON) return

        val stillNeeded = remainingDownloadedCodes.any { code ->
            downloadableByCode[code]?.language?.searchModuleId == moduleId
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
