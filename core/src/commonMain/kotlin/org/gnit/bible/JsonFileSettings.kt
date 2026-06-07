package org.gnit.bible

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class JsonFileSettings(
    private val fileSystem: FileSystem,
    private val path: Path
) : Settings {

    private val values: MutableMap<String, JsonPrimitive> = mutableMapOf()
    private var loaded = false

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun ensureLoaded() {
        if (loaded) return
        if (fileSystem.exists(path)) {
            runCatching {
                val root = json.parseToJsonElement(fileSystem.read(path) { readUtf8() }).jsonObject
                values.clear()
                root.forEach { (key, element) ->
                    (element as? JsonPrimitive)?.let { values[key] = it }
                }
            }.getOrElse {
                values.clear()
            }
        }
        loaded = true
    }

    private fun persist() {
        fileSystem.createDirectories(path.parent ?: ".".toPath())
        val root: JsonObject = buildJsonObject {
            values.entries.sortedBy { it.key }.forEach { (key, value) ->
                put(key, value)
            }
        }
        fileSystem.write(path) {
            writeUtf8(json.encodeToString(JsonObject.serializer(), root))
            writeUtf8("\n")
        }
    }

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
        ensureLoaded()
        values[key] = JsonPrimitive(value)
        persist()
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String): Int? {
        ensureLoaded()
        val raw = values[key] ?: return null
        return raw.intOrNull ?: raw.contentOrNull?.toIntOrNull()
            ?: error("Invalid Int value for key '$key': '${raw.contentOrNull ?: raw}'")
    }

    override fun putLong(key: String, value: Long) {
        ensureLoaded()
        values[key] = JsonPrimitive(value)
        persist()
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String): Long? {
        ensureLoaded()
        val raw = values[key] ?: return null
        return raw.longOrNull ?: raw.contentOrNull?.toLongOrNull()
            ?: error("Invalid Long value for key '$key': '${raw.contentOrNull ?: raw}'")
    }

    override fun putString(key: String, value: String) {
        ensureLoaded()
        values[key] = when {
            value.toIntOrNull() != null -> JsonPrimitive(value.toInt())
            value.toBooleanStrictOrNull() != null -> JsonPrimitive(value.toBooleanStrict())
            else -> JsonPrimitive(value)
        }
        persist()
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? {
        ensureLoaded()
        return values[key]?.contentOrNull
    }

    override fun putFloat(key: String, value: Float) {
        ensureLoaded()
        values[key] = JsonPrimitive(value)
        persist()
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String): Float? {
        ensureLoaded()
        val raw = values[key] ?: return null
        return raw.floatOrNull ?: raw.contentOrNull?.toFloatOrNull()
            ?: error("Invalid Float value for key '$key': '${raw.contentOrNull ?: raw}'")
    }

    override fun putDouble(key: String, value: Double) {
        ensureLoaded()
        values[key] = JsonPrimitive(value)
        persist()
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? {
        ensureLoaded()
        val raw = values[key] ?: return null
        return raw.doubleOrNull ?: raw.contentOrNull?.toDoubleOrNull()
            ?: error("Invalid Double value for key '$key': '${raw.contentOrNull ?: raw}'")
    }

    override fun putBoolean(key: String, value: Boolean) {
        ensureLoaded()
        values[key] = JsonPrimitive(value)
        persist()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? {
        ensureLoaded()
        val raw = values[key] ?: return null
        return raw.booleanOrNull ?: raw.contentOrNull?.toBooleanStrictOrNull()
            ?: error("Invalid Boolean value for key '$key': '${raw.contentOrNull ?: raw}'")
    }
}
