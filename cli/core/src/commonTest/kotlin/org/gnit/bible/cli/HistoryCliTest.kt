package org.gnit.bible.cli

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.HistoryFormat
import org.gnit.bible.InMemorySettings
import org.gnit.bible.getPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryCliTest {
    private val platform = getPlatform()
    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_cli_history_test_dir"}"
    private var originalPackDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem
    private var originalSettings = platform.overrideSettings
    private var originalConfigSettings = platform.overrideConfigSettings
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var bible: Bible

    @BeforeTest
    fun setup() {
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        originalConfigSettings = platform.overrideConfigSettings
        fakeFs = FakeFileSystem()
        platform.overridePlatformPackDir = testPackDir
        platform.overrideFileSystem = fakeFs
        platform.overrideSettings = InMemorySettings()
        platform.overrideConfigSettings = null
        fakeFs.createDirectories(testPackDir.toPath())
        bible = Bible(AssetManagerImpl(platform = platform, fileSystem = fakeFs))
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
    fun bblHistoryPrintsAllCommandsWithNumbers() {
        seedHistory()
        val expected = renderExpectedHistory()

        val result = Bbl(bible).test("history")

        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals(expected, result.stdout)
    }

    @Test
    fun bblHistoryFiltersReadCommands() {
        seedHistory()

        val short = Bbl(bible).test("history r")
        val long = Bbl(bible).test("history read")

        assertEquals(renderExpectedHistory("bbl gen 1"), short.stdout)
        assertEquals(short.stdout, long.stdout)
    }

    @Test
    fun bblHistoryReadFilterExcludesCommandAliases() {
        BblHistory.record(bible, "bbl gen 1")
        BblHistory.record(bible, "bbl ls")
        BblHistory.record(bible, "bbl get kjv")
        BblHistory.record(bible, "bbl pull kjv")
        BblHistory.record(bible, "bbl rm kjv")
        BblHistory.record(bible, "bbl remove kjv")
        BblHistory.record(bible, "bbl del kjv")
        BblHistory.record(bible, "bbl delete kjv")

        val result = Bbl(bible).test("history read")

        assertEquals(renderExpectedHistory("bbl gen 1"), result.stdout)
    }

    @Test
    fun bblHistoryFiltersSearchCommands() {
        seedHistory()

        val short = Bbl(bible).test("history s")
        val long = Bbl(bible).test("history search")
        val typo = Bbl(bible).test("history saerch")

        assertEquals(renderExpectedHistory("bbl search Jesus Christ"), short.stdout)
        assertEquals(short.stdout, long.stdout)
        assertEquals(short.stdout, typo.stdout)
    }

    @Test
    fun bblHistoryFiltersConfigCommands() {
        seedHistory()

        val short = Bbl(bible).test("history c")
        val long = Bbl(bible).test("history config")

        assertEquals(renderExpectedHistory("bbl config searchResult 10"), short.stdout)
        assertEquals(short.stdout, long.stdout)
    }

    @Test
    fun bblHistoryUsesDatetimeCommandFormat() {
        BblHistory.record(bible, "bbl gen 1")
        platform.configSettings.putString(ConfigKey.HISTAORY_FROMAT.value, "datetimeCommand")

        val result = Bbl(bible).test("history")

        assertTrue(
            Regex("""\s*1  \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2} bbl gen 1\n""").matches(result.stdout),
            "Expected datetimeCommand history output. stdout=${result.stdout}"
        )
    }

    @Test
    fun bblHistoryUsesDatetimeTimezoneCommandFormat() {
        BblHistory.record(bible, "bbl gen 1")
        platform.configSettings.putString(ConfigKey.HISTAORY_FROMAT.value, "datetimeTimezoneCommand")

        val result = Bbl(bible).test("history")

        assertContains(result.stdout, " bbl gen 1")
        assertTrue(
            Regex(""".*\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2} .+ bbl gen 1\n""").matches(result.stdout),
            "Expected datetimeTimezoneCommand history output. stdout=${result.stdout}"
        )
    }

    @Test
    fun bblHistoryRejectsUnknownFilter() {
        val result = Bbl(bible).test("history unknown")

        assertTrue(result.statusCode != 0, "Unknown history filter should fail")
        assertContains(result.stderr, "Unknown history filter")
    }

    @Test
    fun historyDisabledSkipsRecording() {
        platform.configSettings.putString(ConfigKey.HISTAORY_ENABLED.value, "false")

        BblHistory.record(bible, "bbl gen 1")

        assertTrue(BblHistory.read(bible).isEmpty())
    }

    private fun seedHistory() {
        BblHistory.record(bible, "bbl search Jesus Christ")
        BblHistory.record(bible, "bbl config searchResult 10")
        BblHistory.record(bible, "bbl gen 1")
        BblHistory.record(bible, "bbl list")
    }

    private fun renderExpectedHistory(vararg commands: String): String {
        val records = if (commands.isEmpty()) {
            BblHistory.read(bible)
        } else {
            BblHistory.read(bible).filter { it.command in commands.toSet() }
        }
        return BblHistory.render(records, HistoryFormat.command).joinToString("\n", postfix = "\n")
    }
}
