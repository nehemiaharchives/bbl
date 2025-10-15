package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class AssetManagerTest {

    @Test
    fun testDownload() {
        val platform = getPlatform()
        if (platform.isAndroid()) return // AssetManagerAndroidUnitTest covers android

        val httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        val am = AssetManagerImpl(httpClient, platform)
        val fileName = "kttv.zip"
        val baseUrl = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        am.download(baseUrl, fileName)
        val translations = am.downloadedTranslations()
        assertContains(translations, "kttv")

        val zipBibleTextReader = ZipBibleTextReader(platform)
        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)
        assertTrue(kttvGenesisChapterOne.startsWith(TestFixtures.KTTV_GENESIS_1_1))
    }
}
