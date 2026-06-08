package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.test.ResourcesTestBase
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AssetManagerTest : ResourcesTestBase() {

    lateinit var am: AssetManagerImpl
    val fileName = "kttv.zip"
    val baseUrl = "https://raw.githubusercontent.com/nehemiaharchives/bbl/refs/heads/master/core/src/commonTest/resources/data/"

    lateinit var platform: Platform
    lateinit var systemFileSystem: FileSystem

    @OptIn(ExperimentalTime::class)
    @BeforeTest
    fun setupAssetManagerTest(){
        platform = createTestPlatform()
        val httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        systemFileSystem = FileSystem.SYSTEM

        val timeStamp = Clock.System.now().toEpochMilliseconds()
        val tempPackDirForTest = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_shared_asset_manager_test_dir" / timeStamp.toString()
        platform.overridePlatformPackDir = tempPackDirForTest.toString()

        am = AssetManagerImpl(httpClient, platform, systemFileSystem)

    }

    @Test
    fun testDownload() {
        runBlocking { am.download(baseUrl, fileName) }

        val packDir = platform.packDir.toPath()
        assertTrue(systemFileSystem.exists(packDir / fileName))

        val translations = am.downloadedTranslationCodes()
        assertContains(translations, "kttv")

        val zipBibleResourcesReader = ZipBibleResourcesReader(platform, systemFileSystem)
        val kttvGenesisChapterOne = zipBibleResourcesReader.getChapterText("kttv", 1, 1)
        assertTrue(kttvGenesisChapterOne.startsWith(TestFixtures.KTTV_GENESIS_1_1))
    }

    @Test
    fun testDownloadedTranslations() {
        // download the pack
        runBlocking { am.download(baseUrl, fileName) }

        // fetched translations from downloaded zip manifests
        val downloaded = am.downloadedTranslations()
        // ensure we have kttv translation and its metadata matches expectations
        val kttv = downloaded.firstOrNull { it.code == "kttv" }
        assertNotNull(kttv)
        kttv.let {
            assertEquals("kttv", it.code)
            assertEquals("vi", it.languageCode)
            assertEquals("Vietnamese Bible 1925", it.englishName)
            assertEquals("Kinh Thánh Tiếng Việt", it.nativeName)
            assertEquals(1925, it.year)
            assertEquals("Public Domain", it.copyright)
        }
    }

}
