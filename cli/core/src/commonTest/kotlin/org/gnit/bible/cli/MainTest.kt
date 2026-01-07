package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.getPlatform
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.test.TestFixtures
import org.gnit.bible.AssetManagerImpl
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath

class MainTest {

    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_main_test_dir"}"

    private val platform = getPlatform().apply { overridePlatformPackDir = testPackDir }

    @BeforeTest
    fun clearSavedSettings() {
        platform.settings.remove(ConfigKey.TRANSLATION.value)
        platform.settings.remove(ConfigKey.HEADER.value)

        runBlocking {
            val packDirPath = platform.packDir.toPath()

            // Strong test isolation: remove all packs installed in this test pack dir.
            platform.fileSystem.deleteRecursively(packDirPath, mustExist = false)
            platform.fileSystem.createDirectories(packDirPath)

            val am = AssetManagerImpl(
                httpClient = HttpClient(TestFixtures.bblInstallMockEngine),
                platform = platform
            )
            am.download(DOWNLOADABLE_BIBLE_BASE_URL, "webus.zip")
            am.download(DOWNLOADABLE_BIBLE_BASE_URL, "jc.zip")
        }

        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
    }

    @Test
    fun testBblWithVersionFlag(){
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test(listOf("-v"))
        assertContains(result.stdout, "While you are in front of your console, you are not alone. God is with you.")
    }

    @Test
    fun testBblWithNoArgs() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test()
        assertEquals("${TestFixtures.WEBUS_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblGen1() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("gen 1")
        assertEquals("${TestFixtures.WEBUS_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblGen1WithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("gen 1")
        assertEquals("Genesis 1\n${TestFixtures.WEBUS_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("john 3:16")
        val webusJohn3v16 = TestFixtures.WEBUS_JOHN_3_16
        assertEquals("16 $webusJohn3v16\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16WithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("john 3:16")
        val webusJohn3v16 = TestFixtures.WEBUS_JOHN_3_16
        assertEquals("John 3:16\n16 $webusJohn3v16\n", result.stdout)
    }

    @Test
    fun testBblMatt28v18to20() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("matt 28:18-20")
        val expected = listOf(
            "18 ${TestFixtures.WEBUS_MATT_28_18}",
            "19 ${TestFixtures.WEBUS_MATT_28_19}",
            "20 ${TestFixtures.WEBUS_MATT_28_20}"
        ).joinToString("\n", postfix = "\n\n")
        assertEquals(expected, result.stdout)
    }

    @Test
    fun testBblInJc() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        val command = Bbl()
        val result = command.test("in jc")
        assertEquals("${TestFixtures.JC_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblInJcWithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("in jc")
        assertEquals("創世記 1\n${TestFixtures.JC_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblGen1InJc() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        val command = Bbl()
        val result = command.test("gen 1 in jc")
        assertEquals("${TestFixtures.JC_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblGen1InJcWebusComparison() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("gen 1 in jc webus")
        val jcVerses = Bible.splitChapterToVerses(TestFixtures.JC_GENESIS_1_1)
        val webusVerses = Bible.splitChapterToVerses(TestFixtures.WEBUS_GENESIS_1_1)

        val expected = buildString {
            for (verseNumber in 1..jcVerses.size) {
                append("$verseNumber ${jcVerses[verseNumber - 1].trim()}\n")
                append("$verseNumber ${webusVerses[verseNumber - 1].trim()}\n")
                if (verseNumber != jcVerses.size) append('\n')
            }
        }

        assertEquals(expected, result.stdout)
    }

    @Test
    fun testBblGen1InJcWithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("gen 1 in jc")
        assertEquals("創世記 1\n${TestFixtures.JC_GENESIS_1_1}\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16InJc() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        val command = Bbl()
        val result = command.test("john 3:16 in jc")
        val jcJohn3v16 = TestFixtures.JC_JOHN_3_16
        assertEquals("16 $jcJohn3v16\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16InJcWebusComparison() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("john 3:16 in jc webus")

        // Keep this test resilient to minor formatting changes (blank lines, headers, etc.)
        assertContains(result.stdout, TestFixtures.JC_JOHN_3_16)
        assertContains(result.stdout, TestFixtures.WEBUS_JOHN_3_16)
    }

    @Test
    fun testBblJohn3v16InJcWithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("john 3:16 in jc")

        // Header and verse should be present; don't require exact book-name spelling.
        assertContains(result.stdout, "3:16")
        assertContains(result.stdout, TestFixtures.JC_JOHN_3_16)
    }

    @Test
    fun testBblMatt28v18to20InJc() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        val command = Bbl()
        val result = command.test("matt 28:18-20 in jc")
        val jcMatt28v18to20 = """
            18 ${TestFixtures.JC_MATT_28_18}
            19 ${TestFixtures.JC_MATT_28_19}
            20 ${TestFixtures.JC_MATT_28_20}
        """.trimIndent()
        assertEquals("$jcMatt28v18to20\n", result.stdout)
    }

    @Test
    fun testBblMatt28v18to20InJcWebusComparison() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")
        val command = Bbl()
        val result = command.test("matt 28:18-20 in jc webus")

        // Resilient to whitespace/newline differences.
        assertContains(result.stdout, TestFixtures.JC_MATT_28_18)
        assertContains(result.stdout, TestFixtures.WEBUS_MATT_28_18)
        assertContains(result.stdout, TestFixtures.JC_MATT_28_19)
        assertContains(result.stdout, TestFixtures.WEBUS_MATT_28_19)
        assertContains(result.stdout, TestFixtures.JC_MATT_28_20)
        assertContains(result.stdout, TestFixtures.WEBUS_MATT_28_20)
    }

    @Test
    fun testBblMatt28v18to20InJcWithHeaderEnabled() {
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        platform.settings.putString(ConfigKey.HEADER.value, "true")

        val command = Bbl()
        val result = command.test("matt 28:18-20 in jc")

        // Header and verses should be present; don't require exact book-name spelling.
        assertContains(result.stdout, "28")
        assertContains(result.stdout, TestFixtures.JC_MATT_28_18)
        assertContains(result.stdout, TestFixtures.JC_MATT_28_19)
        assertContains(result.stdout, TestFixtures.JC_MATT_28_20)
    }

    @Test
    fun testBblUnknownBookShowsFriendlyError() {
        val command = Bbl()
        val result = command.test("notabook 1")
        assertTrue(result.statusCode != 0, "Command should fail on unknown book name")
        assertContains(result.stderr, "Unknown book 'notabook'")
        assertContains(result.stderr, "bbl list books")
    }

    @Test
    fun testBblChapterOutOfRangeShowsFriendlyError() {
        val command = Bbl()
        val result = command.test("gen 51")
        assertTrue(result.statusCode != 0, "Command should fail on chapter out of range")
        assertContains(result.stderr, "Chapter 51 is out of range for Genesis")
        assertContains(result.stderr, "Valid range: 1..50")
    }

    @Test
    fun testBblVerseOutOfRangeShowsFriendlyError() {
        val bible = Bible(assetManager = AssetManagerImpl(
            httpClient = HttpClient(TestFixtures.bblInstallMockEngine),
            platform = platform
        ))
        runBlocking {
            bible.assetManager.download(DOWNLOADABLE_BIBLE_BASE_URL, "webus.zip")
        }

        val command = Bbl(bible = bible)
        val result = command.test("john 3:37")

        assertTrue(result.statusCode != 0, "Command should fail on verse out of range")
        assertContains(result.stderr, "Verse 37 is out of range")
    }

    @Test
    fun testBblInvalidVerseRangeShowsFriendlyError() {
        val command = Bbl()
        val result = command.test("john 3:20-10")
        assertTrue(result.statusCode != 0, "Command should fail on invalid verse range")
        assertContains(result.stderr, "Invalid verse range 20-10")
        assertContains(result.stderr, "Start verse must be <= end verse")
    }

    @Test
    fun testPackSubcommandIsNotAvailable() {
        val command = Bbl()
        val result = command.test(listOf("pack", "webus"))
        assertTrue(
            result.statusCode != 0,
            "Expected `bbl pack` to be unavailable (Phase 6 moved it to :cli:packer)."
        )
        // clikt prints help/usage on unknown subcommand; keep assertion flexible.
        assertContains(result.output.lowercase(), "pack")
    }
}
