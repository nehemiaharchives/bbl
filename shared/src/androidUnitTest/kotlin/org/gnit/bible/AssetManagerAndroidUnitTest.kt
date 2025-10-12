package org.gnit.bible

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AssetManagerAndroidUnitTest {

    @Test
    fun test(){
        val httpClient = createPlatformHttpClient()
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val platform = getPlatform(ctx)
        assertNotNull(platform)
        val am = AssetManager(httpClient, platform)
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        am.download(baseUrl, fileName)

        val zipBibleTextReader = ZipBibleTextReader(platform)
        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)
        assertTrue(kttvGenesisChapterOne.startsWith(TestConstants.KTTV_GENESIS_1_1))
    }
}
