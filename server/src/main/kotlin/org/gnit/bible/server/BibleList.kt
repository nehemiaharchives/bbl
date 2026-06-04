package org.gnit.bible.server

import kotlinx.serialization.json.Json
import org.gnit.bible.Translation

object BibleList {
    fun getAllTranslations(): String {
        val allTranslations = Translation.embeddedTranslations + Translation.downloadableTranslationsCli
        val jsonString = Json { prettyPrint = true }.encodeToString(allTranslations)
        return jsonString
    }
}
