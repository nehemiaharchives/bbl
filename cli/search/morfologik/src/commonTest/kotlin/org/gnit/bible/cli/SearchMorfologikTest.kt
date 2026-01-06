package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchMorfologikTest {

    @Test
    fun morfologikHelperHasWorkingTestFixtureConstants() {
        // Avoid depending on other modules' test source sets.
        // TestFixtures lives in :test-framework which this module already depends on for commonTest.
        assertTrue(org.gnit.bible.test.TestFixtures.WEBUS_GENESIS_1_1.contains("In the beginning"))
    }
}