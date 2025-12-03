package org.gnit.bible.test

import com.russhwolf.settings.Settings
import org.gnit.bible.AssetManager
import org.gnit.bible.Platform
import org.gnit.bible.Translation

class MockAssetManager() : AssetManager {

    override val platform: Platform = object : Platform() {
        override val name: String
            get() = "Mock Platform"
        override val platformPackDir: String
            get() = TODO("Not yet implemented")
        override val settings: Settings
            get() = TODO("Not yet implemented")
    }

    override fun downloadableTranslationList(listUrl: String): List<Translation> {
        TODO("Not yet implemented")
    }

    override fun download(baseUrl: String, fileName: String) {
        TODO("Not yet implemented")
    }

    override fun downloadedTranslationCodes(): List<String> {
        return listOf("kttv")
    }

    override fun delete(translationCode: String) {
        // no-op for tests
    }
}
