package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformReleaseTargetTest {
    @Test
    fun detectsSupportedReleaseTargets() {
        assertEquals(
            ReleaseTarget(ReleasePlatform.LINUX, ReleaseArchitecture.X64),
            detectReleaseTarget("Linux", "x86_64")
        )
        assertEquals(
            ReleaseTarget(ReleasePlatform.MACOS, ReleaseArchitecture.ARM64),
            detectReleaseTarget("Mac OS X", "aarch64")
        )
        assertEquals(
            ReleaseTarget(ReleasePlatform.MACOS, ReleaseArchitecture.X64),
            detectReleaseTarget("macOS", "amd64")
        )
        assertEquals(
            ReleaseTarget(ReleasePlatform.WINDOWS, ReleaseArchitecture.X64),
            detectReleaseTarget("Windows 11", "x64")
        )
    }

    @Test
    fun returnsNullForUnsupportedTargets() {
        assertNull(detectReleaseTarget("Linux", "armv7"))
        assertNull(detectReleaseTarget("Android", "arm64"))
    }

    @Test
    fun buildsPublishedReleaseAssetNames() {
        assertEquals(
            "bbl-search-common-linux-x64",
            ReleaseTarget(ReleasePlatform.LINUX, ReleaseArchitecture.X64)
                .assetName("bbl-search-common")
        )
        assertEquals(
            "bbl-search-common-macos-arm64",
            ReleaseTarget(ReleasePlatform.MACOS, ReleaseArchitecture.ARM64)
                .assetName("bbl-search-common")
        )
        assertEquals(
            "bbl-search-common-windows-x64.exe",
            ReleaseTarget(ReleasePlatform.WINDOWS, ReleaseArchitecture.X64)
                .assetName("bbl-search-common")
        )
    }
}
