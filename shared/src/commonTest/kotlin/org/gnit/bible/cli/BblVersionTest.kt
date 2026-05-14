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
    fun downloadablePackUrlsUsePinnedServerResourcesTag() {
        assertEquals(
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/$bblCliVersion/server/src/main/resources/files/bbllist.json",
            DOWNLOADABLE_BIBLE_LIST_URL
        )
        assertEquals(
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/$bblCliVersion/server/src/main/resources/files/bblpacks",
            DOWNLOADABLE_BIBLE_BASE_URL
        )
    }

    @Test
    fun searchHelperUrlUsesPinnedReleaseTag() {
        assertEquals(
            "https://github.com/nehemiaharchives/bbl-kmp/releases/download/$bblCliVersion",
            bblReleaseDownloadBaseUrl
        )
    }
}
