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
    private var originalFileSystem: okio.FileSystem? = null

    @BeforeTest
    fun setup(){

        fakeFs = FakeFileSystem()
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_kmp_cli_install_test_dir"
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
    }

    @Test
    fun testBblInstallKttv() {
        val result = Bbl(bible = bible).test("install kttv").output
        assertInstallResult(result, listOf("kttv"), listOf(searchHelperName("extra")))
    }

    @Test
    fun testBblAliasGetKttv(){
        val result = Bbl(bible = bible).test("get kttv").output
        assertInstallResult(result, listOf("kttv"), listOf(searchHelperName("extra")))
    }

    @Test
    fun testBblInstallMultipleTranslations() {
        val searchHelperName = searchHelperName("extra")
        val result = Bbl(bible = bible).test("install kttv th1971").output
        assertInstallResult(
            result = result,
            expectedCodes = listOf("kttv", "th1971"),
            expectedSearchBinaries = listOf(searchHelperName),
            expectedOutputLines = listOf("Installed kttv", "Installed $searchHelperName", "Installed th1971")
        )
    }

    @Test
    fun testBblInstallJcInstallsKuromojiSearchBinary() {
        val searchHelperName = searchHelperName("kuromoji")
        val result = Bbl(bible = bible).test("install jc").output.replace("\r\n", "\n")

        assertEquals("Installed jc\nInstalled $searchHelperName\n", result)
        assertInstalledPack("jc")
        assertInstalledSearchBinary(searchHelperName)
    }

    @Test
    fun testBblInstallSkipsExistingSharedSearchBinaryDependency() {
        val searchHelperName = searchHelperName("morfologik")
        val first = Bbl(bible = bible).test("install ubio").output.replace("\r\n", "\n")
        assertEquals("Installed ubio\nInstalled $searchHelperName\n", first)
        assertInstalledPack("ubio")
        assertInstalledSearchBinary(searchHelperName)

        val second = Bbl(bible = bible).test("install ubg").output.replace("\r\n", "\n")
        assertEquals("Installed ubg\n$searchHelperName already installed, skipping download\n", second)
        assertInstalledPack("ubg")
        assertInstalledSearchBinary(searchHelperName)
    }

    private fun assertInstallResult(
        result: String,
        expectedCodes: List<String>,
        expectedSearchBinaries: List<String> = emptyList(),
        expectedOutputLines: List<String> = expectedCodes.map { "Installed $it" } +
                expectedSearchBinaries.map { "Installed $it" }
    ){
        val expectedOutput = expectedOutputLines.joinToString(separator = "\n", postfix = "\n")
        assertEquals(expectedOutput, result.replace("\r\n", "\n"))

        val packDir = bible.assetManager.platform.packDir.toPath()
        val packFileList = fakeFs.list(packDir)
        assertEquals(expectedCodes.size, packFileList.size)
        val actualNames = packFileList.map { it.name }.sorted()
        val expectedNames = expectedCodes.map { "$it.zip" }.sorted()
        assertEquals(expectedNames, actualNames)

        expectedCodes.forEach { code ->
            assertInstalledPack(code)
        }
        expectedSearchBinaries.forEach { name ->
            assertInstalledSearchBinary(name)
        }
    }

    private fun assertInstalledPack(code: String) {
        val zipPath = bible.assetManager.platform.packDir.toPath() / "$code.zip".toPath()
        assertTrue(fakeFs.exists(zipPath))
        fakeFs.metadata(zipPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }

    private fun assertInstalledSearchBinary(name: String) {
        val binPath = bible.assetManager.platform.packDir.toPath().parent!! / "bin".toPath() / name
        assertTrue(fakeFs.exists(binPath))
        fakeFs.metadata(binPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }

    private fun searchHelperName(moduleId: String): String {
        val executableSuffix = if (platform.name == "Windows") ".exe" else ""
        return "bbl-search-$moduleId$executableSuffix"
    }

    @Ignore //Integration test: touches real ~/.bbl/packs and uses network
    @Test
    fun testBblInstallInProductionEnv(){
        val command = Bbl()
        val result = command.test("install ayt")
        println(result.stdout)
    }
}
