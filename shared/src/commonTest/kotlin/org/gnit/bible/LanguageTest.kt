package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageTest {

    @Test
    fun bookNamesTest(){
        Language.embeddedLanguages.forEach { language ->
            val bookNames = language.bookNames()
            assertEquals(66, bookNames.size, "Language ${language.code} should have 66 book names")
        }

        Language.downloadableLanguages.forEach { language ->
            val bookNames = language.bookNames()
            assertEquals(66, bookNames.size, "Language ${language.code} should have 66 book names")
        }
    }
}
