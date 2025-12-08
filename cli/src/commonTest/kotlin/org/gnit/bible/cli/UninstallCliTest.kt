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
import kotlin.test.assertFalse

class UninstallCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    lateinit var fakeFs: FakeFileSystem

    @BeforeTest
    fun setup(){
        val platform = createTestPlatform().apply { overridePlatformPackDir = "/tmp/bblpack" }
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        fakeFs = FakeFileSystem()
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
        Bbl(bible = bible).test("install kttv")
    }

    @Test
    fun testBblUninstallKttv() {
        val result = Bbl(bible = bible).test("uninstall kttv").stdout
        assertResult(result)
    }

    @Test
    fun testBblAliasRemoveKttv() {
        val result = Bbl(bible = bible).test("remove kttv").stdout
        assertResult(result)
    }

    @Test
    fun testBblAliasDeleteKttv() {
        val result = Bbl(bible = bible).test("delete kttv").stdout
        assertResult(result)
    }

    fun assertResult(result: String){
        assertEquals("Uninstalled kttv\n", result)
        val zipPath = "/tmp/bblpack/kttv.zip".toPath()
        assertFalse(fakeFs.exists(zipPath))
    }
}
