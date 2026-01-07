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
import org.gnit.bible.test.BibleTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliBibleTest : BibleTestBase {

    val cliBibleTestPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_cli_bible_test_dir"}"
    override val bible: Bible = Bible(
        assetManager = AssetManagerImpl(
        httpClient = HttpClient(TestFixtures.bblInstallMockEngine),
        platform = getPlatform().apply {
            overridePlatformPackDir = cliBibleTestPackDir
        }
    )).apply {
        // zip-only: no embedded reader
    }

    @BeforeTest
    fun setup(){
        val fs = FileSystem.SYSTEM
        fs.createDirectories(cliBibleTestPackDir.toPath())
        fs.list(cliBibleTestPackDir.toPath()).forEach { fs.delete(it) }
        runBlocking {
            bible.assetManager.download(DOWNLOADABLE_BIBLE_BASE_URL, "webus.zip")
            bible.assetManager.download(DOWNLOADABLE_BIBLE_BASE_URL, "jc.zip")
        }
    }

    @Test
    override fun testVerses() {
        val verses = bible.verses("webus", 1, 1)
        assertTrue(verses.startsWith(TestFixtures.WEBUS_GENESIS_1_1))
    }

    @Test
    override fun testDownloadedVerses() {
        val verses = bible.verses("jc", 1, 1)
        assertTrue(verses.startsWith(TestFixtures.JC_GENESIS_1_1))
    }

    @Test
    fun findTranslationByCodeTest() {
        assertTrue(bible.findTranslationByCode("webus"))
        assertTrue(bible.findTranslationByCode("jc"))
        assertFalse(bible.findTranslationByCode("kttv"))
        runBlocking{ bible.assetManager.download(DOWNLOADABLE_BIBLE_BASE_URL, "kttv.zip") }
        assertTrue(bible.findTranslationByCode("kttv"))
        assertFalse(bible.findTranslationByCode("unknown_code"))
    }

    @Test
    override fun testListIndexFiles() = super.testListIndexFiles()

    @Test
    override fun testReadIndexFile() = super.testReadIndexFile()
}