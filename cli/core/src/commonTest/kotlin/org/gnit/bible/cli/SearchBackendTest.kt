package org.gnit.bible.cli

import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.BblVersion
import org.gnit.bible.InMemorySettings
import org.gnit.bible.Platform
import org.gnit.bible.SupportedTranslation
import org.gnit.bible.Translation
import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchBackendTest : ResourcesTestBase() {

    private lateinit var fakeFs: FakeFileSystem
    private lateinit var bible: Bible
    private lateinit var platform: Platform
    private var originalPackDir: String? = null
    private var originalFileSystem: okio.FileSystem? = null
    private var originalSettings: com.russhwolf.settings.Settings? = null

    @BeforeTest
    fun setup() {
        fakeFs = FakeFileSystem()
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_cli_search_backend_test_dir"
        platform.overrideSettings = InMemorySettings()
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine())
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
    }

    @Test
    fun commonLanguageUsesSearchHelperBackend() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("common")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.WEBUS.translation)
        val request = SearchRequest(
            term = "Jesus",
            translation = SupportedTranslation.WEBUS.translation,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "webus",
            "--verses", "5",
            "Jesus"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendBuildsCommand() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("kuromoji")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.JC.translation)
        val request = SearchRequest(
            term = "grace",
            translation = SupportedTranslation.JC.translation,
            bookNumber = 1,
            startChapter = 1,
            endChapter = null,
            verses = 5,
            categoryKeys = listOf("david")
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "jc",
            "--book", "1",
            "--chapter", "1",
            "--category", "david",
            "--verses", "5",
            "grace"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun sideloadedTranslationUsesLanguageSearchModule() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("morfologik")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )
        val sideloaded = Translation(
            code = "sidepl",
            languageCode = "pl",
            englishName = "Sideloaded Polish",
            nativeName = "Sideloaded Polish",
            year = 2026,
            copyright = "Test"
        )

        val backend = selector.backendFor(sideloaded)
        val request = SearchRequest(
            term = "Jezus",
            translation = sideloaded,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "sidepl",
            "--verses", "5",
            "Jezus"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendSplitsPlainMultiWordSearchTerm() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("common")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.WEBUS.translation)
        val request = SearchRequest(
            term = "Jesus wept",
            translation = SupportedTranslation.WEBUS.translation,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "webus",
            "--verses", "5",
            "Jesus", "wept"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendKeepsQuotedMultiWordSearchTermAsOneArgument() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("common")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.WEBUS.translation)
        val request = SearchRequest(
            term = "\"Jesus wept\"",
            translation = SupportedTranslation.WEBUS.translation,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "webus",
            "--verses", "5",
            "\"Jesus wept\""
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendBuildsCommandWithCategoryOnly() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("kuromoji")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.JC.translation)
        val request = SearchRequest(
            term = "grace",
            translation = SupportedTranslation.JC.translation,
            bookNumber = 1,
            startChapter = 1,
            endChapter = null,
            verses = 5,
            categoryKeys = listOf("david")
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "jc",
            "--book", "1",
            "--chapter", "1",
            "--category", "david",
            "--verses", "5",
            "grace"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendBuildsCommandWithSpacedCategoryKey() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("kuromoji")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.JC.translation)
        val request = SearchRequest(
            term = "grace",
            translation = SupportedTranslation.JC.translation,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5,
            categoryKeys = listOf("johns letters")
        )

        backend.search(request)

        val expected = listOf(
            binaryPath.toString(),
            "-t", "jc",
            "--category", "johns letters",
            "--verses", "5",
            "grace"
        )
        assertEquals(expected, runner.lastCommand)
    }

    @Test
    fun externalBackendErrorIncludesModuleIdAndStderr() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("kuromoji")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, BblVersion.version, "")
            } else {
                ProcessResult(2, "", "boom")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.JC.translation)
        val request = SearchRequest(
            term = "grace",
            translation = SupportedTranslation.JC.translation,
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

    @Test
    fun externalBackendRejectsMismatchedHelperVersion() {
        val binDir = "/tmp/bbl/bin".toPath()
        fakeFs.createDirectories(binDir)
        val binaryPath = binDir / searchHelperName("kuromoji")
        fakeFs.write(binaryPath) { writeUtf8("bin") }

        val runner = FakeProcessRunner { command ->
            if (command.lastOrNull() == "--version") {
                ProcessResult(0, "0.0.1", "")
            } else {
                ProcessResult(0, "ok", "")
            }
        }
        val selector = SearchBackendSelector(
            bible = bible,
            processRunner = runner,
            fileSystem = fakeFs,
            binDirProvider = { binDir }
        )

        val backend = selector.backendFor(SupportedTranslation.JC.translation)
        val request = SearchRequest(
            term = "grace",
            translation = SupportedTranslation.JC.translation,
            bookNumber = null,
            startChapter = null,
            endChapter = null,
            verses = 5
        )

        val error = assertFailsWith<SearchBackendException> {
            backend.search(request)
        }

        val message = error.message ?: ""
        assertTrue(message.contains("version mismatch"))
        assertTrue(message.contains(BblVersion.version))
    }

    private class FakeProcessRunner(
        private val resultProvider: (List<String>) -> ProcessResult = { ProcessResult(0, "", "") }
    ) : ProcessRunner {
        var lastCommand: List<String>? = null

        override fun run(command: List<String>): ProcessResult {
            lastCommand = command
            return resultProvider(command)
        }
    }

    private fun searchHelperName(moduleId: String): String {
        val executableSuffix = if (platform.name == "Windows") ".exe" else ""
        return "bbl-search-$moduleId$executableSuffix"
    }
}
