package org.gnit.bible

import com.oldguy.common.io.File
import com.oldguy.common.io.TextFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ZipTest {

    @Test
    fun testTxtExistInResources(){

        if (isIos()) return

        val path = "src/commonTest/resources/data/test.txt"
        val file = File(path)
        val textFile = TextFile(file)
        val content = runBlocking{ textFile.readLine() }

        assertNotNull(content)
        assertEquals("test", content)
    }

    @Test
    fun testZipExistInResources(){

        if(isIos()) return

        val overridePath = "src/commonTest/resources/data"
        val platform = getPlatform()
        platform.overridePlatformPackDir = overridePath

        val zipBibleTextReader = ZipBibleTextReader(platform)

        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)

        assertContains(kttvGenesisChapterOne, TestConstants.KTTV_GENESIS_1_1)
    }
}

