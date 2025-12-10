package org.gnit.bible

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosixSettingsTest {
    private val realFs: FileSystem = FileSystem.SYSTEM
    private val baseDir = "/tmp/bbl_kmp_shared_posix_settings_test_dir".toPath()

    private fun freshPath() = run {
        realFs.createDirectories(baseDir)
        baseDir / "settings-${Random.nextLong().toString(16)}.properties"
    }

    @Test
    fun roundTripPersistsValues() {
        val path = freshPath()
        val settings = PosixSettings(fileSystem = realFs, path = path)

        settings.putString("name", "alice")
        settings.putInt("age", 7)
        settings.putBoolean("enabled", true)
        settings.putDouble("pi", 3.14159)

        // New instance should read persisted values
        val reloaded = PosixSettings(fileSystem = realFs, path = path)
        assertEquals("alice", reloaded.getString("name", ""))
        assertEquals(7, reloaded.getInt("age", 0))
        assertTrue(reloaded.getBoolean("enabled", false))
        assertEquals(3.14159, reloaded.getDouble("pi", 0.0))
        assertEquals(setOf("name", "age", "enabled", "pi"), reloaded.keys)
        assertEquals(4, reloaded.size)
    }

    @Test
    fun escapesKeysAndValues() {
        val path = freshPath()
        val trickyKey = "user=name\\nwith=equals"
        val trickyValue = "line1\nline2=\\backslash\\"
        val settings = PosixSettings(fileSystem = realFs, path = path)

        settings.putString(trickyKey, trickyValue)

        val reloaded = PosixSettings(fileSystem = realFs, path = path)
        assertEquals(trickyValue, reloaded.getString(trickyKey, ""))
        assertTrue(reloaded.hasKey(trickyKey))
    }

    @Test
    fun removeAndClearFlushToDisk() {
        val path = freshPath()
        val settings = PosixSettings(fileSystem = realFs, path = path)
        settings.putString("k1", "v1")
        settings.putString("k2", "v2")

        settings.remove("k1")
        val afterRemove = PosixSettings(fileSystem = realFs, path = path)
        assertFalse(afterRemove.hasKey("k1"))
        assertEquals("v2", afterRemove.getString("k2", ""))

        afterRemove.clear()
        val afterClear = PosixSettings(fileSystem = realFs, path = path)
        assertEquals(0, afterClear.size)
    }
}
