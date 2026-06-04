package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BblVersionTest {

    @Test
    fun bblCliVersionUsesBareNumericTag() {
        assertFalse(BblVersion.cliVersion.startsWith("v"))
    }

    @Test
    fun bblArtifactCompatibilityVersionUsesBareNumericTag() {
        assertFalse(BblVersion.artifactCompatibilityVersion.startsWith("v"))
    }

    @Test
    fun searchHelperVersionLineUsesCliVersion() {
        assertEquals(
            "bbl-search-kuromoji version ${BblVersion.cliVersion}",
            BblVersion.searchHelperVersionLine("bbl-search-kuromoji")
        )
    }

    @Test
    fun searchHelperArtifactCompatibilityVersionLineIsMachineReadable() {
        assertEquals(
            BblVersion.artifactCompatibilityVersion,
            BblVersion.artifactCompatibilityVersionLine()
        )
    }

    @Test
    fun downloadablePackUrlsUsePinnedServerResourcesTag() {
        assertEquals(
            BblVersion.serverResourceUrl("bbllist.json"),
            DOWNLOADABLE_BIBLE_LIST_URL
        )
        assertEquals(
            BblVersion.serverResourceUrl("bblpacks"),
            DOWNLOADABLE_BIBLE_BASE_URL
        )
    }

    @Test
    fun searchHelperUrlUsesPinnedReleaseTag() {
        assertEquals(
            "https://github.com/${BblVersion.downloadRepository}/releases/download/${BblVersion.cliVersion}",
            BblVersion.releaseDownloadBaseUrl
        )
    }
}
