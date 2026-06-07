package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.mr.ct.BibleMarathiAnalyzer
import org.gnit.lucenekmp.analysis.tl.ct.BibleTagalogAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.ct.BibleVietnameseAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchExtraTest: CliSearchTestBase(ExtraAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchExtra(Bible(), ExtraAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.tl) is BibleTagalogAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.vi) is BibleVietnameseAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.gu) is GujaratiAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.mr) is BibleMarathiAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ur) is UrduAnalyzer)
    }

    @Test
    fun searchExtraCli() {
        super.searchExtra()
    }
}
