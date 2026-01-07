package org.gnit.bible.cli

import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchMorfologikTest: CliSearchTestBase(MorfologikAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        // Polish (pl)
        val actualUbg = bible.search(term = "Jezusa Chrystusa", translation = Translation.ubg).first()
        assertEquals(VersePointer(Translation.ubg, 40, 1, 1), actualUbg)

        // Ukrainian (uk)
        val actualUbio = bible.search(term = "Ісуса Христа", translation = Translation.ubio).first()
        assertEquals(VersePointer(Translation.ubio, 40, 1, 1), actualUbio)
    }
}