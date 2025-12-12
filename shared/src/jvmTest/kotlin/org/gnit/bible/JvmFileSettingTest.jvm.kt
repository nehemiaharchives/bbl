package org.gnit.bible

import okio.fakefilesystem.FakeFileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmFileSettingTest {

    private fun freshPath(): Path =
        "/tmp/bbl_kmp_shared_jvm_file_settings_test_${Random.nextLong().toString(16)}/settings.properties".toPath()

    @Test
    fun roundTripPersistsValues() {
        val fs = FakeFileSystem()
        val path = freshPath()

        val settings = JvmFileSettings(fileSystem = fs, path = path)
        settings.putString("name", "alice")
        settings.putInt("age", 7)
        settings.putBoolean("enabled", true)
        settings.putDouble("pi", 3.14159)

        val reloaded = JvmFileSettings(fileSystem = fs, path = path)
        assertEquals("alice", reloaded.getString("name", ""))
        assertEquals(7, reloaded.getInt("age", 0))
        assertTrue(reloaded.getBoolean("enabled", false))
        assertEquals(3.14159, reloaded.getDouble("pi", 0.0))
        assertEquals(setOf("name", "age", "enabled", "pi"), reloaded.keys)
        assertEquals(4, reloaded.size)
    }

    @Test
    fun persistsPlainHumanFriendlyValues() {
        val fs = FakeFileSystem()
        val path = freshPath()

        val settings = JvmFileSettings(fileSystem = fs, path = path)
        settings.putString("translation", "webus")
        settings.putString("randomlyShow", "verse")
        settings.putInt("fontSize", 16)
        settings.putBoolean("enabled", true)

        assertTrue(fs.exists(path), "settings file should be created")
        val text = fs.read(path) { readUtf8() }
        assertTrue(text.contains("translation=webus"))
        assertTrue(text.contains("randomlyShow=verse"))
        assertTrue(text.contains("fontSize=16"))
        assertTrue(text.contains("enabled=true"))

        assertFalse(text.contains("=s:"))
        assertFalse(text.contains("=i:"))
        assertFalse(text.contains("=l:"))
        assertFalse(text.contains("=f:"))
        assertFalse(text.contains("=d:"))
        assertFalse(text.contains("=b:"))
    }

    @Test
    fun removeAndClearPersist() {
        val fs = FakeFileSystem()
        val path = freshPath()

        val settings = JvmFileSettings(fileSystem = fs, path = path)
        settings.putString("k1", "v1")
        settings.putString("k2", "v2")

        settings.remove("k1")
        val afterRemove = JvmFileSettings(fileSystem = fs, path = path)
        assertFalse(afterRemove.hasKey("k1"))
        assertEquals("v2", afterRemove.getString("k2", ""))

        afterRemove.clear()
        val afterClear = JvmFileSettings(fileSystem = fs, path = path)
        assertEquals(0, afterClear.size)
    }

    @Test
    fun strictParsingThrowsWhenInvalidValuePresent() {
        val fs = FakeFileSystem()
        val path = freshPath()
        fs.createDirectories(path.parent!!)
        fs.write(path) { writeUtf8("age=notAnInt\n") }

        val settings = JvmFileSettings(fileSystem = fs, path = path)
        assertFailsWith<IllegalStateException> {
            settings.getIntOrNull("age")
        }
    }

    @Test
    fun createsMissingParentDirectory() {
        val fs = FakeFileSystem()
        val path = "/tmp/bbl_kmp_shared_jvm_file_settings_missing_${Random.nextLong().toString(16)}/nested/settings.properties".toPath()

        val settings = JvmFileSettings(fileSystem = fs, path = path)
        settings.putString("key", "value")

        assertTrue(fs.exists(path.parent!!))
        val reloaded = JvmFileSettings(fileSystem = fs, path = path)
        assertEquals("value", reloaded.getString("key", ""))
    }
}
