package org.gnit.bible.cli

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.getPlatform
import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliBibleTest : BibleTest {

    val cliBibleTestPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_cli_bible_test_dir"}"
    override val bible: Bible = Bible(assetManager = AssetManagerImpl(
        httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
        platform = getPlatform().apply { overridePlatformPackDir = cliBibleTestPackDir }
    )).apply {
        bibleTextReader = CliBibleTextReader()
    }

    @BeforeTest
    fun setup(){
        val kttv = cliBibleTestPackDir.toPath() / "kttv.zip"
        val fs = FileSystem.SYSTEM
        if (fs.exists(kttv)){
            fs.delete(kttv)
        }
    }

    @Test
    override fun testVerses() = super.testVerses()

    @Test
    override fun testDownloadedVerses() = super.testDownloadedVerses()

    @Test
    fun findTranslationByCodeTest() {
        assertTrue(bible.findTranslationByCode("webus"))
        assertFalse(bible.findTranslationByCode("kttv"))
        runBlocking{ bible.assetManager.download(DOWNLOADABLE_BIBLE_BASE_URL, "kttv.zip") }
        assertTrue(bible.findTranslationByCode("kttv"))
        assertFalse(bible.findTranslationByCode("unknown_code"))
    }
}
