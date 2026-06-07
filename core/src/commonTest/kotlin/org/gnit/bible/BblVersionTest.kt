package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BblVersionTest {

    @Test
    fun bblVersionUsesBareNumericTag() {
        assertFalse(BblVersion.version.startsWith("v"))
    }

    @Test
    fun searchHelperVersionLineUsesVersion() {
        assertEquals(
            "bbl-search-kuromoji version ${BblVersion.version}",
            BblVersion.searchHelperVersionLine("bbl-search-kuromoji")
        )
    }

    @Test
    fun downloadablePackBaseUrlUsesPinnedServerResourcesTag() {
        assertEquals(
            BblVersion.serverResourceUrl("bblpacks"),
            DOWNLOADABLE_BIBLE_BASE_URL
        )
    }

    @Test
    fun searchHelperUrlUsesPinnedReleaseTag() {
        assertEquals(
            "https://github.com/${BblVersion.downloadRepository}/releases/download/${BblVersion.version}",
            BblVersion.releaseDownloadBaseUrl
        )
    }

    @Test
    fun packManifestVersionOrNullReturnsNullForInvalidJson() {
        assertEquals(null, BblVersion.packManifestVersionOrNull("not json"))
        assertEquals(null, BblVersion.packManifestVersionOrNull("{}"))
    }

    @Test
    fun packManifestVersionOrNullReadsVersion() {
        assertEquals("4.0.0", BblVersion.packManifestVersionOrNull("""{"version":"4.0.0"}"""))
    }

    @Test
    fun packManifestVersionOrNullReadsBblArtifactCompatibilityVersionForBackCompat() {
        assertEquals("4.0.0", BblVersion.packManifestVersionOrNull("""{"bblArtifactCompatibilityVersion":"4.0.0"}"""))
    }
}
