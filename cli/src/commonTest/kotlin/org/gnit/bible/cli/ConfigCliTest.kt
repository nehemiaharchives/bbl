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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigCliTest {

    lateinit var fakeFs: FileSystem
    lateinit var bible: Bible
    lateinit var settingsPath: Path
    lateinit var bblDir: Path

    @BeforeTest
    fun setup(){
        fakeFs = FakeFileSystem()
        val platform = getPlatform().apply { overrideFileSystem = fakeFs }
        platform.settings.clear()

        val packDirPath = platform.packDir.toPath()
        bblDir = packDirPath.parent!!
        settingsPath = bblDir.resolve(SETTINGS_FILE_NAME)

        val am = AssetManagerImpl(platform = platform, fileSystem = fakeFs)
        bible = Bible(am)
    }

    @Test
    fun bllConfigInitTest(){
        val command = Bbl(bible)
        val result = command.test("config init")

        assertEquals(0, result.statusCode, "Command should succeed")
        assertEquals("default config file was generated at $bblDir/$SETTINGS_FILE_NAME\n", result.stdout)

        assertTrue(fakeFs.exists(settingsPath), "settings file should be created")
        val text = fakeFs.read(settingsPath) { readUtf8() }
        assertTrue(
            text.contains("translation=webus"),
            "settings file should record default translation"
        )
        assertTrue(
            text.contains("randomlyShow=verse"),
            "settings file should record default randomlyShow"
        )
        assertTrue(
            text.contains("header=false"),
            "settings file should record default header"
        )
        assertTrue(
            text.contains("border=false"),
            "settings file should record default border"
        )
    }

    @Test
    fun configReadMissingKeyFails() {
        val command = Bbl(bible)
        val result = command.test("config translation")

        assertTrue(result.statusCode != 0, "Command should fail when key is not set")
        assertTrue(result.stderr.contains("is not set"), "Should explain missing config key")
    }

    @Test
    fun configWriteThenReadTranslation() {
        val command = Bbl(bible)
        val write = command.test("config translation jc")
        assertEquals(0, write.statusCode, "Write should succeed")
        assertEquals("", write.stdout)

        val read = command.test("config translation")
        assertEquals(0, read.statusCode, "Read should succeed")
        assertEquals("jc\n", read.stdout)

        assertTrue(fakeFs.exists(settingsPath), "settings file should be created")
        val text = fakeFs.read(settingsPath) { readUtf8() }
        assertTrue(text.contains("translation=jc"), "settings file should record translation")
    }

    @Test
    fun configWriteThenReadBorder() {
        val command = Bbl(bible)
        val write = command.test("config border true")
        assertEquals(0, write.statusCode, "Write should succeed")
        assertEquals("", write.stdout)

        val read = command.test("config border")
        assertEquals(0, read.statusCode, "Read should succeed")
        assertEquals("true\n", read.stdout)

        assertTrue(fakeFs.exists(settingsPath), "settings file should be created")
        val text = fakeFs.read(settingsPath) { readUtf8() }
        assertTrue(text.contains("border=true"), "settings file should record border")
    }

    @Test
    fun configWriteInvalidRandomlyShowFails() {
        val command = Bbl(bible)
        val result = command.test("config randomlyShow invalidValue")
        assertTrue(result.statusCode != 0, "Command should fail on invalid randomlyShow")
        assertTrue(result.stderr.contains("Valid values"), "Should show allowed values")
    }

    @Test
    fun configWriteInvalidBorderFails() {
        val command = Bbl(bible)
        val result = command.test("config border notABoolean")
        assertTrue(result.statusCode != 0, "Command should fail on invalid border")
        assertTrue(result.stderr.contains("Valid values: true, false"), "Should report boolean value requirement")
    }

    @Test
    fun configUnknownKeyFails() {
        val command = Bbl(bible)
        val result = command.test("config doesNotExist")
        assertTrue(result.statusCode != 0, "Command should fail on unknown key")
        assertTrue(result.stderr.contains("Unknown config key"), "Should report unknown key")
    }

    @Test
    fun configWriteInvalidTranslationFails() {
        val command = Bbl(bible)
        val result = command.test("config translation definitelyNotARealTranslation")
        assertTrue(result.statusCode != 0, "Command should fail on invalid translation code")
        assertTrue(result.stderr.contains("Invalid translation code"), "Should report invalid translation code")
    }

    @Test
    fun configWriteDownloadableButNotInstalledTranslationSuggestsInstall() {
        val command = Bbl(bible)
        val result = command.test("config translation ayt")

        assertTrue(result.statusCode != 0, "Command should fail when translation isn't installed")
        assertTrue(result.stderr.contains("bbl install ayt"), "Should suggest installing the translation")
    }
}
