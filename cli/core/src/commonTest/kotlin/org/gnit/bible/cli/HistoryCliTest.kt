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
            Regex("""\s*1  \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} bbl gen 1\n""").matches(result.stdout),
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
            Regex(""".*\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} .+ bbl gen 1\n""").matches(result.stdout),
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
    fun historyNormalizesBookNameAliases() {
        assertEquals("bbl genesis 4", BblHistory.normalizeReadCommand(listOf("gn"), "4"))
        assertEquals("bbl genesis 4", BblHistory.normalizeReadCommand(listOf("gen"), "4"))
        assertEquals("bbl genesis 4", BblHistory.normalizeReadCommand(listOf("genesis"), "4"))
        assertEquals("bbl genesis 3:1-2", BblHistory.normalizeReadCommand(listOf("gen"), "3:1-2"))
        assertEquals("bbl genesis 5:2", BblHistory.normalizeReadCommand(listOf("gen"), "5:2"))
        assertEquals("bbl 2 john 1", BblHistory.normalizeReadCommand(listOf("2john"), "1"))
        assertEquals("bbl 2 john 1", BblHistory.normalizeReadCommand(listOf("2", "john"), "1"))
        assertEquals("bbl 3 john 1", BblHistory.normalizeReadCommand(listOf("3john"), "1"))
        assertEquals("bbl 1 john 1", BblHistory.normalizeReadCommand(listOf("1john"), "1"))
        assertEquals("bbl ecclesiastes 3:1-8", BblHistory.normalizeReadCommand(listOf("eccles"), "3:1-8"))
        assertEquals("bbl song of solomon 2", BblHistory.normalizeReadCommand(listOf("song", "of", "solomon"), "2"))
        assertEquals("bbl matthew 5:3-7", BblHistory.normalizeReadCommand(listOf("matt"), "5:3-7"))
        assertEquals("bbl psalms 23", BblHistory.normalizeReadCommand(listOf("ps"), "23"))
        assertEquals("bbl 1st samuel 17", BblHistory.normalizeReadCommand(listOf("1sam"), "17"))
        assertEquals("bbl 1st samuel 17", BblHistory.normalizeReadCommand(listOf("1", "samuel"), "17"))
        assertEquals("bbl revelation 21:1-4", BblHistory.normalizeReadCommand(listOf("rev"), "21:1-4"))
    }

    @Test
    fun historyRecordingNormalizesReadCommands() {
        BblHistory.record(bible, BblHistory.normalizeReadCommand(listOf("gn"), "4"))
        BblHistory.record(bible, BblHistory.normalizeReadCommand(listOf("2john"), "1"))
        BblHistory.record(bible, BblHistory.normalizeReadCommand(listOf("eccles"), "3:1-8"))

        val records = BblHistory.read(bible)
        assertEquals(3, records.size)
        assertEquals("bbl genesis 4", records[0].command)
        assertEquals("bbl 2 john 1", records[1].command)
        assertEquals("bbl ecclesiastes 3:1-8", records[2].command)

        val result = Bbl(bible).test("history")
        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertContains(result.stdout, "bbl genesis 4")
        assertContains(result.stdout, "bbl 2 john 1")
        assertContains(result.stdout, "bbl ecclesiastes 3:1-8")
    }

    @Test
    fun historyFormatBookChapterVerseOpenEndedRange() {
        val result = formatBookChapterVerse(43, 3, 16, -1)
        assertEquals("john 3:16-", result)
    }

    @Test
    fun historyFormatBookChapterVerseClosedRange() {
        val result = formatBookChapterVerse(43, 3, 16, 18)
        assertEquals("john 3:16-18", result)
    }

    @Test
    fun historyFormatBookChapterVerseSingleVerse() {
        val result = formatBookChapterVerse(43, 3, 16, null)
        assertEquals("john 3:16", result)
    }

    @Test
    fun historyFormatBookChapterVerseWholeChapter() {
        val result = formatBookChapterVerse(43, 3, null, null)
        assertEquals("john 3", result)
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
