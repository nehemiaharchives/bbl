package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.test.CliSearchTestBase
import org.gnit.lucenekmp.analysis.ja.ct.BibleJapaneseAnalyzer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchKuromojiTest: CliSearchTestBase(KuromojiAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun testContainsAnalyzers() {
        val helper = SearchKuromoji(Bible(), KuromojiAnalyzerProvider())
        assertTrue(helper.analyzerProvider.analyzerFor(Language.ja) is BibleJapaneseAnalyzer)
    }

    @Test
    fun searchKuromojiCli() {
        super.searchKuromoji()
    }
}
