package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.bn.ct.BibleBengaliAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.ct.BibleEnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.ct.BibleSpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.hi.ct.BibleHindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.ct.BiblePortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.ct.BibleRussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.ct.BibleTamilAnalyzer
import org.gnit.lucenekmp.analysis.te.ct.BibleTeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchCommonTest: CliSearchTestBase(CommonAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchHelperCli(Bible(), CommonAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.en) is BibleEnglishAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.es) is BibleSpanishAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.pt) is BiblePortugueseAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.de) is BibleGermanAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.fr) is FrenchAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ru) is BibleRussianAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.nl) is DutchAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.it) is ItalianAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.sv) is BibleSwedishAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.id) is IndonesianAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.th) is ThaiAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.hi) is BibleHindiAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.bn) is BibleBengaliAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ta) is BibleTamilAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.te) is BibleTeluguAnalyzer)
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ne) is BibleNepaliAnalyzer)
    }

    @Test
    fun searchCommonCli(){
        super.searchCommonEmbedded()

        super.searchCommonDownloaded()
    }
}
