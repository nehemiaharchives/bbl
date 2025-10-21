package org.gnit.bible

import kotlinx.serialization.json.Json
import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranslationTest : ResourcesTestBase() {

    @Test
    fun embeddedTranslationCompanionObjectExistsForEachResource() {
        Bible.embeddedTranslations.forEach { translationCode ->
            val translation = Translation.embeddedTranslations.find { it.code == translationCode }
            assertNotNull(translation) { "Embedded Translation companion object not found for code: $translationCode" }
        }
    }

    val webusJsonString = """
        {"code":"webus","languageCode":"en","englishName":"World English Bible","nativeName":"World English Bible","year":2000,"copyright":"Public Domain"}
        """.trimIndent()

    @Test
    fun encodeTranslationToJsonTest(){
        val actual = Json.encodeToString(Translation.webus)
        assertEquals(webusJsonString, actual)
    }

    @Test
    fun decodeTranslationFromJsonTest(){
        val actual = Json.decodeFromString<Translation>(webusJsonString)
        assertEquals(Translation.webus, actual)
    }
}
