package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchSmartcnTest {

    @Test
    fun smartcnHelperHasWorkingTestFixtureConstants() {
        // Avoid depending on other modules' test source sets.
        // TestFixtures lives in :test-framework which this module already depends on for commonTest.
        assertTrue(org.gnit.bible.test.TestFixtures.WEBUS_JOHN_3_16.isNotBlank())
    }
}