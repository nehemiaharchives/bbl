package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchExtraTest {

    @Test
    fun extraHelperHasWorkingTestFixtureConstants() {
        // This ensures this module can at least link against the shared test framework.
        assertTrue(org.gnit.bible.test.TestFixtures.JC_GENESIS_1_1.isNotBlank())
    }
}