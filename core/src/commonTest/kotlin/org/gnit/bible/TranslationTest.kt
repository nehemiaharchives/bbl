package org.gnit.bible

import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranslationTest : ResourcesTestBase() {

    @Test
    fun embeddedSupportedTranslationExistsForEachResource() {
        SupportedTranslation.embeddedCodes.forEach { translationCode ->
            val translation = SupportedTranslation.embeddedTranslations.find { it.code == translationCode }
            assertNotNull(translation) { "Embedded translation not found for code: $translationCode" }
        }
    }

    @Test
    fun supportedTranslationLookupReturnsKnownTranslation() {
        assertEquals(SupportedTranslation.WEBUS, SupportedTranslation.byCode["webus"])
        assertEquals(SupportedTranslation.JC, SupportedTranslation.byCode["jc"])
    }

    val webusJsonString = """
        {"code":"webus","languageCode":"en","englishName":"World English Bible","nativeName":"World English Bible","year":2000,"copyright":"Public Domain","version":"${BblVersion.VERSION}"}
        """.trimIndent()

    @Test
    fun encodeTranslationToJsonTest(){
        val actual = SupportedTranslation.WEBUS.translation.toJson()
        assertEquals(webusJsonString, actual)
    }

    @Test
    fun decodeTranslationFromJsonTest(){
        val actual = Translation.fromJson(webusJsonString)
        assertEquals(SupportedTranslation.WEBUS.translation, actual)
    }

    @Test
    fun translationDefaultsToCurrentBblVersion() {
        assertEquals(BblVersion.VERSION, SupportedTranslation.WEBUS.translation.version)
        assertTrue(SupportedTranslation.WEBUS.translation.toJson().contains("\"version\":\"${BblVersion.VERSION}\""))
    }

    @Test
    fun encodeTranslationListToJsonTest(){
        val translationList = listOf(SupportedTranslation.WEBUS.translation, SupportedTranslation.KJV.translation, SupportedTranslation.RVR09.translation)
        val actual = translationList.toJson()
        val expected = """
            [{"code":"webus","languageCode":"en","englishName":"World English Bible","nativeName":"World English Bible","year":2000,"copyright":"Public Domain","version":"${BblVersion.VERSION}"},
            {"code":"kjv","languageCode":"en","englishName":"King James Version","nativeName":"King James Version","year":1611,"copyright":"Public Domain","version":"${BblVersion.VERSION}"},
            {"code":"rvr09","languageCode":"es","englishName":"Reina-Valera","nativeName":"Reina-Valera","year":1909,"copyright":"Public Domain","version":"${BblVersion.VERSION}"}]
        """.trimIndent().replace("\n", "").replace(" ", "")
        assertEquals(expected, actual.replace("\n", "").replace(" ", ""))
    }
}
