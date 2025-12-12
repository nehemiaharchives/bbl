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
        assertEquals("default config file was generated at $bblDir\n", result.stdout)

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
    }
}
