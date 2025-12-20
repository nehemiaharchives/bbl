package org.gnit.bible.test

import org.gnit.bible.Bible
import kotlinx.coroutines.runBlocking
import org.gnit.bible.SearchEngine
import org.gnit.bible.Translation
import org.gnit.bible.embeddedTranslationCodes
import org.gnit.lucenekmp.index.StandardDirectoryReader
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface BibleTestBase {
    abstract val bible: Bible

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
        embeddedTranslationCodes.forEach { translationCode ->
            val indexFiles = bible.bibleResourcesReader.listIndexFiles(translationCode)
            assertTrue(indexFiles.isNotEmpty())
        }
    }

    fun testReadIndexFile() {
        val luceneCodecMagic = byteArrayOf(0x3f, 0xd7.toByte(), 0x6c, 0x17)
        embeddedTranslationCodes.forEach { translationCode ->
            val indexFiles = bible.bibleResourcesReader.listIndexFiles(translationCode)
            indexFiles.forEach { indexFileName ->
                val indexFileBytes = bible.bibleResourcesReader.readIndexFile(translationCode, indexFileName)
                assertTrue(indexFileName.isNotBlank(), "Index file name must not be blank")
                assertTrue(!indexFileName.contains('/'), "Index file name must be flat, got: $indexFileName")
                assertTrue(!indexFileName.contains('\\'), "Index file name must be flat, got: $indexFileName")

                assertTrue(indexFileBytes.isNotEmpty(), "Index file must not be empty: $translationCode/$indexFileName")
                assertTrue(
                    indexFileBytes.size >= luceneCodecMagic.size,
                    "Index file is too small: $translationCode/$indexFileName (${indexFileBytes.size} bytes)"
                )
                assertTrue(
                    indexFileBytes.copyOfRange(0, luceneCodecMagic.size).contentEquals(luceneCodecMagic),
                    "Index file does not look like a Lucene codec file (CODEC_MAGIC mismatch): $translationCode/$indexFileName"
                )
            }
        }
    }


    fun searchJesusChristInWebus() {
        val term = "Jesus Christ"
        val result = bible.search(term, null, null, null, 100, Translation.webus)
        val actual = result.first().trim()

        println("search result for term: $term: $actual")

        assertEquals(
            "Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.",
            actual
        )
    }
}
