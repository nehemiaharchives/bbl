package org.gnit.bible.test

import org.gnit.bible.AssetManager
import org.gnit.bible.Platform

class MockAssetManager() : AssetManager {

    override val platform: Platform = object : Platform() {
        override val name: String
            get() = "Mock Platform"
        override val platformPackDir: String
            get() = TODO("Not yet implemented")
    }

    override fun download(baseUrl: String, fileName: String) {
        TODO("Not yet implemented")
    }

    override fun downloadedTranslations(): List<String> {
        return listOf("kttv")
    }
}
