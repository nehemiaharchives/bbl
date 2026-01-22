package org.gnit.bible.test

import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import kotlin.test.assertContains
import kotlin.test.assertTrue

interface BibleTestBase {
    val bible: Bible

    fun testVerses(){
        val verses = bible.verses(translation = "webus", book = 1, chapter = 1)
        assertTrue(verses.startsWith("1 In the beginning, God created the heavens and the earth."))
    }

    fun testDownloadedVerses(){
        runBlocking {
            bible.assetManager.download("https://gnit.org/bblpacks/kttv.zip", "kttv.zip")
        }
        assertContains(bible.availableTranslationCodes(), "kttv")
        val verses = bible.verses(translation = "kttv", book = 1, chapter = 1)
        assertTrue(verses.startsWith("1 Ban đầu Đức Chúa Trời dựng nên trời đất."))
    }

    fun testListIndexFiles() {
        val downloadedCodes = bible.assetManager.downloadedTranslationCodes()
        downloadedCodes.forEach { translationCode ->
            val indexFiles = bible.obtainZipBibleResourcesReader().listIndexFiles(translationCode)
            assertTrue(indexFiles.isNotEmpty())
        }
    }

    fun testReadIndexFile() {
        bible.assetManager.downloadedTranslationCodes().forEach { translationCode ->
            val indexFiles = bible.obtainZipBibleResourcesReader().listIndexFiles(translationCode)
            indexFiles.forEach { indexFileName ->
                val indexFileBytes = bible.obtainZipBibleResourcesReader().readIndexFile(translationCode, indexFileName)
                assertTrue(indexFileName.isNotBlank(), "Index file name must not be blank")
                assertTrue(!indexFileName.contains('/'), "Index file name must be flat, got: $indexFileName")
                assertTrue(!indexFileName.contains('\\'), "Index file name must be flat, got: $indexFileName")

                assertTrue(indexFileBytes.isNotEmpty(), "Index file must not be empty: $translationCode/$indexFileName")
            }
        }
    }
}
