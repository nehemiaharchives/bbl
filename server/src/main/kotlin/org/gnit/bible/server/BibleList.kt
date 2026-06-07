package org.gnit.bible.server

import org.gnit.bible.SupportedTranslation

import kotlinx.serialization.json.Json
import org.gnit.bible.Translation

object BibleList {
    fun getAllTranslations(): String {
        val allTranslations = SupportedTranslation.embeddedTranslations + SupportedTranslation.downloadableTranslations
        val jsonString = Json { prettyPrint = true }.encodeToString(allTranslations)
        return jsonString
    }
}
