package org.gnit.bible

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DownloaderAndroidUnitTest {

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
    }
}
