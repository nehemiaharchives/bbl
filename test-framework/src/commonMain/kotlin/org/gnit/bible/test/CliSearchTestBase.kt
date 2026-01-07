package org.gnit.bible.test

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.ZipBibleResourcesReader
import org.gnit.bible.getPlatform

open class CliSearchTestBase(analyzerProvider: AnalyzerProvider) {

    lateinit var bible: Bible

    open fun setup(){
        val platform = getPlatform()
        platform.overridePlatformPackDir = "../../../server/src/main/resources/files/bblpacks/"

        val zipBibleResourcesReader = ZipBibleResourcesReader(
            platform = platform
        )

        val am = AssetManagerImpl(platform = platform)

        bible = Bible(am)
    }
}