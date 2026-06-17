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
        fakeFs.write(packDirPath / "krv.zip") { write(krvSearchFixtureZipBytes) }

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

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Olivet in kjv jc krv compares multiple translations by block`() {
        platform.configSettings.putString(ConfigKey.COMPARE_BY.value, "block")
        val backend = RecordingBackendFactory {
            assertEquals("Olivet", it.term)
            assertEquals("kjv", it.translation.code)
            listOf(
                VersePointer(translation = SupportedTranslation.KJV.translation, book = 10, chapter = 15, startVerse = 30),
                VersePointer(translation = SupportedTranslation.KJV.translation, book = 44, chapter = 1, startVerse = 12)
            )
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Olivet in kjv jc krv")

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals(olivetComparisonOutput, result.stdout)
    }

    @Test
    fun `bbl search Olivet in kjv jc krv compares multiple translations by verse`() {
        platform.configSettings.putString(ConfigKey.COMPARE_BY.value, "verse")
        val backend = RecordingBackendFactory {
            assertEquals("Olivet", it.term)
            assertEquals("kjv", it.translation.code)
            listOf(
                VersePointer(translation = SupportedTranslation.KJV.translation, book = 10, chapter = 15, startVerse = 30),
                VersePointer(translation = SupportedTranslation.KJV.translation, book = 44, chapter = 1, startVerse = 12)
            )
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Olivet in kjv jc krv")

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals(olivetComparisonOutput, result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in kjv jc krv compares first translation hit across translations`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in kjv jc krv")

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals(
            """
            Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.
            マタイによる福音書 1:1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。
            마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라
            
            """.trimIndent(),
            result.stdout
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans in kjv jc krv searches first translation and compares hits`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 45, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans in kjv jc krv")

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals(
            """
            Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, separated unto the gospel of God,
            ローマ人への手紙 1:1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-
            로마서 1:1 예수 그리스도의 종 바울은 사도로 부르심을 받아 하나님의 복음을 위하여 택정함을 입었으니
            
            """.trimIndent(),
            result.stdout
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans chapter in kjv jc krv keeps chapter filter`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(3, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 45, chapter = 3, startVerse = 22))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 3 in kjv jc krv")

        assertEquals(0, result.statusCode, result.stderr)
        assertEquals(
            """
            Romans 3:22 Even the righteousness of God [which is] by faith of Jesus Christ unto all and upon all them that believe: for there is no difference:
            ローマ人への手紙 3:22 すなわち、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。
            로마서 3:22 곧 예수 그리스도를 믿음으로 말미암아 모든 믿는 자에게 미치는 하나님의 의니 차별이 없느니라
            
            """.trimIndent(),
            result.stdout
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans chapter range in kjv jc krv keeps chapter range filter`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(3, it.startChapter)
            assertEquals(5, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 45, chapter = 5, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 3-5 in kjv jc krv")

        assertEquals(0, result.statusCode)
        assertEquals(
            """
            Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
            ローマ人への手紙 5:1 このように、わたしたちは、信仰によって義とされたのだから、わたしたちの主イエス・キリストにより、神に対して平和を得ている。
            로마서 5:1 그러므로 우리가 믿음으로 의롭다 하심을 얻었은즉 우리 주 예수 그리스도로 말미암아 하나님으로 더불어 화평을 누리자
            
            """.trimIndent(),
            result.stdout
        )
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

    @Test
    fun `bbl search Jesus Christ limit 10 overrides search result count`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals(10, it.verses)
            assertEquals(null, it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ limit 10")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in kjv limit 10 combines limit with translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals("kjv", it.translation.code)
            assertEquals(10, it.verses)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 40, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in kjv limit 10")

        assertEquals(0, result.statusCode)
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans limit 10 combines limit with book`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals(10, it.verses)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(null, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 1, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans limit 10")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans 3 limit 10 combines limit with chapter`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals(10, it.verses)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(3, it.startChapter)
            assertEquals(null, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 3, startVerse = 22))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 3 limit 10")

        assertEquals(0, result.statusCode, "stderr: ${result.stderr}")
        assertEquals("Romans 3:22 Even the righteousness of God which is by faith in Jesus Christ to all and upon all those who believe; for there is no distinction,\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ in romans 5-12 limit 10 combines limit with range`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals(10, it.verses)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 5, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 5-12 limit 10")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 5:1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ limit 10 in romans 5-12 in kjv combines limit with range and translation`() {
        val backend = RecordingBackendFactory {
            assertEquals("Jesus Christ", it.term)
            assertEquals(10, it.verses)
            assertEquals("kjv", it.translation.code)
            assertEquals(Books.bookNumber("romans"), it.bookNumber)
            assertEquals(5, it.startChapter)
            assertEquals(12, it.endChapter)
            listOf(VersePointer(translation = SupportedTranslation.KJV.translation, book = 45, chapter = 5, startVerse = 1))
        }

        val result = Bbl(bible, searchBackendProvider = backend::backendFor).test("search Jesus Christ in romans 5-12 in kjv limit 10")

        assertEquals(0, result.statusCode)
        assertEquals("Romans 5:1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:\n", result.stdout)
    }

    @Test
    fun `bbl search Jesus Christ limit 0 gives error`() {
        val result = Bbl(bible).test("search Jesus Christ limit 0")

        assertTrue(result.statusCode != 0, "Command should reject limit 0")
        assertTrue(
            result.stderr.contains("positive integer"),
            "Should mention positive integer. Got: ${result.stderr}"
        )
    }

    @Test
    fun `bbl search Jesus Christ limit abc gives error`() {
        val result = Bbl(bible).test("search Jesus Christ limit abc")

        assertTrue(result.statusCode != 0, "Command should reject limit abc")
        assertTrue(
            result.stderr.contains("Invalid limit"),
            "Should mention invalid limit. Got: ${result.stderr}"
        )
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
                "webus.45.3.txt" to chapterWithVerse(22, "Even the righteousness of God which is by faith in Jesus Christ to all and upon all those who believe; for there is no distinction,"),
                "webus.45.5.txt" to "1 Being therefore justified by faith, we have peace with God through our Lord Jesus Christ;\n",
                "webus$MANIFEST_JSON_POSTFIX" to SupportedTranslation.WEBUS.translation.toJson()
            )
        )

        private val kjvSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "kjv.40.1.txt" to "1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.\n",
                "kjv.45.1.txt" to "1 Paul, a servant of Jesus Christ, called to be an apostle, separated unto the gospel of God,\n",
                "kjv.45.3.txt" to chapterWithVerse(22, "Even the righteousness of God [which is] by faith of Jesus Christ unto all and upon all them that believe: for there is no difference:"),
                "kjv.45.5.txt" to "1 Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:\n",
                "kjv.10.15.txt" to chapterWithVerse(30, "And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up."),
                "kjv.44.1.txt" to chapterWithVerse(12, "Then returned they unto Jerusalem from the mount called Olivet, which is from Jerusalem a sabbath day's journey."),
                "kjv$MANIFEST_JSON_POSTFIX" to SupportedTranslation.KJV.translation.toJson()
            )
        )

        private val jcSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "jc.40.1.txt" to "1 イエス・キリストの系図である。ダビデの子、アブラハムの子である。\n",
                "jc.45.1.txt" to "1 キリスト・イエスの僕、神の福音のために選び別たれ、召されて使徒となったパウロから-\n",
                "jc.45.3.txt" to chapterWithVerse(22, "すなわち、イエス・キリストを信じる信仰による神の義であって、すべて信じる人に与えられるものである。そこにはなんらの差別もない。"),
                "jc.45.5.txt" to "1 このように、わたしたちは、信仰によって義とされたのだから、わたしたちの主イエス・キリストにより、神に対して平和を得ている。\n",
                "jc.10.15.txt" to chapterWithVerse(30, "ダビデはオリブ山の坂道を登ったが、登る時に泣き、その頭をおおい、はだしで行った。彼と共にいる民もみな頭をおおって登り、泣きながら登った。"),
                "jc.44.1.txt" to chapterWithVerse(12, "それから彼らは、オリブという山を下ってエルサレムに帰った。この山はエルサレムに近く、安息日に許されている距離のところにある。"),
                "jc$MANIFEST_JSON_POSTFIX" to SupportedTranslation.JC.translation.toJson()
            )
        )

        private val krvSearchFixtureZipBytes = ZipUtil.buildMinimalZip(
            listOf(
                "krv.40.1.txt" to "1 아브라함과 다윗의 자손 예수 그리스도의 세계라\n",
                "krv.45.1.txt" to "1 예수 그리스도의 종 바울은 사도로 부르심을 받아 하나님의 복음을 위하여 택정함을 입었으니\n",
                "krv.45.3.txt" to chapterWithVerse(22, "곧 예수 그리스도를 믿음으로 말미암아 모든 믿는 자에게 미치는 하나님의 의니 차별이 없느니라"),
                "krv.45.5.txt" to "1 그러므로 우리가 믿음으로 의롭다 하심을 얻었은즉 우리 주 예수 그리스도로 말미암아 하나님으로 더불어 화평을 누리자\n",
                "krv.10.15.txt" to chapterWithVerse(30, "다윗이 감람산 길로 올라갈 때에 머리를 가리우고 맨발로 울며 행하고 저와 함께 가는 백성들도 각각 그 머리를 가리우고 울며 올라가니라"),
                "krv.44.1.txt" to chapterWithVerse(12, "제자들이 감람원이라 하는 산으로부터 예루살렘에 돌아오니 이 산은 예루살렘에서 가까와 안식일에 가기 알맞은 길이라"),
                "krv$MANIFEST_JSON_POSTFIX" to SupportedTranslation.KRV.translation.toJson()
            )
        )

        private val olivetComparisonOutput = """
            2 Samuel 15:30 And David went up by the ascent of [mount] Olivet, and wept as he went up, and had his head covered, and he went barefoot: and all the people that [was] with him covered every man his head, and they went up, weeping as they went up.
            サムエル記下 15:30 ダビデはオリブ山の坂道を登ったが、登る時に泣き、その頭をおおい、はだしで行った。彼と共にいる民もみな頭をおおって登り、泣きながら登った。
            사무엘하 15:30 다윗이 감람산 길로 올라갈 때에 머리를 가리우고 맨발로 울며 행하고 저와 함께 가는 백성들도 각각 그 머리를 가리우고 울며 올라가니라
            Acts 1:12 Then returned they unto Jerusalem from the mount called Olivet, which is from Jerusalem a sabbath day's journey.
            使徒行伝 1:12 それから彼らは、オリブという山を下ってエルサレムに帰った。この山はエルサレムに近く、安息日に許されている距離のところにある。
            사도행전 1:12 제자들이 감람원이라 하는 산으로부터 예루살렘에 돌아오니 이 산은 예루살렘에서 가까와 안식일에 가기 알맞은 길이라
            
        """.trimIndent()

        private fun chapterWithVerse(verseNumber: Int, verseText: String): String {
            return buildString {
                for (number in 1 until verseNumber) {
                    append(number)
                    append(" fixture verse\n")
                }
                append(verseNumber)
                append(' ')
                append(verseText)
                append('\n')
            }
        }
    }
}
