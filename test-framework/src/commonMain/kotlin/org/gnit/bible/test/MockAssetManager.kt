package org.gnit.bible.test

import com.russhwolf.settings.Settings
import org.gnit.bible.AssetManager
import org.gnit.bible.Platform
import org.gnit.bible.Translation
import okio.FileSystem
import okio.SYSTEM

class MockAssetManager() : AssetManager {

    override val platform: Platform = object : Platform() {
        override val name: String
            get() = "Mock Platform"
        override val platformBblDirPath: String
            get() = TODO("Not yet implemented")
        override val settings: Settings
            get() = TODO("Not yet implemented")
        override val platformSettings: Settings
            get() = TODO("Not yet implemented")
    }
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override suspend fun download(baseUrl: String, fileName: String) {
        TODO("Not yet implemented")
    }

    override suspend fun downloadTo(baseUrl: String, fileName: String, destinationDir: String) {
        TODO("Not yet implemented")
    }

    override fun downloadedTranslationCodes(): List<String> {
        return listOf("kttv")
    }

    override fun downloadedTranslations(): List<Translation> {
        TODO("Not yet implemented")
    }

    override fun delete(translationCode: String) {
        // no-op for tests
    }
}
