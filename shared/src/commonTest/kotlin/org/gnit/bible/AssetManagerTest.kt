package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetManagerTest : ResourcesTestBase() {

    @Test
    fun testDownload() {
        val platform = createTestPlatform()

        val httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        val am = AssetManagerImpl(httpClient, platform)
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        runBlocking { am.download(baseUrl, fileName) }
        val translations = am.downloadedTranslationCodes()
        assertContains(translations, "kttv")

        val zipBibleTextReader = ZipBibleTextReader(platform)
        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)
        assertTrue(kttvGenesisChapterOne.startsWith(TestFixtures.KTTV_GENESIS_1_1))
    }

    @Test
    fun testDownloadableTranslationList() {
        val platform = createTestPlatform()

        val httpClient = HttpClient(TestFixtures.downloadableTranslationsListMockEngine)
        val am = AssetManagerImpl(httpClient, platform)
        val listUrl = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bbllist.json"
        val result = runBlocking { am.downloadableTranslationList(listUrl) }
        val abtag = result.first()
        assertEquals("abtag", abtag.code)
        assertEquals("tl", abtag.languageCode)
        assertEquals("Ang Biblia", abtag.englishName)
        assertEquals("Public Domain", abtag.copyright)
    }
}
