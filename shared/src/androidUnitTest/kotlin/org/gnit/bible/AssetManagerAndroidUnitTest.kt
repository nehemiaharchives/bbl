package org.gnit.bible

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AssetManagerAndroidUnitTest : ResourcesTestBase() {

    @Test
    fun test(){

        val androidPlatform = createTestPlatform()
        val mockHttpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        val am = AssetManagerImpl(httpClient = mockHttpClient, platform = androidPlatform)
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        am.download(baseUrl, fileName)

        assertContains(am.downloadedTranslations(), "kttv")

        val zipBibleTextReader = ZipBibleTextReader(androidPlatform)
        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)
        assertTrue(kttvGenesisChapterOne.startsWith(TestFixtures.KTTV_GENESIS_1_1))
    }
}
