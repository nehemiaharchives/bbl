package org.gnit.bible.cli

import org.gnit.bible.CommonAnalyzerProvider
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test

class SearchCommonTest: CliSearchTestBase(CommonAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist(){
        super.searchJesusChristCommonEmbedded()

        super.searchJesusChristCommonDownloaded()
    }
}
