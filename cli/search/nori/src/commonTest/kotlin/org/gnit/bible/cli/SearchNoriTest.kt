package org.gnit.bible.cli

import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test

class SearchNoriTest: CliSearchTestBase(NoriAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        super.searchJesusChristNori()
    }
}
