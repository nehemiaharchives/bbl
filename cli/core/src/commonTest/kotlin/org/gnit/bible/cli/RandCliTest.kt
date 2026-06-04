package org.gnit.bible.cli


import org.gnit.bible.AssetManager
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.BblVersion
import org.gnit.bible.BookChapterVerse
import org.gnit.bible.Books
import org.gnit.bible.ConfigKey
import org.gnit.bible.InMemorySettings
import org.gnit.bible.RandPicker
import org.gnit.bible.RandomlyShow
import org.gnit.bible.Translation

import org.gnit.bible.getPlatform
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandCliTest {

    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem
    private var originalSettings = platform.overrideSettings
    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_cli_rand_test_dir"}"
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var bible: Bible

    @BeforeTest
    fun enableHeaderAndVerseMode() {
        fakeFs = FakeFileSystem()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        platform.overridePlatformPackDir = testPackDir
        platform.overrideFileSystem = fakeFs
        platform.overrideSettings = InMemorySettings()

        val settings = platform.settings
        settings.remove(ConfigKey.TRANSLATION.value)
        settings.putString(ConfigKey.HEADER.value, "true")
        settings.putString(ConfigKey.RANDOMLY_SHOW.value, RandomlyShow.verse.toString())

        bible = Bible(
            assetManager = FakeAssetManager(
                platform = platform,
                fileSystem = fakeFs,
                translations = listOf(Translation.webus, Translation.jc)
            )
        )
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
    }

    private fun stubPickerForSingleVerse(verseText: String): RandPicker {
        val chapterText = "1 $verseText\n"
        return RandPicker(
            readChapter = { _, _, _ -> chapterText },
            selector = { range -> range.first }
        )
    }

    private fun stubPickerForAbrahamPassage(): RandPicker {
        // Abraham passage can start at Genesis 11:27. RandPicker's passage handling will
        // constrain the range to startVerse (27) when it picks chapter 11.
        // So we must provide >= 27 verses to avoid out-of-bounds.
        val chapterText = buildString {
            for (i in 1..31) {
                append("$i v$i\n")
            }
        }
        return RandPicker(
            readChapter = { _, _, _ -> chapterText },
            selector = { range -> range.first }
        )
    }

    @Test
    fun `bbl rand nt`() {
        val picker = stubPickerForSingleVerse("For God so loved the world.")

        val command = RandCli(bible = bible, picker = picker)
        val result = command.test(listOf("nt"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(firstLine.matches(Regex(".+ \\d+:\\d+")), "Header should match 'BookName chapter:verse' pattern. Got: $firstLine")

        // Ensure the chosen book falls under the NT filter.
        val match = Regex("^(.*) (\\d+):(\\d+)").find(firstLine)
        val bookName = match?.groupValues?.get(1)?.trim()
        assertTrue(bookName != null, "Failed to parse book name from header: $firstLine")
        val bookId = Books.bookNumber(bookName.lowercase())
        val filter = Books.Category.fromKey("nt")!!.filter
        assertTrue(filter.contains(BookChapterVerse(bookId, 1, 1)), "Header book '$bookName' should be in NT")
    }

    @Test
    fun `bbl rand ot`() {
        val picker = stubPickerForSingleVerse("In the beginning.")

        val command = RandCli(bible = bible, picker = picker)
        val result = command.test(listOf("ot"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(firstLine.matches(Regex(".+ \\d+:\\d+")), "Header should match 'BookName chapter:verse' pattern. Got: $firstLine")

        val match = Regex("^(.*) (\\d+):(\\d+)").find(firstLine)
        val bookName = match?.groupValues?.get(1)?.trim()
        assertTrue(bookName != null, "Failed to parse book name from header: $firstLine")
        val bookId = Books.bookNumber(bookName.lowercase())
        val filter = Books.Category.fromKey("ot")!!.filter
        assertTrue(filter.contains(BookChapterVerse(bookId, 1, 1)), "Header book '$bookName' should be in OT")
    }

    @Test
    fun `bbl rand prophets`() {
        val picker = stubPickerForSingleVerse("Isaiah prophecy.")

        val command = RandCli(bible = bible, picker = picker)
        val result = command.test(listOf("prophets"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(firstLine.matches(Regex(".+ \\d+:\\d+")), "Header should match 'BookName chapter:verse' pattern. Got: $firstLine")

        val match = Regex("^(.*) (\\d+):(\\d+)").find(firstLine)
        val bookName = match?.groupValues?.get(1)?.trim()
        assertTrue(bookName != null, "Failed to parse book name from header: $firstLine")
        val bookId = Books.bookNumber(bookName.lowercase())
        val filter = Books.Category.fromKey("prophets")!!.filter
        assertTrue(filter.contains(BookChapterVerse(bookId, 1, 1)), "Header book '$bookName' should be in prophets")
    }

    @Test
    fun `bbl rand paul`() {
        val picker = stubPickerForSingleVerse("Pauline intro.")

        val command = RandCli(bible = bible, picker = picker)
        val result = command.test(listOf("paul"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(firstLine.matches(Regex(".+ \\d+:\\d+")), "Header should match 'BookName chapter:verse' pattern. Got: $firstLine")

        val match = Regex("^(.*) (\\d+):(\\d+)").find(firstLine)
        val bookName = match?.groupValues?.get(1)?.trim()
        assertTrue(bookName != null, "Failed to parse book name from header: $firstLine")
        val bookId = Books.bookNumber(bookName.lowercase())
        val filter = Books.Category.fromKey("paul")!!.filter
        assertTrue(filter.contains(BookChapterVerse(bookId, 1, 1)), "Header book '$bookName' should be in paul")
    }

    @Test
    fun `bbl rand abraham in jc uses Japanese header and verse`() {
        val settings = platform.settings
        settings.putString(ConfigKey.TRANSLATION.value, "jc")

        val picker = stubPickerForAbrahamPassage()

        val command = RandCli(bible = bible, picker = picker)
        val result = command.test(listOf("abraham"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val lines = result.stdout.lines()
        val firstLine = lines.first()
        assertTrue(firstLine.startsWith("創世記"), "Header should start with '創世記' for Abraham passage in jc. Got: $firstLine")
        assertTrue(firstLine.matches(Regex("創世記 \\d+:\\d+")), "Header should match '創世記 chapter:verse' pattern. Got: $firstLine")

        // Body text is stubbed (ASCII), so don't assert Japanese characters here.
        val body = lines.drop(1).joinToString("\n")
        assertTrue(body.isNotBlank(), "Verse should not be empty")
    }

    @Test
    fun `resolveFilter maps category keys to expected BibleFilter types`() {
        // This test verifies our list of category keys is still mapped to correct filter shapes.
        // It does not depend on any bible text resources.

        fun resolve(key: String): BibleFilter {
            // We can’t reach resolveFilter directly; so we assert via Books.Category contract by checking known shapes.
            // Still helpful as guardrail.
            return Books.Category.fromKey(key)!!.filter
        }

        assertTrue(resolve("nt") is BibleFilter.BookRange)
        assertTrue(resolve("ot") is BibleFilter.BookRange)
        assertTrue(resolve("prophets") is BibleFilter.BookSet || resolve("prophets") is BibleFilter.BookRange || resolve("prophets") is BibleFilter.Union)
        assertTrue(resolve("paul") is BibleFilter.BookSet || resolve("paul") is BibleFilter.BookRange || resolve("paul") is BibleFilter.Union)

        val abraham = resolve("abraham")
        assertTrue(abraham is BibleFilter.Passage)
        assertEquals(BookChapterVerse(1, 11, 27), abraham.start)
        assertEquals(BookChapterVerse(1, 25, 11), abraham.endInclusive)
    }

    private class FakeAssetManager(
        override val platform: org.gnit.bible.Platform,
        override val fileSystem: FileSystem,
        private val translations: List<Translation>
    ) : AssetManager {

        override suspend fun downloadableTranslationList(listUrl: String): List<Translation> = translations

        override suspend fun download(baseUrl: String, fileName: String) {
            // No-op for these tests: they don't exercise downloads.
        }

        override suspend fun downloadTo(baseUrl: String, fileName: String, destinationDir: String) {
            // No-op for these tests: they don't exercise downloads.
        }

        override fun downloadedTranslationCodes(): List<String> = translations.map { it.code }

        override fun downloadedTranslations(): List<Translation> = translations

        override fun delete(translationCode: String) {
            // No-op: no real files to delete in these tests.
        }
    }
}
