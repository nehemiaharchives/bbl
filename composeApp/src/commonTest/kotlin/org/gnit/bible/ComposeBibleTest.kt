package org.gnit.bible

import io.ktor.client.HttpClient
import okio.FileSystem
import org.gnit.bible.test.BibleTestBase
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class ComposeBibleTest : BibleTestBase, ResourcesTestBase() {

    private fun createComposePlatform(): Platform {
        val platform = createTestPlatform().apply {
            overridePlatformPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_composeapp_compose_bible_test_dir"}"
        }
        seedComposePackDirIfNeeded(platform)
        return platform
    }

    override val bible: Bible = Bible(
        assetManager = AssetManagerImpl(
            httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
            platform = createComposePlatform()
        ),
        analyzerProvider = CmpAnalyzerProvider()
    ).apply {
        bibleResourcesReader = ComposeBibleResourcesReader()
    }

    @Test
    override fun testVerses() = super.testVerses()

    @Test
    override fun testDownloadedVerses() = super.testDownloadedVerses()

    @Test
    override fun testListIndexFiles() = super.testListIndexFiles()

    @Test
    override fun testReadIndexFile() = super.testReadIndexFile()

    /*@Test
    override fun searchJesusChrist() = super.searchJesusChrist()

    @Test
    override fun searchJesusChristInWebusInRomans() = super.searchJesusChristInWebusInRomans()

    @Test
    override fun searchJesusChristInWebusInRomans2() = super.searchJesusChristInWebusInRomans2()

    @Test
    override fun searchJesusChristInWebusInRomans3To5() = super.searchJesusChristInWebusInRomans3To5()

    @Test
    override fun searchJesusChristInKjv() = super.searchJesusChristInKjv()*/
}
