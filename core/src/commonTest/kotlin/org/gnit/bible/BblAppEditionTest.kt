package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BblAppEditionTest {

    @Test
    fun allEditionIdsAreUnique() {
        val ids = SupportedTranslation.allAppEditions.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun allEmbeddedCodesExistInSupportedTranslationCatalog() {
        val knownCodes = SupportedTranslation.byCode.keys
        SupportedTranslation.allAppEditions.forEach { edition ->
            assertTrue(
                edition.embeddedCodes.all { it in knownCodes },
                "Unknown embedded code in ${edition.id}: ${edition.embeddedCodes - knownCodes}"
            )
        }
    }

    @Test
    fun editionCountsMatchCatalogRules() {
        val translationCount = SupportedTranslation.all.size
        val englishNonEnglishKjvPairsExcluded = SupportedTranslation.all.count {
            it.code != "webus" && it.code != "kjv"
        }
        val expectedPairCount = (translationCount * (translationCount - 1) / 2) - englishNonEnglishKjvPairsExcluded

        assertEquals(translationCount, SupportedTranslation.allAppEditions.count { it.kind == BblAppEdition.Kind.single })
        assertEquals(expectedPairCount, SupportedTranslation.allAppEditions.count { it.kind == BblAppEdition.Kind.pair })
        assertEquals(4, SupportedTranslation.allAppEditions.count { it.kind == BblAppEdition.Kind.regional })
    }

    @Test
    fun regionalEditionsContainRequiredCodes() {
        assertEquals(
            setOf("webus", "kjv", "rvr09", "tb", "delut", "lsg", "sinod", "svrj", "rdv24", "ubg", "ubio", "sven"),
            SupportedTranslation.appEditionById("western").embeddedCodes
        )
        assertEquals(
            setOf("webus", "kjv", "cunp", "krv", "jc"),
            SupportedTranslation.appEditionById("east-asia").embeddedCodes
        )
        assertEquals(
            setOf("webus", "kjv", "ayt", "th1971", "abtag", "kttv"),
            SupportedTranslation.appEditionById("sea").embeddedCodes
        )
        assertEquals(
            setOf("webus", "kjv", "irvhin", "irvben", "irvtam", "irvguj", "irvmar", "irvtel", "irvurd", "npiulb"),
            SupportedTranslation.appEditionById("south-asia").embeddedCodes
        )
    }

    @Test
    fun rdv24ExistsAndMisspelledRvd24DoesNot() {
        assertTrue("rdv24" in SupportedTranslation.byCode)
        assertFalse("rvd24" in SupportedTranslation.byCode)
    }

    @Test
    fun kjvIsNotUsedForEnglishNonEnglishPairs() {
        assertTrue(SupportedTranslation.allAppEditions.any { it.id == "webus-jc" })
        assertFalse(SupportedTranslation.allAppEditions.any { it.id == "kjv-jc" })
        assertFalse(SupportedTranslation.allAppEditions.any { it.id == "kjv-irvhin" })
    }
}
