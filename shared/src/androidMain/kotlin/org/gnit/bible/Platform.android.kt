package org.gnit.bible

import android.content.Context
import android.os.Build
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File

class AndroidPlatform(val platformContext: Any?) : Platform() {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override val platformPackDir: String by lazy {
        requireNotNull(platformContext){
            "platformContext is required to get packDir"
        }
        require(platformContext is Context){
            "platformContext must be a android.content.Context"
        }
        File(platformContext.filesDir, "$bblDir/$packBaseDir").absolutePath
    }

    override val settings: Settings by lazy {
        if(platformContext == null){
            return@lazy PreviewSettings()
        }
        require(platformContext is Context){
            "platformContext must be a android.content.Context"
        }
        SharedPreferencesSettings(platformContext.getSharedPreferences("bbl_settings", Context.MODE_PRIVATE))
    }
}

class PreviewSettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String>
        get() = values.keys
    override val size: Int
        get() = values.size

    override fun clear() {
        values.clear()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean {
        return values.containsKey(key)
    }

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return (values[key] as? Int) ?: defaultValue
    }

    override fun getIntOrNull(key: String): Int? {
        return values[key] as? Int
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return (values[key] as? Long) ?: defaultValue
    }

    override fun getLongOrNull(key: String): Long? {
        return values[key] as? Long
    }

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String): String {
        return (values[key] as? String) ?: defaultValue
    }

    override fun getStringOrNull(key: String): String? {
        return values[key] as? String
    }

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return (values[key] as? Float) ?: defaultValue
    }

    override fun getFloatOrNull(key: String): Float? {
        return values[key] as? Float
    }

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return (values[key] as? Double) ?: defaultValue
    }

    override fun getDoubleOrNull(key: String): Double? {
        return values[key] as? Double
    }

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return (values[key] as? Boolean) ?: defaultValue
    }

    override fun getBooleanOrNull(key: String): Boolean? {
        return values[key] as? Boolean
    }
}

actual fun getPlatform(platformContext: Any?): Platform{
    return AndroidPlatform(platformContext)
}

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
