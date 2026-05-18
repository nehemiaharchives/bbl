package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL

class BblVersionTest {

    @Test
    fun bblCliVersionUsesBareNumericTag() {
        assertFalse(bblCliVersion.startsWith("v"))
    }

    @Test
    fun bblArtifactCompatibilityVersionUsesBareNumericTag() {
        assertFalse(bblArtifactCompatibilityVersion.startsWith("v"))
    }

    @Test
    fun searchHelperVersionLineUsesCliVersion() {
        assertEquals(
            "bbl-search-kuromoji version $bblCliVersion",
            bblSearchHelperVersionLine("bbl-search-kuromoji")
        )
    }

    @Test
    fun searchHelperArtifactCompatibilityVersionLineIsMachineReadable() {
        assertEquals(
            bblArtifactCompatibilityVersion,
            bblSearchHelperArtifactCompatibilityVersionLine()
        )
    }

    @Test
    fun downloadablePackUrlsUsePinnedServerResourcesTag() {
        assertEquals(
            "https://raw.githubusercontent.com/$bblDownloadRepository/$bblArtifactCompatibilityVersion/server/src/main/resources/files/bbllist.json",
            DOWNLOADABLE_BIBLE_LIST_URL
        )
        assertEquals(
            "https://raw.githubusercontent.com/$bblDownloadRepository/$bblArtifactCompatibilityVersion/server/src/main/resources/files/bblpacks",
            DOWNLOADABLE_BIBLE_BASE_URL
        )
    }

    @Test
    fun searchHelperUrlUsesPinnedReleaseTag() {
        assertEquals(
            "https://github.com/$bblDownloadRepository/releases/download/$bblArtifactCompatibilityVersion",
            bblReleaseDownloadBaseUrl
        )
    }
}
