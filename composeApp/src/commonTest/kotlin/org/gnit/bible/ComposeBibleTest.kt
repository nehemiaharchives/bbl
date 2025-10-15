package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class ComposeBibleTest : BibleTest, ResourcesTestBase() {

    override val bible: Bible = Bible(assetManager = AssetManagerImpl(
        httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
        platform = createTestPlatform()
    )).apply {
        bibleTextReader = ComposeBibleTextReader()
    }

    @Test
    override fun testVerses() = super.testVerses()

    @Test
    override fun testDownloadedVerses() = super.testDownloadedVerses()
}
