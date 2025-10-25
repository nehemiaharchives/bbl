package org.gnit.bible

import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranslationTest : ResourcesTestBase() {

    @Test
    fun embeddedTranslationCompanionObjectExistsForEachResource() {
        Bible.embeddedTranslationCodes.forEach { translationCode ->
            val translation = Translation.embeddedTranslations.find { it.code == translationCode }
            assertNotNull(translation) { "Embedded Translation companion object not found for code: $translationCode" }
        }
    }

    val webusJsonString = """
        {"code":"webus","languageCode":"en","englishName":"World English Bible","nativeName":"World English Bible","year":2000,"copyright":"Public Domain"}
        """.trimIndent()

    @Test
    fun encodeTranslationToJsonTest(){
        val actual = Translation.webus.toJson()
        assertEquals(webusJsonString, actual)
    }

    @Test
    fun decodeTranslationFromJsonTest(){
        val actual = Translation.fromJson(webusJsonString)
        assertEquals(Translation.webus, actual)
    }

    @Test
    fun encodeTranslationListToJsonTest(){
        val translationList = listOf(Translation.webus, Translation.kjv, Translation.rvr09)
        val actual = translationList.toJson()
        val expected = """
            [{"code":"webus","languageCode":"en","englishName":"World English Bible","nativeName":"World English Bible","year":2000,"copyright":"Public Domain"},
            {"code":"kjv","languageCode":"en","englishName":"King James Version","nativeName":"King James Version","year":1611,"copyright":"Public Domain"},
            {"code":"rvr09","languageCode":"es","englishName":"Reina-Valera","nativeName":"Reina-Valera","year":1909,"copyright":"Public Domain"}]
        """.trimIndent().replace("\n", "").replace(" ", "")
        assertEquals(expected, actual.replace("\n", "").replace(" ", ""))
    }
}
