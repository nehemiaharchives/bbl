package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformLinuxX64Test {
    @Test
    fun detectsLinuxX64ReleaseTarget() {
        assertEquals(
            ReleaseTarget(ReleasePlatform.LINUX, ReleaseArchitecture.X64),
            getPlatform().releaseTarget
        )
    }
}
