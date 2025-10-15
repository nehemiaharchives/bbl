package org.gnit.bible.test

import org.gnit.bible.Platform
import org.gnit.bible.getPlatform

actual abstract class ResourcesTestBase {
    actual fun createTestPlatform(): Platform {
        return getPlatform()
    }
}
