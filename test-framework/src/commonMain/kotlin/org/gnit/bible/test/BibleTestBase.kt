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

        val delutResult = bible.search(term = "Jesu Christi", translation = Translation.delut).first().trim()
        assertEquals("Matthäus 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.", delutResult)

        val lsgResult = bible.search(term = "Jésus-Christ", translation = Translation.lsg).first().trim()
        assertEquals("Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d'Abraham.", lsgResult)

        val sinodResult = bible.search(term = "Иисуса Христа", translation = Translation.sinod).first().trim()
        assertEquals("От Матфея святое благовествование 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.", sinodResult)

        val svrjResult = bible.search(term = "JEZUS CHRISTUS", translation = Translation.svrj).first().trim()
        assertEquals("MATTHEÜS 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.", svrjResult)

        val rdv24Result = bible.search(term = "Gesù Cristo", translation = Translation.rdv24).first().trim()
        assertEquals("Matteo 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d'Abramo.", rdv24Result)

        val ubgResult = bible.search(term = "Jezusa Chrystusa", translation = Translation.ubg).first().trim()
        assertEquals("Mateusza 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.", ubgResult)

        val ubioResult = bible.search(term = "Ісуса Христа", translation = Translation.ubio).first().trim()
        assertEquals("Вiд Матвiя 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:", ubioResult)

        val svenResult = bible.search(term = "Jesu Kristi", translation = Translation.sven).first().trim()
        assertEquals("Matteus 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.", svenResult)

        val cunpResult = bible.search(term = "耶稣基督", translation = Translation.cunp).first().trim()
        assertEquals("马太福音 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：", cunpResult)

        val krvResult = bible.search(term = "예수 그리스도의", translation = Translation.krv).first().trim()
        assertEquals("마태복음 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라", krvResult)

        val jcResult = bible.search(term = "イエス・キリスト", translation = Translation.jc).first().trim()
        assertEquals("マタイによる福音書 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。", jcResult)
    }
}
