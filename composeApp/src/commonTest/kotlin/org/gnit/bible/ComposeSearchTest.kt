package org.gnit.bible

import io.ktor.client.HttpClient
import okio.FileSystem
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.SearchTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class ComposeSearchTest() : SearchTestBase, ResourcesTestBase()  {

    private fun createComposePlatform(): Platform {
        val platform = createTestPlatform().apply {
            overridePlatformPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_composeapp_compose_bible_test_dir"}"
        }
        seedComposePackDirIfNeeded(platform)
        return platform
    }

    override var bible: Bible = Bible(
        assetManager = AssetManagerImpl(
            httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
            platform = createComposePlatform()
        ),
        analyzerProvider = CmpAnalyzerProvider()
    ).apply {
        bibleResourcesReader = ComposeBibleResourcesReader()
    }

    @Test
    fun searchJesusChristCommonEmbeddedCmp(){
        super.searchJesusChristCommonEmbedded()
    }

    @Test
    fun searchJesusChristCommonDownloadedCmp(){
        super.searchJesusChristCommonDownloaded()
    }

    @Test
    fun searchJesusChristMorfologikCmp(){
        super.searchJesusChristMorfologik()
    }

    @Test
    fun searchJesusChristSmartcnCmp(){
        super.searchJesusChristSmartcn()
    }

    @Test
    fun searchJesusChristNoriCmp(){
        super.searchJesusChristNori()
    }

    @Test
    fun searchJesusChristKuromojiCmp(){
        super.searchJesusChristKuromoji()
    }

    @Test
    fun searchJesusChristExtraCmp(){
        super.searchJesusChristExtra()
    }
}
