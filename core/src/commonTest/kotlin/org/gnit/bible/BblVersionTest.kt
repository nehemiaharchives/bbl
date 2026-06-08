package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BblVersionTest {

    @Test
    fun bblVersionUsesBareNumericTag() {
        assertFalse(BblVersion.VERSION.startsWith("v"))
    }

    @Test
    fun searchHelperUrlUsesPinnedReleaseTag() {
        assertEquals(
            "https://github.com/${BblVersion.BBL_REPOSITORY}/releases/download/${BblVersion.VERSION}",
            BblVersion.RELEASE_DOWNLOAD_URL
        )
    }

}
