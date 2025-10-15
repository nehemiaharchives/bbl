package org.gnit.bible.cli

import io.ktor.client.HttpClient
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.getPlatform
import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class CliBibleTest : BibleTest {

    override val bible: Bible = Bible(assetManager = AssetManagerImpl(
        httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
        platform = getPlatform()
    )).apply {
        bibleTextReader = CliBibleTextReader()
    }

    @Test
    override fun testVerses() = super.testVerses()

    @Test
    override fun testDownloadedVerses() = super.testDownloadedVerses()
}
