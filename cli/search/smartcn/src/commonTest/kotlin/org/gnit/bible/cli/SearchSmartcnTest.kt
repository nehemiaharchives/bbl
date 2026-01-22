package org.gnit.bible.cli

import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test

class SearchSmartcnTest: CliSearchTestBase(SmartcnAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        super.searchJesusChristSmartcn()
    }
}
