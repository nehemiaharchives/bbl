package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.getPlatform
import org.gnit.bible.jcGenesisChapterOne
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTest {

    private lateinit var fakeFs: FileSystem
    private lateinit var bible: Bible
    private lateinit var settingsPath: Path

    @BeforeTest
    fun setup() {
        val platform = getPlatform()
        fakeFs = FakeFileSystem()
        platform.overrideFileSystem = fakeFs

        // compute settings path using the same layout as Platform implementations
        val packDirPath = platform.packDir.toPath()
        settingsPath = packDirPath.parent!!.resolve("settings.properties")

        val settings = platform.settings
        settings.clear()
        settings.putString(ConfigKey.TRANSLATION.value, "jc")

        val am = AssetManagerImpl(platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = am)
    }

    @Test
    fun testBblWithDefaultTranslationJc() {
        val command = Bbl(bible = bible)
        val result = command.test("gen 1")
        assertEquals("$jcGenesisChapterOne\n", result.stdout)

        // persisted default translation
        assertEquals("jc", bible.assetManager.platform.settings.getString(ConfigKey.TRANSLATION.value, ""))

        // verify FakeFileSystem is used
        assertEquals(fakeFs, bible.assetManager.fileSystem)

        // packs dir untouched
        val packDir = bible.assetManager.platform.packDir.toPath()
        assertTrue(fakeFs.listOrNull(packDir)?.isEmpty() ?: true)

        // settings file exists in fake FS and records the translation
        assertTrue(fakeFs.exists(settingsPath), "settings file should be created")
        val text = fakeFs.read(settingsPath) { readUtf8() }
        assertTrue(
            text.contains("translation=jc"),
            "settings file should record jc translation"
        )
    }
}
