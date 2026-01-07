package org.gnit.bible.cli

import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchNoriTest: CliSearchTestBase(NoriAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        // Korean (ko)
        val actualKrv = bible.search(term = "예수 그리스도", translation = Translation.krv).first()
        assertEquals(VersePointer(Translation.krv, 40, 1, 1), actualKrv)
    }
}
