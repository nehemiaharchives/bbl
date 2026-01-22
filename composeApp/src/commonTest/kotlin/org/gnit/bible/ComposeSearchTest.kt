package org.gnit.bible

import io.ktor.client.HttpClient
import okio.FileSystem
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.SearchTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class ComposeSearchTest() : SearchTestBase, ResourcesTestBase()  {

    override var bible: Bible = Bible(
        assetManager = AssetManagerImpl(
            httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
            platform = createTestPlatform().apply { overridePlatformPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_composeapp_compose_bible_test_dir"}" }
        ),
        analyzerProvider = CmpAnalyzerProvider()
    ).apply {
        bibleResourcesReader = ComposeBibleResourcesReader()
    }

    @Test
    fun searchJesusChristEmbedded(){
        super.searchJesusChristCommonEmbedded()
    }

    @Test
    fun searchJesusChristDownloaded(){
        super.searchJesusChristCommonDownloaded()
    }
}
