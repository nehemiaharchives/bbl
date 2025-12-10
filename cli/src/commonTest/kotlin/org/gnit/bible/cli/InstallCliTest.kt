package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var fakeFs: FakeFileSystem

    @BeforeTest
    fun setup(){

        fakeFs = FakeFileSystem()
        val platform = createTestPlatform()
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
    }

    @Test
    fun testBblInstallKttv() {
        val result = Bbl(bible = bible).test("install kttv").stdout
        assertResult(result)
    }

    @Test
    fun testBblAliasGetKttv(){
        val result = Bbl(bible = bible).test("get kttv").stdout
        assertResult(result)
    }

    fun assertResult(result: String){
        assertEquals("Installed kttv\n", result)

        val packDir = bible.assetManager.platform.packDir.toPath()
        val packFileList = fakeFs.list(packDir)
        assertEquals(1, packFileList.size)
        assertEquals("kttv.zip", packFileList.first().name)

        val zipPath = packDir / "kttv.zip".toPath()
        assertTrue(fakeFs.exists(zipPath))
        fakeFs.metadata(zipPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }
}
