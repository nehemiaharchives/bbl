package org.gnit.bible

import okio.fakefilesystem.FakeFileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonFileSettingsTest {

    @Test
    fun readsLegacyConfigJsonShape() {
        val fileSystem = FakeFileSystem()
        val path = "/home/joel/.bbl/config.json".toPath()
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) {
            writeUtf8(
                """
                {
                    "translation": "kjv",
                    "searchResult": 10,
                    "randomlyShow": "chapter"
                }
                """.trimIndent()
            )
        }

        val settings = JsonFileSettings(fileSystem, path)

        assertEquals("kjv", settings.getString(ConfigKey.TRANSLATION.value, ""))
        assertEquals(10, settings.getInt(ConfigKey.SEARCH_RESULT.value, 100))
        assertEquals("chapter", settings.getString(ConfigKey.RANDOMLY_SHOW.value, ""))
    }

    @Test
    fun writesConfigJsonShape() {
        val fileSystem = FakeFileSystem()
        val path = "/home/joel/.bbl/config.json".toPath()
        val settings = JsonFileSettings(fileSystem, path)

        settings.putString(ConfigKey.TRANSLATION.value, "kjv")
        settings.putInt(ConfigKey.SEARCH_RESULT.value, 10)
        settings.putString(ConfigKey.RANDOMLY_SHOW.value, "chapter")

        val json = fileSystem.read(path) { readUtf8() }
        assertTrue(json.contains("\"translation\": \"kjv\""))
        assertTrue(json.contains("\"searchResult\": 10"))
        assertTrue(json.contains("\"randomlyShow\": \"chapter\""))
    }

    @Test
    fun malformedConfigFallsBackToEmptySettings() {
        val fileSystem = FakeFileSystem()
        val path = "/home/joel/.bbl/config.json".toPath()
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { writeUtf8("not json") }

        val settings = JsonFileSettings(fileSystem, path)

        assertEquals("webus", settings.getString(ConfigKey.TRANSLATION.value, "webus"))
        assertEquals(100, settings.getInt(ConfigKey.SEARCH_RESULT.value, 100))
    }
}
