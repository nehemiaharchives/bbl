package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformMacosArm64Test {
    @Test
    fun detectsMacosArm64ReleaseTarget() {
        assertEquals(
            ReleaseTarget(ReleasePlatform.MACOS, ReleaseArchitecture.ARM64),
            getPlatform().releaseTarget
        )
    }
}
