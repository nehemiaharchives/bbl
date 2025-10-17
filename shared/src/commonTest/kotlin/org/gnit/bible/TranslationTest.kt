package org.gnit.bible

import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.Test
import kotlin.test.assertNotNull

class TranslationTest : ResourcesTestBase() {

    @Test
    fun embeddedTranslationCompanionObjectExistsForEachResource() {
        Bible.embeddedTranslations.forEach { translationCode ->
            val translation = Translation.embeddedTranslations.find { it.code == translationCode }
            assertNotNull(translation) { "Embedded Translation companion object not found for code: $translationCode" }
        }
    }
}
