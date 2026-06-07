package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.InstallationState
import org.gnit.bible.Translation
import org.gnit.bible.TranslationEntry
import org.gnit.bible.Translation.Companion.downloadableTranslationCodeListCli
import org.gnit.bible.Translation.Companion.downloadableTranslationsCmp

object CliTranslationCatalog {

    fun downloadableTranslationsByCode(): Map<String, Translation> =
        downloadableTranslationsCmp.associateBy { it.code }

    fun downloadableTranslationCodes(): List<String> = downloadableTranslationCodeListCli

    fun downloadedTranslationEntries(bible: Bible): List<TranslationEntry> {
        return bible.assetManager.downloadedTranslations()
            .map { TranslationEntry(it, InstallationState.DOWNLOADED) }
    }

    fun downloadableTranslationEntries(bible: Bible): List<TranslationEntry> {
        val downloadedCodes = bible.assetManager.downloadedTranslations()
            .map { it.code }
            .toSet()
        return downloadableTranslationsCmp
            .filterNot { downloadedCodes.contains(it.code) }
            .map { TranslationEntry(it, InstallationState.DOWNLOADABLE) }
    }
}
