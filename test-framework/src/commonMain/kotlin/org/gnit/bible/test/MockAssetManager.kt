package org.gnit.bible.test

import org.gnit.bible.AssetManager

class MockAssetManager : AssetManager {
    override fun download(baseUrl: String, fileName: String) {
        TODO("Not yet implemented")
    }

    override fun downloadedTranslations(): List<String> {
        return listOf("kttv")
    }
}
