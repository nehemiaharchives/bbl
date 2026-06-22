package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformMingwX64Test {
    @Test
    fun detectsWindowsX64ReleaseTarget() {
        assertEquals(
            ReleaseTarget(ReleasePlatform.WINDOWS, ReleaseArchitecture.X64),
            getPlatform().releaseTarget
        )
    }
}
