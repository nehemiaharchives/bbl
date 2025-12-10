package org.gnit.bible

import io.ktor.client.HttpClient
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
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
    val baseUrl =
        "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/shared/src/commonTest/resources/data/"

    lateinit var platform: Platform
    lateinit var systemFileSystem: FileSystem

    @OptIn(ExperimentalTime::class)
    @BeforeTest
    fun setupAssetManagerTest(){
        platform = createTestPlatform()
        val httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        systemFileSystem = FileSystem.SYSTEM

        val timeStamp = Clock.System.now().toEpochMilliseconds()
        val tempPackDirForTest = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_shared_asset_manager_test_dir" / timeStamp.toString()
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

        val zipBibleTextReader = ZipBibleTextReader(platform, systemFileSystem)
        val kttvGenesisChapterOne = zipBibleTextReader.getChapterText("kttv", 1, 1)
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

    @Test
    fun testDownloadableTranslationList() {
        val httpClient = HttpClient(TestFixtures.downloadableTranslationsListMockEngine)
        val am = AssetManagerImpl(httpClient, platform)
        val listUrl = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bbllist.json"
        val result = runBlocking { am.downloadableTranslationList(listUrl) }
        val abtag = result.first()
        assertEquals("abtag", abtag.code)
        assertEquals("tl", abtag.languageCode)
        assertEquals("Ang Biblia", abtag.englishName)
        assertEquals("Public Domain", abtag.copyright)
    }
}
