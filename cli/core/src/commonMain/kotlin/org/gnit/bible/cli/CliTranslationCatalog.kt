package org.gnit.bible.cli

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.InstallationState
import org.gnit.bible.Translation
import org.gnit.bible.TranslationEntry
import org.gnit.bible.Translation.Companion.downloadableTranslationCodeListCli
import org.gnit.bible.Translation.Companion.downloadableTranslationsCmp

object CliTranslationCatalog {
    private val logger = KotlinLogging.logger {}

    fun downloadableTranslations(bible: Bible): List<Translation> {
        val am = bible.assetManager
        return runBlocking {
            runCatching { am.downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL) }
                .onFailure { logger.debug { "Failed to download latest downloadable translation list, falling back to built-in translation catalog" } }
                .getOrDefault(downloadableTranslationsCmp)
        }
    }

    fun downloadableTranslationsByCode(bible: Bible): Map<String, Translation> {
        return (downloadableTranslations(bible) + downloadableTranslationsCmp)
            .associateBy { it.code }
    }

    fun downloadableTranslationCodes(): List<String> = downloadableTranslationCodeListCli

    fun downloadedTranslationEntries(bible: Bible): List<TranslationEntry> {
        return bible.assetManager.downloadedTranslations()
            .map { TranslationEntry(it, InstallationState.DOWNLOADED) }
    }

    fun downloadableTranslationEntries(bible: Bible): List<TranslationEntry> {
        val downloadableByCode = downloadableTranslationsByCode(bible)
        val downloadedCodes = bible.assetManager.downloadedTranslations()
            .map { it.code }
            .toSet()
        return downloadableByCode.values
            .filterNot { downloadedCodes.contains(it.code) }
            .map { TranslationEntry(it, InstallationState.DOWNLOADABLE) }
    }
}
