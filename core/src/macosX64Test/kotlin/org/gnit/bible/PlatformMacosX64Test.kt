package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformMacosX64Test {
    @Test
    fun detectsMacosX64ReleaseTarget() {
        assertEquals(
            ReleaseTarget(ReleasePlatform.MACOS, ReleaseArchitecture.X64),
            getPlatform().releaseTarget
        )
    }
}
