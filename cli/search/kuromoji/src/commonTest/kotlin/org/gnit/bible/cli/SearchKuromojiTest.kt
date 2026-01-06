package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchKuromojiTest {

    @Test
    fun kuromojiHelperHasWorkingTestFixtureConstants() {
        // Avoid depending on other modules' test source sets.
        // TestFixtures lives in :test-framework which this module already depends on for commonTest.
        assertTrue(org.gnit.bible.test.TestFixtures.JC_GENESIS_1_1.isNotBlank())
    }
}