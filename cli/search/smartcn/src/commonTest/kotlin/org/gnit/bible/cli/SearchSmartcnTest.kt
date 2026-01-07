package org.gnit.bible.cli

import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchSmartcnTest: CliSearchTestBase(SmartcnAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        // Chinese (zh)
        val actualCunp = bible.search(term = "耶稣基督", translation = Translation.cunp).first()
        assertEquals(VersePointer(Translation.cunp, 40, 1, 1), actualCunp)
    }
}
