package org.gnit.bible

import kotlin.test.Test

class DownloaderTest {

    @Test
    fun testDownload() {
        val httpClient = createPlatformHttpClient()
        val platform = getPlatform()
        val am = AssetManager(httpClient, platform)
        val fileName = "kttv.zip"
        val baseUrl =
            "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"
        am.download(baseUrl, fileName)
    }
}
