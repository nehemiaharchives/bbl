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

    override val platformSettings: Settings by lazy {
        if(platformContext == null){
            return@lazy InMemorySettings()
        }
        require(platformContext is Context){
            "platformContext must be a android.content.Context"
        }
        SharedPreferencesSettings(platformContext.getSharedPreferences("bbl_settings", Context.MODE_PRIVATE))
    }
}

actual fun getPlatform(platformContext: Any?): Platform{
    return AndroidPlatform(platformContext)
}

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
