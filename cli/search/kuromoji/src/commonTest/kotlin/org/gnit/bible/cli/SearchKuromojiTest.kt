package org.gnit.bible.cli

import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchKuromojiTest: CliSearchTestBase(KuromojiAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist() {
        // Japanese (ja)
        val actualJc = bible.search(term = "イエス・キリスト", translation = Translation.jc).first()
        assertEquals(VersePointer(Translation.jc, 40, 1, 1), actualJc)
    }
}