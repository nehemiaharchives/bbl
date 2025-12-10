package org.gnit.bible.cli

import io.ktor.client.HttpClient
import okio.FileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.getPlatform
import org.gnit.bible.test.BibleTest
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test

class CliBibleTest : BibleTest {

    override val bible: Bible = Bible(assetManager = AssetManagerImpl(
        httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine),
        platform = getPlatform().apply { overridePlatformPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_cli_bible_test_dir"}" }
    )).apply {
        bibleTextReader = CliBibleTextReader()
    }

    @Test
    override fun testVerses() = super.testVerses()

    @Test
    override fun testDownloadedVerses() = super.testDownloadedVerses()
}
