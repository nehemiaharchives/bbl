package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.MockAssetManager
import kotlin.test.Test

class CliBibleTest : BibleTest {

    override val bible: Bible = Bible(assetManager = MockAssetManager()).apply {
        bibleTextReader = CliBibleTextReader()
    }

    @Test
    override fun testVerses() = super.testVerses()
}
