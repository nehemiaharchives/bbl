package org.gnit.bible

import com.russhwolf.settings.Settings
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class CommonFileSettings(
    private val fileSystem: FileSystem,
    private val path: Path
) : Settings {

    private val values: MutableMap<String, String> = mutableMapOf()
    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        if (fileSystem.exists(path)) {
            parse(fileSystem.read(path) { readUtf8() })
        }
        loaded = true
    }

    private fun persist() {
        fileSystem.createDirectories(path.parent ?: ".".toPath())
        fileSystem.write(path) {
            values.entries
                .sortedBy { it.key }
                .forEach { (key, value) ->
                    writeUtf8(escape(key))
                    writeUtf8("=")
                    writeUtf8(escape(value))
                    writeUtf8("\n")
                }
        }
    }

    private fun parse(contents: String) {
        values.clear()
        contents.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val separator = firstUnescapedEquals(line)
            if (separator < 0) return@forEach
            values[unescape(line.substring(0, separator))] = unescape(line.substring(separator + 1))
        }
    }

    private fun firstUnescapedEquals(line: String): Int {
        var escaped = false
        line.forEachIndexed { index, c ->
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '=' -> return index
            }
        }
        return -1
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("=", "\\=")

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                sb.append(
                    when (next) {
                        'n' -> '\n'
                        '\\' -> '\\'
                        '=' -> '='
                        else -> next
                    }
                )
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
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
            return values.keys.toSet()
        }

    override val size: Int
        get() {
            ensureLoaded()
            return values.size
        }

    override fun clear() {
        ensureLoaded()
        if (values.isNotEmpty()) {
            values.clear()
            persist()
        }
    }

    override fun remove(key: String) {
        ensureLoaded()
        if (values.remove(key) != null) {
            persist()
        }
    }

    override fun hasKey(key: String): Boolean {
        ensureLoaded()
        return values.containsKey(key)
    }

    override fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String): Int? {
        val raw = getStringOrNull(key) ?: return null
        return parseIntStrict(key, raw)
    }

    override fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String): Long? {
        val raw = getStringOrNull(key) ?: return null
        return parseLongStrict(key, raw)
    }

    override fun putString(key: String, value: String) {
        ensureLoaded()
        values[key] = value
        persist()
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? {
        ensureLoaded()
        return values[key]
    }

    override fun putFloat(key: String, value: Float) {
        putString(key, value.toString())
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String): Float? {
        val raw = getStringOrNull(key) ?: return null
        return parseFloatStrict(key, raw)
    }

    override fun putDouble(key: String, value: Double) {
        putString(key, value.toString())
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? {
        val raw = getStringOrNull(key) ?: return null
        return parseDoubleStrict(key, raw)
    }

    override fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? {
        val raw = getStringOrNull(key) ?: return null
        return parseBooleanStrict(key, raw)
    }
}
