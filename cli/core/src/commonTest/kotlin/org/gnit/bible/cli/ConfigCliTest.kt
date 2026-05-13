package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.SETTINGS_FILE_NAME
import org.gnit.bible.getPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ConfigCliTest {

    lateinit var fakeFs: FileSystem
    lateinit var bible: Bible
    lateinit var settingsPath: Path
    lateinit var bblDir: Path
    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem

    @BeforeTest
    fun setup(){
        fakeFs = FakeFileSystem()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_kmp_cli_config_test_dir"
        platform.settings.clear()

        val packDirPath = platform.packDir.toPath()
        bblDir = packDirPath.parent!!
        settingsPath = bblDir.resolve(SETTINGS_FILE_NAME)

        val am = AssetManagerImpl(platform = platform, fileSystem = fakeFs)
        bible = Bible(am)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
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
    fun configWriteInvalidRandomlyShowFails() {
        val command = Bbl(bible)
        val result = command.test("config randomlyShow invalidValue")
        assertTrue(result.statusCode != 0, "Command should fail on invalid randomlyShow")
        assertTrue(result.stderr.isNotBlank(), "Should show allowed values")
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
    fun configWriteDownloadableButNotInstalledTranslationSuggestsInstall() {
        val command = Bbl(bible)
        val result = command.test("config translation ayt")

        assertTrue(result.statusCode != 0, "Command should fail when translation isn't installed")
        assertTrue(
            result.stderr.contains("bbl install", ignoreCase = true),
            "Should suggest installing the translation. Got: ${result.stderr}"
        )
    }
}
