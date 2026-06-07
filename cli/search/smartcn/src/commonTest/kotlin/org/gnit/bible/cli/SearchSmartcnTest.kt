package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchSmartcnTest: CliSearchTestBase(SmartcnAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchSmartcn(Bible(), SmartcnAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.zh) is SmartChineseAnalyzer)
    }

    @Test
    fun searchSmartcnCli() {
        super.searchSmartcn()
    }
}
