package org.gnit.bible

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AssetManagerAndroidUnitTest {

    @Test
    fun test(){
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val androidPlatform = getPlatform(ctx)
        assertNotNull(androidPlatform)

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
