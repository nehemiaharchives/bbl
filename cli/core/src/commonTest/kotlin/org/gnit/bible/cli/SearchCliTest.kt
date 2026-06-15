package org.gnit.bible.cli

import org.gnit.bible.SupportedTranslation


import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.Books
import org.gnit.bible.InMemorySettings
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.VersePointerJson

import org.gnit.bible.getPlatform
import org.gnit.bible.test.ZipUtil
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchCliTest {

    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_cli_search_test_dir"}"

    private val platform = getPlatform()
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
        platform.configSettings.remove(ConfigKey.HEADER.value)
        platform.configSettings.putString(ConfigKey.TRANSLATION.value, "webus")

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
        platform.overrideSettings = originalSettings
        platform.overrideConfigSettings = originalConfigSettings
    }

    @Test
    fun `bbl search Jesus Christ uses default webus translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(100, it.verses)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search uses configured search result count`() {
        platform.configSettings.putInt(ConfigKey.SEARCH_RESULT.value, 10)
        val backend = RecordingBackendFactory {
            assertEquals(10, it.verses)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search quoted Jesus wept preserves exact phrase`() {
        val backend = RecordingBackendFactory {
            assertEquals("\"Jesus wept\"", it.term)
            assertEquals("webus", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search \"Jesus wept\"")

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
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in kjv")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search with explicit translation and verses does not read config settings`() {
        platform.configSettings.clear()
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(1, it.verses)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in kjv --verses 1")

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
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
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
            assertEquals(Books.bookNumber("john"), it.bookNumber)
            assertEquals(3, it.startChapter)
            assertEquals(null, it.endChapter)
            assertTrue(it.filters.isEmpty())
            assertTrue(it.categoryKeys.isEmpty())
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
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
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 1, startVerse = 1))
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
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 5, startVerse = 1))
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
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 45, chapter = 5, startVerse = 1))
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
            listOf(VersePointer(translation = SupportedTranslation.JC.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in jc")

        assertEquals(0, result.statusCode)
        assertEquals("マタイによる福音書 1:1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n", result.stdout)
    }

    @Test
    fun `bbl search accepts full width spaces in Japanese input method command strings`() {
        val commands = listOf(
            FullWidthSpaceSearchCommand(
                command = "search イエス キリスト in jc",
                term = "イエス キリスト",
                translationCode = "jc",
                bookNumber = null,
                startChapter = null,
                expectedPointer = VersePointer(translation = SupportedTranslation.JC.translation, book = 40, chapter = 1, startVerse = 1),
                expectedOutput = "マタイによる福音書 1:1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n"
            ),
            FullWidthSpaceSearchCommand(
                command = "search イエス キリスト in jc in romans",
                term = "イエス キリスト",
                translationCode = "jc",
                bookNumber = Books.bookNumber("romans"),
                startChapter = null,
                expectedPointer = VersePointer(translation = SupportedTranslation.JC.translation, book = 45, chapter = 1, startVerse = 1),
                expectedOutput = "ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-\n"
            ),
            FullWidthSpaceSearchCommand(
                command = "search イエス キリスト in jc in romans 5",
                term = "イエス キリスト",
                translationCode = "jc",
                bookNumber = Books.bookNumber("romans"),
                startChapter = 5,
                expectedPointer = VersePointer(translation = SupportedTranslation.JC.translation, book = 45, chapter = 5, startVerse = 1),
                expectedOutput = "ローマ人への手紙 5:1 このように、わたしたちは、信仰によって義とされたのだから、わたしたちの主イエス・キリストにより、神に対して平和を得ている。\n"
            ),
            FullWidthSpaceSearchCommand(
                command = "search イエス in jc",
                term = "イエス",
                translationCode = "jc",
                bookNumber = null,
                startChapter = null,
                expectedPointer = VersePointer(translation = SupportedTranslation.JC.translation, book = 40, chapter = 1, startVerse = 1),
                expectedOutput = "マタイによる福音書 1:1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n"
            ),
            FullWidthSpaceSearchCommand(
                command = "search イエス in jc in romans",
                term = "イエス",
                translationCode = "jc",
                bookNumber = Books.bookNumber("romans"),
                startChapter = null,
                expectedPointer = VersePointer(translation = SupportedTranslation.JC.translation, book = 45, chapter = 1, startVerse = 1),
                expectedOutput = "ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-\n"
            )
        )

        val fullWidthSpaceMixedCommandString: List<FullWidthSpaceSearchCommand> =
            commands.flatMap { command ->
                command.fullWidthSpaceCombinations().map { command.copy(command = it) }
            }

        fullWidthSpaceMixedCommandString.forEach { command ->
            val backend = RecordingBackendFactory {
                assertEquals(command.term, it.term, "term for ${command.command}")
                assertEquals(command.translationCode, it.translation.code, "translation for ${command.command}")
                assertEquals(command.bookNumber, it.bookNumber, "book for ${command.command}")
                assertEquals(command.startChapter, it.startChapter, "start chapter for ${command.command}")
                assertEquals(null, it.endChapter, "end chapter for ${command.command}")
                listOf(command.expectedPointer)
            }

            val result = Bbl(bible, searchBackendProvider = backend::backendFor).test(command.command)

            assertEquals(0, result.statusCode, "status for ${command.command}: ${result.stderr}")
            assertEquals(command.expectedOutput, result.stdout, "stdout for ${command.command}")
        }
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

    private data class FullWidthSpaceSearchCommand(
        val command: String,
        val term: String,
        val translationCode: String,
        val bookNumber: Int?,
        val startChapter: Int?,
        val expectedPointer: VersePointer,
        val expectedOutput: String
    ) {
        fun fullWidthSpaceCombinations(): List<String> {
            val separators = command.indices.filter { command[it] == ' ' }
            val combinations = 1 shl separators.size
            return (0 until combinations).map { mask ->
                buildString {
                    command.forEachIndexed { index, char ->
                        val separatorIndex = separators.indexOf(index)
                        append(
                            if (separatorIndex >= 0 && mask and (1 shl separatorIndex) != 0) {
                                '\u3000'
                            } else {
                                char
                            }
                        )
                    }
                }
            }.distinct()
        }
    }

    companion object {
        private val webusSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "webus.40.1.txt" to "1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n",
                "webus.45.1.txt" to "1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,\n",
                "webus.45.5.txt" to "1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;\n",
                "webus$MANIFEST_JSON_POSTFIX" to SupportedTranslation.WEBUS.translation.toJson()
            )
        )

        private val kjvSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "kjv.40.1.txt" to "1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n",
                "kjv.45.1.txt" to "1 Paul, a servant of Jesus Christ, called to be an apostle, separated unto the gospel of God,\n",
                "kjv.45.5.txt" to "1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:\n",
                "kjv$MANIFEST_JSON_POSTFIX" to SupportedTranslation.KJV.translation.toJson()
            )
        )

        private val jcSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "jc.40.1.txt" to "1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n",
                "jc.45.1.txt" to "1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-\n",
                "jc.45.5.txt" to "1 このように、わたしたちは、信仰によって義とされたのだから、わたしたちの主イエス・キリストにより、神に対して平和を得ている。\n",
                "jc$MANIFEST_JSON_POSTFIX" to SupportedTranslation.JC.translation.toJson()
            )
        )
    }
}
