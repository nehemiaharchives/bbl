package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.Books
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.VersePointerJson
import org.gnit.bible.bookNumber
import org.gnit.bible.getPlatform
import org.gnit.bible.test.ZipUtil
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchCliTest {

    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_search_test_dir"}"

    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var bible: Bible

    @BeforeTest
    fun setup() {
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        fakeFs = FakeFileSystem()
        platform.overridePlatformPackDir = testPackDir
        platform.overrideFileSystem = fakeFs
        platform.settings.remove(ConfigKey.HEADER.value)
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")

        val packDirPath = platform.packDir.toPath()
        fakeFs.deleteRecursively(packDirPath, mustExist = false)
        fakeFs.createDirectories(packDirPath)
        fakeFs.write(packDirPath / "webus.zip") { write(webusSearchFixtureZipBytes) }
        fakeFs.write(packDirPath / "kjv.zip") { write(kjvSearchFixtureZipBytes) }
        fakeFs.write(packDirPath / "jc.zip") { write(jcSearchFixtureZipBytes) }

        bible = Bible(
            assetManager = AssetManagerImpl(
                platform = platform,
                fileSystem = fakeFs
            )
        )
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
    }

    @Test
    fun `bbl search Jesus Christ uses default webus translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = Translation.webus, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in kjv overrides translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = Translation.kjv, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in kjv")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search in christ stays literal search text`() {
        val backend = RecordingBackendFactory {
            assertEquals("in christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertTrue(it.filters.isEmpty())
            assertTrue(it.categoryKeys.isEmpty())
            listOf(VersePointer(translation = Translation.webus, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search in christ")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search king in david resolves category filter`() {
        val backend = RecordingBackendFactory {
            assertEquals("king", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertEquals(listOf("david"), it.categoryKeys)
            assertEquals(listOf(Books.Category.DAVID.filter), it.filters)
            listOf(VersePointer(translation = Translation.webus, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search king in david")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in johns letters resolves spaced category key`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertEquals(listOf("johns letters"), it.categoryKeys)
            assertEquals(listOf(Books.Category.JOHN_LETTERS.filter), it.filters)
            listOf(VersePointer(translation = Translation.webus, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in johns letters")

        assertEquals(0, result.statusCode)
        assertTrue(result.stdout.contains("Matthew 1:1"))
    }

    @Test
    fun `bbl search Jesus in kjv john 3 is rejected`() {
        val backend = RecordingBackendFactory {
            error("backend should not be reached for invalid mixed scope syntax")
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus in kjv john 3")

        assertTrue(result.statusCode != 0, "Command should reject compact mixed scope syntax")
        assertTrue(
            result.stderr.contains("Translation scope must be separate"),
            "Should explain the strict scope syntax. Got: ${result.stderr}"
        )
    }

    @Test
    fun `bbl search Jesus in john 3 in kjv combines repeated scopes`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(bookNumber("john"), it.bookNumber)
            assertEquals(3, it.startChapter)
            assertEquals(null, it.endChapter)
            assertTrue(it.filters.isEmpty())
            assertTrue(it.categoryKeys.isEmpty())
            listOf(VersePointer(translation = Translation.kjv, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus in john 3 in kjv")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search king in kjv in david combines translation and category`() {
        val backend = RecordingBackendFactory {
            assertEquals("king", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertEquals(listOf("david"), it.categoryKeys)
            assertEquals(listOf(Books.Category.DAVID.filter), it.filters)
            listOf(VersePointer(translation = Translation.kjv, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search king in kjv in david")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search king in david in kjv combines category and translation in reverse order`() {
        val backend = RecordingBackendFactory {
            assertEquals("king", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertEquals(listOf("david"), it.categoryKeys)
            assertEquals(listOf(Books.Category.DAVID.filter), it.filters)
            listOf(VersePointer(translation = Translation.kjv, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search king in david in kjv")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search gospel category option resolves category filter`() {
        val backend = RecordingBackendFactory {
            assertEquals("gospel", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            assertEquals(listOf("paul"), it.categoryKeys)
            assertTrue(it.filters.isNotEmpty())
            listOf(VersePointer(translation = Translation.webus, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search gospel --category paul")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans filters by book`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(bookNumber("romans"), it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = Translation.webus, book = 45, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans 5-12 filters by chapter range`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = Translation.webus, book = 45, chapter = 5, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 5-12")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans 5-12 in kjv combines range and translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = Translation.kjv, book = 45, chapter = 5, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 5-12 in kjv")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in jc uses localized book name`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("jc", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = Translation.jc, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in jc")

        assertEquals(0, result.statusCode)
        assertEquals("マタイによる福音書 1:1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n", result.stdout)
    }

    private class RecordingBackendFactory(
        private val handler: (SearchRequest) -> List<VersePointer>
    ) {
        fun backendFor(@Suppress("UNUSED_PARAMETER") translation: Translation): SearchBackend {
            return object : SearchBackend {
                override fun search(request: SearchRequest): SearchOutput {
                    return SearchOutput(VersePointerJson.encodeList(handler(request)))
                }
            }
        }
    }

    companion object {
        private val webusSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "webus.40.1.txt" to "1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n",
                "webus.45.1.txt" to "1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,\n",
                "webus.45.5.txt" to "1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;\n",
                "webus$MANIFEST_JSON_POSTFIX" to Translation.webus.toJson()
            )
        )

        private val kjvSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "kjv.40.1.txt" to "1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n",
                "kjv.45.1.txt" to "1 Paul, a servant of Jesus Christ, called to be an apostle, separated unto the gospel of God,\n",
                "kjv.45.5.txt" to "1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:\n",
                "kjv$MANIFEST_JSON_POSTFIX" to Translation.kjv.toJson()
            )
        )

        private val jcSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "jc.40.1.txt" to "1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n",
                "jc$MANIFEST_JSON_POSTFIX" to Translation.jc.toJson()
            )
        )
    }
}
