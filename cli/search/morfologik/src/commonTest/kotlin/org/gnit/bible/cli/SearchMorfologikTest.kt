package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.morfologik.MorfologikAnalyzer
import org.gnit.lucenekmp.analysis.uk.ct.BibleUkrainianAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchMorfologikTest: CliSearchTestBase(MorfologikAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchMorfologik(Bible(), MorfologikAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.pl) is MorfologikAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.uk) is BibleUkrainianAnalyzer)
    }

    @Test
    fun searchMorfologikCli() {
        super.searchMorfologik()
    }
}
