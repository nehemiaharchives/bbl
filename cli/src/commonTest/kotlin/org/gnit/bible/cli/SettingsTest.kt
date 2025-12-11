package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.getPlatform
import org.gnit.bible.jcGenesisChapterOne
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {

    lateinit var fakeFs: FileSystem
    lateinit var bible: Bible

    @BeforeTest
    fun setup() {
        val platform = getPlatform()
        platform.settings.clear()
        platform.settings.putString(ConfigKey.TRANSLATION.value, "jc")
        fakeFs = FakeFileSystem()
        val am = AssetManagerImpl(platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = am)
    }

    @Test
    fun testBblWithDefaultTranslationJc(){
        val command = Bbl(bible = bible)
        val result = command.test("gen 1")
        assertEquals("$jcGenesisChapterOne\n", result.stdout)
    }
}
