package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.ko.ct.BibleKoreanAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchNoriTest: CliSearchTestBase(NoriAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchHelperCli(Bible(), NoriAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ko) is BibleKoreanAnalyzer)
    }

    @Test
    fun searchNoriCli() {
        super.searchNori()
    }
}
