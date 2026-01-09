package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.SETTINGS_FILE_NAME
import org.gnit.bible.getPlatform
import org.gnit.bible.test.TestFixtures
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTest {

    private lateinit var systemFs: FileSystem
    private lateinit var bible: Bible
    private lateinit var settingsPath: Path
    private val platform = getPlatform()
    private var originalPackDir: String? = null
    private var originalCacheDir: String? = null
    private var originalFileSystem = platform.overrideFileSystem

    @BeforeTest
    fun setup() {
        originalPackDir = platform.overridePlatformPackDir
        originalCacheDir = platform.overridePlatformCacheDir
        originalFileSystem = platform.overrideFileSystem
        // Integration-like: ZipBibleResourcesReader reads real zip files from the OS filesystem.
        systemFs = FileSystem.SYSTEM
        platform.overrideFileSystem = null
        platform.overridePlatformPackDir = "/tmp/bbl_kmp_cli_settings_test_dir"
        platform.overridePlatformCacheDir = null

        // compute settings path using the same layout as Platform implementations
        val packDirPath = platform.packDir.toPath()
        settingsPath = packDirPath.parent!!.resolve(SETTINGS_FILE_NAME)

        // install a minimal JC pack into the temp pack dir (CLI no longer embeds packs)
        systemFs.createDirectories(packDirPath)
        systemFs.write(packDirPath / "jc.zip") { write(TestFixtures.jcMinimalZipBytes) }

        val settings = platform.settings
        settings.clear()
        settings.putString(ConfigKey.TRANSLATION.value, "jc")

        val am = AssetManagerImpl(platform = platform, fileSystem = systemFs)
        bible = Bible(assetManager = am)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overridePlatformCacheDir = originalCacheDir
        platform.overrideFileSystem = originalFileSystem
    }

    @Test
    fun testBblWithDefaultTranslationJc() {
        val command = Bbl(bible = bible)
        val result = command.test("gen 1")

        // The pack fixture only contains Genesis 1:1, so `bbl gen 1` should print that only.
        assertEquals("${TestFixtures.JC_GENESIS_1_1}\n", result.stdout)

        // persisted default translation
        assertEquals("jc", bible.assetManager.platform.settings.getString(ConfigKey.TRANSLATION.value, ""))

        // verify FakeFileSystem is used
        assertEquals(systemFs, bible.assetManager.fileSystem)

        // jc pack is installed
        val packDir = bible.assetManager.platform.packDir.toPath()
        assertTrue(systemFs.exists(packDir / "jc.zip"))

        // NOTE: We don't assert the physical settings file exists here.
        // Platform.settings is platform-specific and not necessarily backed by FileSystem.
    }
}
