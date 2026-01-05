package org.gnit.bible.cli

import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Language
import org.gnit.bible.Translation
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchBackendTest : ResourcesTestBase() {

    private lateinit var fakeFs: FakeFileSystem
    private lateinit var bible: Bible

    @BeforeTest
    fun setup() {
        fakeFs = FakeFileSystem()
        val platform = createTestPlatform().apply { overrideFileSystem = fakeFs }
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager, analyzerProvider = CommonAnalyzerProvider())
    }

    @Test
    fun commonLanguageUsesInternalBackend() {
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = FakeProcessRunner(),
            fileSystem = fakeFs,
            binDirProvider = { "/tmp/bbl/bin".toPath() }
        )

        val backend = selector.backendFor(Language.en)
        assertTrue(backend is InternalSearchBackend)
    }

    @Test
    fun externalBackendBuildsCommand() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / "bbl-search-kuromoji"
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner(ProcessResult(0, "ok", ""))
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(Language.ja)
        val request = SearchRequest(
            term = "grace",
            translation = Translation.jc,
            bookNumber = 1,
            startChapter = 1,
            endChapter = null,
            verses = 5
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "jc",
            "--book", "1",
            "--chapter", "1",
            "--verses", "5",
            "grace"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendErrorIncludesModuleIdAndStderr() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / "bbl-search-kuromoji"
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner(ProcessResult(2, "", "boom"))
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(Language.ja)
        val request = SearchRequest(
            term = "grace",
            translation = Translation.jc,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        val error = assertFailsWith<SearchBackendException> {
            backend.search(request)
        }

        val message = error.message ?: ""
        assertTrue(message.contains("kuromoji"))
        assertTrue(message.contains("boom"))
    }

    private class FakeProcessRunner(
        private val result: ProcessResult = ProcessResult(0, "", "")
    ) : ProcessRunner {
        var lastCommand: List<String>? = null

        override fun run(command: List<String>): ProcessResult {
            lastCommand = command
            return result
        }
    }
}
