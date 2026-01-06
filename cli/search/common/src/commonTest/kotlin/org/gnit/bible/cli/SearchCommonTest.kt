package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class SearchCommonTest {

    @Test
    fun commonHelperHasWorkingTestFixtureConstants() {
        // Keep this module's tests independent of other modules' test source sets.
        // TestFixtures lives in :test-framework (a normal dependency of commonTest).
        assertTrue(org.gnit.bible.test.TestFixtures.WEBUS_GENESIS_1_1.contains("In the beginning"))
    }
}