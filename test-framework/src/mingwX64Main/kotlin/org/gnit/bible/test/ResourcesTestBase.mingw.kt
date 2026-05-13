package org.gnit.bible.test

import org.gnit.bible.Platform
import org.gnit.bible.getPlatform

actual abstract class ResourcesTestBase actual constructor() {
    actual fun createTestPlatform(): Platform {
        return getPlatform()
    }

    actual fun seedComposePackDirIfNeeded(platform: Platform) {
        // No-op on Windows native.
    }
}
