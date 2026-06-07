package org.gnit.bible.cli


import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.LoggingSetup
import org.gnit.bible.SearchQueryText
import org.gnit.bible.Books
import org.gnit.bible.BblVersion
import org.gnit.bible.CONFIG_FILE_NAME
import org.gnit.bible.ConfigKey
import org.gnit.bible.InMemorySettings
import org.gnit.bible.getPlatform
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
    private var originalFileSystem = platform.overrideFileSystem
    private var originalSettings = platform.overrideSettings
    private var originalConfigSettings = platform.overrideConfigSettings

    @BeforeTest
    fun setup() {
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        originalConfigSettings = platform.overrideConfigSettings
        // Integration-like: ZipBibleResourcesReader reads real zip files from the OS filesystem.
        systemFs = FileSystem.SYSTEM
        platform.overrideFileSystem = null
        platform.overridePlatformPackDir = "/tmp/bbl_cli_settings_test_dir"
        platform.overrideSettings = InMemorySettings()
        platform.overrideConfigSettings = null

        // compute settings path using the same layout as Platform implementations
        val packDirPath = platform.packDir.toPath()
        settingsPath = packDirPath.parent!!.resolve(CONFIG_FILE_NAME)

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
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
        platform.overrideConfigSettings = originalConfigSettings
    }

    @Test
    fun testBblWithDefaultTranslationJc() {
        val command = Bbl(bible = bible)
        val result = command.test("gen 1")

        // The pack fixture only contains Genesis 1:1, so `bbl gen 1` should print that only.
        assertEquals("${TestFixtures.genesisOneJc}\n", result.stdout)

        // persisted default translation
        assertEquals("jc", bible.assetManager.platform.configSettings.getString(ConfigKey.TRANSLATION.value, ""))

        // verify FakeFileSystem is used
        assertEquals(systemFs, bible.assetManager.fileSystem)

        // jc pack is installed
        val packDir = bible.assetManager.platform.packDir.toPath()
        assertTrue(systemFs.exists(packDir / "jc.zip"))

        // NOTE: We don't assert the physical settings file exists here.
        // Platform.settings is platform-specific and not necessarily backed by FileSystem.
    }
}
