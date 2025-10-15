package org.gnit.bible

import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.MockAssetManager
import kotlin.test.Test

class ComposeBibleTest : BibleTest, ResourcesTestBase() {

    override val bible: Bible = Bible(assetManager = MockAssetManager()).apply {
        bibleTextReader = ComposeBibleTextReader()
    }

    @Test
    override fun testVerses() = super.testVerses()
}
