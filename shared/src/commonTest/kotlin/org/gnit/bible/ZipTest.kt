package org.gnit.bible

import com.oldguy.common.io.File
import com.oldguy.common.io.TextFile
import com.oldguy.common.io.ZipFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZipTest {

    @Test
    fun testTxtExistInResources(){
        val path = "src/commonTest/resources/data/test.txt"
        val file = File(path)
        val textFile = TextFile(file)
        val content = runBlocking{ textFile.readLine() }

        assertNotNull(content)
        assertEquals("test", content)
    }

    @Test
    fun testZipExistInResources() = runBlocking {
        val path = "src/commonTest/resources/data/kttv.zip"
        val file = File(path)
        assertTrue(file.exists, "Zip file not found at $path")

        val zipFile = ZipFile(file)

        zipFile.use { zip ->
            val targetName = zip.entries.firstOrNull { it.name.endsWith("kttv.1.1.txt") }?.name
                ?: error("No entry ending with kttv.1.1.txt found")

            val sb = StringBuilder()
            zip.readTextEntry(targetName) { text, _ -> sb.append(text) }
            assertContains(sb.toString(), "1 Ban đầu Đức Chúa Trời dựng nên trời đất.")

        }
    }
}
