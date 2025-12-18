package org.gnit.bible

import com.oldguy.common.io.File
import com.oldguy.common.io.TextFile
import kotlinx.coroutines.runBlocking
import okio.SYSTEM
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ZipTest {

    @Test
    fun testTxtExistInResources(){

        val platform = getPlatform()
        if(platform.isIos()) return // iOS resources are not accessible via path

        val path = "src/commonTest/resources/data/test.txt"
        val file = File(path)
        val textFile = TextFile(file)
        val content = runBlocking{ textFile.readLine() }

        assertNotNull(content)
        assertEquals("test", content)
    }

    @Test
    fun testZipExistInResources(){

        val platform = getPlatform()
        if(platform.isIos()) return // iOS resources are not accessible via path

        val overridePath = "src/commonTest/resources/data"
        platform.overridePlatformPackDir = overridePath

        val zipBibleResourcesReader = ZipBibleResourcesReader(platform, okio.FileSystem.SYSTEM)

        val kttvGenesisChapterOne = zipBibleResourcesReader.getChapterText("kttv", 1, 1)

        assertContains(kttvGenesisChapterOne, TestFixtures.KTTV_GENESIS_1_1)
    }

    @Test
    fun testGetTranslationFromManifest(){

        val platform = getPlatform()
        if(platform.isIos()) return // iOS resources are not accessible via path

        val overridePath = "src/commonTest/resources/data"
        platform.overridePlatformPackDir = overridePath

        val zipBibleResourcesReader = ZipBibleResourcesReader(platform, okio.FileSystem.SYSTEM)

        val translation = zipBibleResourcesReader.getTranslationFromManifest("kttv")

        assertEquals("kttv", translation.code)
    }
}
