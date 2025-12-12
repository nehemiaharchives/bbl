package org.gnit.bible

import com.russhwolf.settings.Settings
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/**
 * File-backed Settings implementation for JVM using Okio FileSystem.
 * Uses a human-friendly on-disk encoding (`key=value`).
 * Meant to be testable with FakeFileSystem by injecting the fileSystem.
 */
class JvmFileSettings(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    path: Path
) : Settings {

    private val path: Path = path
    private val props = Properties()
    private var loaded = false
    private val lock = Any()

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            if (fileSystem.exists(path)) {
                val text = fileSystem.read(path) { readUtf8() }
                props.load(StringReader(text))
            }
            loaded = true
        }
    }

    private fun persist() {
        synchronized(lock) {
            fileSystem.createDirectories(path.parent ?: ".".toPath())
            val writer = StringWriter()
            // store adds a comment line; acceptable for our simple format
            props.store(writer, null)
            val text = writer.toString()
            fileSystem.write(path) { writeUtf8(text) }
        }
    }

    private fun parseIntStrict(key: String, raw: String): Int =
        raw.toIntOrNull() ?: error("Invalid Int value for key '$key': '$raw'")

    private fun parseLongStrict(key: String, raw: String): Long =
        raw.toLongOrNull() ?: error("Invalid Long value for key '$key': '$raw'")

    private fun parseFloatStrict(key: String, raw: String): Float =
        raw.toFloatOrNull() ?: error("Invalid Float value for key '$key': '$raw'")

    private fun parseDoubleStrict(key: String, raw: String): Double =
        raw.toDoubleOrNull() ?: error("Invalid Double value for key '$key': '$raw'")

    private fun parseBooleanStrict(key: String, raw: String): Boolean =
        raw.toBooleanStrictOrNull() ?: error("Invalid Boolean value for key '$key': '$raw'")

    override val keys: Set<String>
        get() {
            ensureLoaded()
            return synchronized(lock) { props.stringPropertyNames() }
        }

    override val size: Int
        get() {
            ensureLoaded()
            return synchronized(lock) { props.size }
        }

    override fun clear() {
        ensureLoaded()
        synchronized(lock) {
            if (props.isNotEmpty()) {
                props.clear()
                persist()
            }
        }
    }

    override fun remove(key: String) {
        ensureLoaded()
        synchronized(lock) {
            if (props.remove(key) != null) {
                persist()
            }
        }
    }

    override fun hasKey(key: String): Boolean {
        ensureLoaded()
        return synchronized(lock) { props.containsKey(key) }
    }

    override fun putInt(key: String, value: Int) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value.toString())
            persist()
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String): Int? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            parseIntStrict(key, raw)
        }
    }

    override fun putLong(key: String, value: Long) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value.toString())
            persist()
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String): Long? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            parseLongStrict(key, raw)
        }
    }

    override fun putString(key: String, value: String) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value)
            persist()
        }
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            raw
        }
    }

    override fun putFloat(key: String, value: Float) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value.toString())
            persist()
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String): Float? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            parseFloatStrict(key, raw)
        }
    }

    override fun putDouble(key: String, value: Double) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value.toString())
            persist()
        }
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            parseDoubleStrict(key, raw)
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        ensureLoaded()
        synchronized(lock) {
            props.setProperty(key, value.toString())
            persist()
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? {
        ensureLoaded()
        return synchronized(lock) {
            val raw = props.getProperty(key) ?: return@synchronized null
            parseBooleanStrict(key, raw)
        }
    }
}
