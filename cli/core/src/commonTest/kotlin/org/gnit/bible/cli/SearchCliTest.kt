package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.Path.Companion.toPath
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
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

class SearchCliTest {

    private val testPackDir = "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_kmp_cli_search_test_dir"}"

    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalCacheDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem
    private lateinit var bible: Bible

    @BeforeTest
    fun setup() {
        originalPackDir = platform.overridePlatformPackDir
        originalCacheDir = platform.overridePlatformCacheDir
        originalFileSystem = platform.overrideFileSystem
        platform.overridePlatformPackDir = testPackDir
        platform.overridePlatformCacheDir = null
        platform.overrideFileSystem = null
        platform.settings.remove(ConfigKey.HEADER.value)
        platform.settings.putString(ConfigKey.TRANSLATION.value, "webus")

        val packDirPath = platform.packDir.toPath()
        platform.fileSystem.deleteRecursively(packDirPath, mustExist = false)
        platform.fileSystem.createDirectories(packDirPath)
        platform.fileSystem.write(packDirPath / "webus.zip") { write(webusSearchFixtureZipBytes) }
        platform.fileSystem.write(packDirPath / "kjv.zip") { write(kjvSearchFixtureZipBytes) }
        platform.fileSystem.write(packDirPath / "jc.zip") { write(jcSearchFixtureZipBytes) }

        bible = Bible(
            assetManager = AssetManagerImpl(
                platform = platform,
                fileSystem = platform.fileSystem
            )
        )
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overridePlatformCacheDir = originalCacheDir
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
