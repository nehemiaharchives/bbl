package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.app.CmpAnalyzerProvider
import org.gnit.bible.app.ComposeBibleResourcesReader
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.SearchTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class ComposeSearchTest() : SearchTestBase, ResourcesTestBase()  {
    override val analyzerProvider = CmpAnalyzerProvider()

    private fun createComposePlatform(): Platform {
        val platform = createTestPlatform().apply {
            overridePlatformPackDir = "${okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_composeapp_compose_bible_test_dir"}"
        }
        seedComposePackDirIfNeeded(platform)
        seedComposePackDirFromResources(platform)
        return platform
    }

    override var bible: Bible = Bible(
        assetManager = AssetManagerImpl(
            httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
            platform = createComposePlatform()
        )
    ).apply {
        bibleResourcesReader = ComposeBibleResourcesReader()
    }

    @Test
    fun searchCommonEmbeddedCmp(){
        super.searchCommonEmbedded()
    }

    @Test
    fun searchCommonDownloadedCmp(){
        super.searchCommonDownloaded()
    }

    @Test
    fun searchMorfologikCmp(){
        super.searchMorfologik()
    }

    @Test
    fun searchSmartcnCmp(){
        super.searchSmartcn()
    }

    @Test
    fun searchNoriCmp(){
        super.searchNori()
    }

    @Test
    fun searchKuromojiCmp(){
        super.searchKuromoji()
    }

    @Test
    fun searchExtraCmp(){
        super.searchExtra()
    }
}
