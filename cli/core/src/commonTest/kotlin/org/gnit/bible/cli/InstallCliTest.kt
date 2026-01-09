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
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var platform: Platform
    private var originalPackDir: String? = null
    private var originalCacheDir: String? = null
    private var originalFileSystem: okio.FileSystem? = null

    @BeforeTest
    fun setup(){

        fakeFs = FakeFileSystem()
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalCacheDir = platform.overridePlatformCacheDir
        originalFileSystem = platform.overrideFileSystem
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_kmp_cli_install_test_dir"
        platform.overridePlatformCacheDir = null
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overridePlatformCacheDir = originalCacheDir
        platform.overrideFileSystem = originalFileSystem
    }

    @Test
    fun testBblInstallKttv() {
        val result = Bbl(bible = bible).test("install kttv").output
        assertInstallResult(result, listOf("kttv"))
    }

    @Test
    fun testBblAliasGetKttv(){
        val result = Bbl(bible = bible).test("get kttv").output
        assertInstallResult(result, listOf("kttv"))
    }

    @Test
    fun testBblInstallMultipleTranslations() {
        val result = Bbl(bible = bible).test("install kttv th1971").output
        assertInstallResult(result, listOf("kttv", "th1971"))
    }

    private fun assertInstallResult(result: String, expectedCodes: List<String>){
        val expectedOutput = expectedCodes.joinToString(separator = "") { "Installed $it\n" }
        assertEquals(expectedOutput, result.replace("\r\n", "\n"))

        val packDir = bible.assetManager.platform.packDir.toPath()
        val packFileList = fakeFs.list(packDir)
        assertEquals(expectedCodes.size, packFileList.size)
        val actualNames = packFileList.map { it.name }.sorted()
        val expectedNames = expectedCodes.map { "$it.zip" }.sorted()
        assertEquals(expectedNames, actualNames)

        expectedCodes.forEach { code ->
            val zipPath = packDir / "$code.zip".toPath()
            assertTrue(fakeFs.exists(zipPath))
            fakeFs.metadata(zipPath).also { metadata ->
                assertTrue(metadata.isRegularFile)
                assertTrue((metadata.size ?: 0L) > 0L)
            }
        }
    }

    @Ignore //Integration test: touches real ~/.bbl/packs and uses network
    @Test
    fun testBblInstallInProductionEnv(){
        val command = Bbl()
        val result = command.test("install ayt")
        println(result.stdout)
    }
}
