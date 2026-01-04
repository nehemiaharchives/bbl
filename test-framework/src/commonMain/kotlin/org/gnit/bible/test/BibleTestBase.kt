package org.gnit.bible.test

import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.embeddedTranslationCodes
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

    fun searchJesusChrist() {
        val englishTerm = "Jesus Christ"
        val webusResult = bible.search(englishTerm, null, null, null, 100, Translation.webus).first().trim()
        assertEquals("Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.", webusResult)

        val kjvResult = bible.search(term = englishTerm, translation = Translation.kjv).first().trim()
        assertEquals("Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.", kjvResult)

        val rvr09Result = bible.search(term = "Jesucristo", translation = Translation.rvr09).first().trim()
        assertEquals("Mateo 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.", rvr09Result)

        val tbResult = bible.search(term = "Jesus Cristo", translation = Translation.tb).first().trim()
        assertEquals("Mateus 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.", tbResult)
    }
}
