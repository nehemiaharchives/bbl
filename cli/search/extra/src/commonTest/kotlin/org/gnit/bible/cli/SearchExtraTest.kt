package org.gnit.bible.cli

import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test

class SearchExtraTest: CliSearchTestBase(ExtraAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchExtraCli() {
        super.searchExtra()
    }
}
