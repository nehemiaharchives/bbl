package org.gnit.bible.cli


import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.CONFIG_FILE_NAME
import org.gnit.bible.LoggingSetup
import org.gnit.bible.SearchQueryText
import org.gnit.bible.Books
import org.gnit.bible.BblVersion
import org.gnit.bible.ConfigKey
import org.gnit.bible.InMemorySettings
import org.gnit.bible.getPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigCliTest {

    lateinit var fakeFs: FileSystem
    lateinit var bible: Bible
    lateinit var settingsPath: Path
    lateinit var bblDir: Path
    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem
    private var originalSettings = platform.overrideSettings
    private var originalConfigSettings = platform.overrideConfigSettings

    @BeforeTest
    fun setup(){
        fakeFs = FakeFileSystem()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        originalConfigSettings = platform.overrideConfigSettings
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_cli_config_test_dir"
        platform.overrideSettings = InMemorySettings()
        platform.overrideConfigSettings = null
        platform.settings.clear()

        val packDirPath = platform.packDir.toPath()
        bblDir = packDirPath.parent!!
        settingsPath = bblDir.resolve(CONFIG_FILE_NAME)

        val am = AssetManagerImpl(platform = platform, fileSystem = fakeFs)
        bible = Bible(am)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
        platform.overrideConfigSettings = originalConfigSettings
    }

    @Test
    fun bllConfigInitTest(){
        val command = Bbl(bible)
        val result = command.test("config init")

        // With no embedded packs and an empty fake filesystem, init cannot succeed because
        // the default translation (webus) isn't installed yet.
        assertTrue(result.statusCode != 0, "Command should fail when default translation pack is not installed")
        assertTrue(
            result.stderr.contains("bbl install", ignoreCase = true),
            "Should suggest installing a translation pack. Got: ${result.stderr}"
        )

        // Ensure we didn't create a settings file on failure.
        assertTrue(!fakeFs.exists(settingsPath), "settings file should not be created when init fails")
    }

    @Test
    fun configReadMissingKeyFails() {
        val command = Bbl(bible)
        val result = command.test("config translation")

        assertTrue(result.statusCode != 0, "Command should fail when key is not set")
        assertTrue(result.stderr.isNotBlank(), "Should explain missing config key")
    }

    @Test
    fun configWriteThenReadTranslation() {
        val command = Bbl(bible)

        // Without an installed pack, setting translation should fail.
        val write = command.test("config translation jc")
        assertTrue(write.statusCode != 0, "Write should fail when translation isn't installed")
        assertTrue(write.stderr.isNotBlank(), "Should explain why write failed")
    }

    @Test
    fun configReadExistingTranslationShowsValue() {
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")

        val command = Bbl(bible)
        val result = command.test("config translation")

        assertEquals(0, result.statusCode, "Read should succeed. stderr=${result.stderr}")
        assertEquals("webus", result.stdout.trim())
    }

    @Test
    fun configSetTranslationShowsConfirmation() {
        fakeFs.createDirectories(platform.packDir.toPath())
        fakeFs.write(platform.packDir.toPath() / "webus.zip") {
            write(TestFixtures.webusMinimalZipBytes)
        }

        val command = Bbl(bible)
        val result = command.test("config translation webus")

        assertEquals(0, result.statusCode, "Write should succeed. stderr=${result.stderr}")
        assertEquals("translation set to webus", result.stdout.trim())
    }

    @Test
    fun configWriteInvalidRandomlyShowFails() {
        val command = Bbl(bible)
        val result = command.test("config randomlyShow invalidValue")
        assertTrue(result.statusCode != 0, "Command should fail on invalid randomlyShow")
        assertTrue(result.stderr.isNotBlank(), "Should show allowed values")
    }

    @Test
    fun configWriteThenReadCompareBy() {
        val command = Bbl(bible)

        val write = command.test("config compareBy verse")
        assertEquals(0, write.statusCode, "Write should succeed. stderr=${write.stderr}")
        assertEquals("compareBy set to verse", write.stdout.trim())

        val read = command.test("config compareBy")
        assertEquals(0, read.statusCode, "Read should succeed. stderr=${read.stderr}")
        assertEquals("verse", read.stdout.trim())
    }

    @Test
    fun configWriteThenReadHistoryEnabled() {
        val command = Bbl(bible)

        val write = command.test("config historyEnabled false")
        assertEquals(0, write.statusCode, "Write should succeed. stderr=${write.stderr}")
        assertEquals("historyEnabled set to false", write.stdout.trim())

        val read = command.test("config historyEnabled")
        assertEquals(0, read.statusCode, "Read should succeed. stderr=${read.stderr}")
        assertEquals("false", read.stdout.trim())
    }

    @Test
    fun configWriteThenReadHistoryFormat() {
        val command = Bbl(bible)

        val write = command.test("config historyFormat datetimeTimezoneCommand")
        assertEquals(0, write.statusCode, "Write should succeed. stderr=${write.stderr}")
        assertEquals("historyFormat set to datetimeTimezoneCommand", write.stdout.trim())

        val read = command.test("config historyFormat")
        assertEquals(0, read.statusCode, "Read should succeed. stderr=${read.stderr}")
        assertEquals("datetimeTimezoneCommand", read.stdout.trim())
    }

    @Test
    fun configWriteInvalidHistoryFormatFails() {
        val result = Bbl(bible).test("config historyFormat invalidValue")

        assertTrue(result.statusCode != 0, "Command should fail on invalid historyFormat")
        assertTrue(result.stderr.contains("datetimeTimezoneCommand"), "Should show allowed values. stderr=${result.stderr}")
    }

    @Test
    fun configRecordsHistoryWhenHistoryEnabled() {
        platform.configSettings.putString(ConfigKey.HISTAORY_ENABLED.value, "true")

        val result = Bbl(bible).test("config searchResult 10")

        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals(listOf("bbl config searchResult 10"), BblHistory.read(bible).map { it.command })
    }

    @Test
    fun configDoesNotRecordHistoryWhenHistoryDisabled() {
        platform.configSettings.putString(ConfigKey.HISTAORY_ENABLED.value, "false")

        val result = Bbl(bible).test("config searchResult 10")

        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertTrue(BblHistory.read(bible).isEmpty(), "History should remain empty when disabled")
    }

    @Test
    fun configWriteInvalidCompareByFails() {
        val command = Bbl(bible)
        val result = command.test("config compareBy invalidValue")

        assertTrue(result.statusCode != 0, "Command should fail on invalid compareBy")
        assertTrue(result.stderr.contains("block"), "Should show allowed values. stderr=${result.stderr}")
        assertTrue(result.stderr.contains("verse"), "Should show allowed values. stderr=${result.stderr}")
    }

    @Test
    fun compareByBlockPrintsMultipleTranslationsByTranslationBlock() {
        installMinimalPacks()
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")
        platform.configSettings.putString(ConfigKey.COMPARE_BY.value, "block")

        val command = Bbl(bible)
        val result = command.test("matt 28:18-20 in jc webus")

        val expected = """
            18 ${TestFixtures.JC_MATT_28_18}
            19 ${TestFixtures.JC_MATT_28_19}
            20 ${TestFixtures.JC_MATT_28_20}
            18 ${TestFixtures.WEBUS_MATT_28_18}
            19 ${TestFixtures.WEBUS_MATT_28_19}
            20 ${TestFixtures.WEBUS_MATT_28_20}
        """.trimIndent()
        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals("$expected\n", result.stdout)
    }

    @Test
    fun compareByVersePrintsMultipleTranslationsVerseByVerse() {
        installMinimalPacks()
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")
        platform.configSettings.putString(ConfigKey.COMPARE_BY.value, "verse")

        val command = Bbl(bible)
        val result = command.test("matt 28:18-20 in jc webus")

        val expected = """
            18 ${TestFixtures.JC_MATT_28_18}
            18 ${TestFixtures.WEBUS_MATT_28_18}
            19 ${TestFixtures.JC_MATT_28_19}
            19 ${TestFixtures.WEBUS_MATT_28_19}
            20 ${TestFixtures.JC_MATT_28_20}
            20 ${TestFixtures.WEBUS_MATT_28_20}
        """.trimIndent()
        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals("$expected\n", result.stdout)
    }

    @Test
    fun compareByVersePrintsWholeChapterVerseByVerse() {
        installMinimalPacks()
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")
        platform.configSettings.putString(ConfigKey.COMPARE_BY.value, "verse")

        val command = Bbl(bible)
        val result = command.test("gen 1 in jc webus")

        val expected = """
            ${TestFixtures.JC_GENESIS_1_1}
            ${TestFixtures.WEBUS_GENESIS_1_1}
        """.trimIndent()
        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals("$expected\n", result.stdout)
    }

    @Test
    fun configWriteThenReadSearchResult() {
        val command = Bbl(bible)

        val write = command.test("config searchResult 10")
        assertTrue(write.statusCode == 0, "Write should succeed. stderr=${write.stderr}")

        val read = command.test("config searchResult")
        assertTrue(read.statusCode == 0, "Read should succeed. stderr=${read.stderr}")
        assertTrue(read.stdout.trim() == "10", "Expected searchResult output 10, got: ${read.stdout}")
    }

    @Test
    fun configWriteInvalidSearchResultFails() {
        val command = Bbl(bible)
        val result = command.test("config searchResult 0")
        assertTrue(result.statusCode != 0, "Command should fail on invalid searchResult")
        assertTrue(result.stderr.contains("positive integer"), "Should show allowed value shape")
    }

    @Test
    fun configUnknownKeyFails() {
        val command = Bbl(bible)
        val result = command.test("config doesNotExist")
        assertTrue(result.statusCode != 0, "Command should fail on unknown key")
        assertTrue(result.stderr.isNotBlank(), "Should report unknown key")
    }

    @Test
    fun configWriteInvalidTranslationFails() {
        val command = Bbl(bible)
        val result = command.test("config translation definitelyNotARealTranslation")
        assertTrue(result.statusCode != 0, "Command should fail on invalid translation code")
        assertTrue(result.stderr.isNotBlank(), "Should report invalid translation code")
    }

    @Test
    fun configAliasTranslationWorksForWrite() {
        fakeFs.createDirectories(platform.packDir.toPath())
        fakeFs.write(platform.packDir.toPath() / "webus.zip") {
            write(TestFixtures.webusMinimalZipBytes)
        }

        val command = Bbl(bible)
        val result = command.test("config tr webus")
        assertEquals(0, result.statusCode, "Alias 'tr' should work for write. stderr=${result.stderr}")
        assertEquals("translation set to webus", result.stdout.trim())
    }

    @Test
    fun configAliasTranslationWorksForRead() {
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")

        val result = Bbl(bible).test("config tr")

        assertEquals(0, result.statusCode, "Alias 'tr' should work for read. stderr=${result.stderr}")
        assertEquals("webus", result.stdout.trim())
    }

    @Test
    fun configAliasSearchResultWorks() {
        val command = Bbl(bible)

        val write = command.test("config sr 10")
        assertEquals(0, write.statusCode, "Alias 'sr' should work for write. stderr=${write.stderr}")
        assertEquals("searchResult set to 10", write.stdout.trim())

        val read = command.test("config sr")
        assertEquals(0, read.statusCode, "Alias 'sr' should work for read. stderr=${read.stderr}")
        assertEquals("10", read.stdout.trim())
    }

    @Test
    fun configAliasHistoryEnabledWorksWithAliasHe() {
        val command = Bbl(bible)

        val write = command.test("config he false")
        assertEquals(0, write.statusCode, "Alias 'he' should work. stderr=${write.stderr}")
        assertEquals("historyEnabled set to false", write.stdout.trim())
    }

    @Test
    fun configAliasHeaderWorksWithAliasHd() {
        val command = Bbl(bible)

        val write = command.test("config hd true")
        assertEquals(0, write.statusCode, "Alias 'hd' should work. stderr=${write.stderr}")
        assertEquals("header set to true", write.stdout.trim())
    }

    @Test
    fun configAliasHistoryFormatWorksWithAliasHf() {
        val command = Bbl(bible)

        val write = command.test("config hf datetimeTimezoneCommand")
        assertEquals(0, write.statusCode, "Alias 'hf' should work. stderr=${write.stderr}")
        assertEquals("historyFormat set to datetimeTimezoneCommand", write.stdout.trim())
    }

    @Test
    fun configAliasCompareByWorksWithAliasCb() {
        val command = Bbl(bible)

        val write = command.test("config cb verse")
        assertEquals(0, write.statusCode, "Alias 'cb' should work. stderr=${write.stderr}")
        assertEquals("compareBy set to verse", write.stdout.trim())
    }

    @Test
    fun configAliasRandomlyShowWorksWithAliasRs() {
        val command = Bbl(bible)

        val write = command.test("config rs chapter")
        assertEquals(0, write.statusCode, "Alias 'rs' should work. stderr=${write.stderr}")
        assertEquals("randomlyShow set to chapter", write.stdout.trim())
    }

    @Test
    fun configWriteDownloadableButNotInstalledTranslationSuggestsInstall() {
        val command = Bbl(bible)
        val result = command.test("config translation ayt")

        assertTrue(result.statusCode != 0, "Command should fail when translation isn't installed")
        assertTrue(
            result.stderr.contains("bbl install", ignoreCase = true),
            "Should suggest installing the translation. Got: ${result.stderr}"
        )
    }

    private fun installMinimalPacks() {
        fakeFs.createDirectories(platform.packDir.toPath())
        fakeFs.write(platform.packDir.toPath() / "webus.zip") {
            write(TestFixtures.webusMinimalZipBytes)
        }
        fakeFs.write(platform.packDir.toPath() / "jc.zip") {
            write(TestFixtures.jcMinimalZipBytes)
        }
    }
}
