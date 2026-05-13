package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Platform
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UninstallCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var platform: Platform
    private var originalPackDir: String? = null
    private var originalFileSystem: okio.FileSystem? = null

    @BeforeTest
    fun setup(){
        val packDir = "/tmp/bblpack-cli-uninstall"
        fakeFs = FakeFileSystem()
        fakeFs.createDirectories(packDir.toPath(), mustCreate = false)
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        platform.overridePlatformPackDir = packDir
        platform.overrideFileSystem = fakeFs
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
        Bbl(bible = bible).test("install kttv th1971")
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
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
    fun testBblAliasRmKttv() {
        val result = Bbl(bible = bible).test("rm kttv").stdout
        assertResult(result)
    }

    @Test
    fun testBblAliasDeleteKttv() {
        val result = Bbl(bible = bible).test("delete kttv").stdout
        assertResult(result)
    }

    @Test
    fun testBblUninstallMultipleTranslations() {
        val result = Bbl(bible = bible).test("uninstall kttv th1971").stdout
        assertEquals("Uninstalled kttv\nUninstalled bbl-search-extra\nUninstalled th1971\n", result)
        assertFalse(fakeFs.exists("/tmp/bblpack-cli-uninstall/kttv.zip".toPath()))
        assertFalse(fakeFs.exists("/tmp/bblpack-cli-uninstall/th1971.zip".toPath()))
        assertFalse(fakeFs.exists("/tmp/bin/bbl-search-extra".toPath()))
    }

    @Test
    fun testBblUninstallKeepsSearchBinaryStillNeededByAnotherPack() {
        Bbl(bible = bible).test("install ubio ubg")

        val result = Bbl(bible = bible).test("uninstall ubio").stdout

        assertEquals("Uninstalled ubio\n", result)
        assertFalse(fakeFs.exists("/tmp/bblpack-cli-uninstall/ubio.zip".toPath()))
        assertEquals(true, fakeFs.exists("/tmp/bblpack-cli-uninstall/ubg.zip".toPath()))
        assertEquals(true, fakeFs.exists("/tmp/bin/bbl-search-morfologik".toPath()))
    }

    fun assertResult(result: String){
        assertEquals("Uninstalled kttv\nUninstalled bbl-search-extra\n", result)
        val zipPath = "/tmp/bblpack-cli-uninstall/kttv.zip".toPath()
        assertFalse(fakeFs.exists(zipPath))
        assertFalse(fakeFs.exists("/tmp/bin/bbl-search-extra".toPath()))
    }
}
