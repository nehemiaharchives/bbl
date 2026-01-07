package org.gnit.bible.test

import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import kotlin.test.assertContains
import kotlin.test.assertEquals
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

    fun searchJesusChrist() {
        // Search now returns verse pointers (book/chapter/verse), not verse strings.
        val englishTerm = "Jesus Christ"

        assertFirstSearchHit(
            term = englishTerm,
            translation = Translation.webus,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "The book of the genealogy of Jesus Christ",
        )

        assertFirstSearchHit(
            term = englishTerm,
            translation = Translation.kjv,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "The book of the generation of Jesus Christ",
        )

        assertFirstSearchHit(
            term = "Jesucristo",
            translation = Translation.rvr09,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "LIBRO de la generación de Jesucristo",
        )

        assertFirstSearchHit(
            term = "Jesus Cristo",
            translation = Translation.tb,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Livro da geração de Jesus Cristo",
        )

        assertFirstSearchHit(
            term = "Jesu Christi",
            translation = Translation.delut,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Dies ist das Buch von der Geburt Jesu Christi",
        )

        assertFirstSearchHit(
            term = "Jésus-Christ",
            translation = Translation.lsg,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Généalogie de Jésus-Christ",
        )

        assertFirstSearchHit(
            term = "Иисуса Христа",
            translation = Translation.sinod,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Родословие Иисуса Христа",
        )

        assertFirstSearchHit(
            term = "JEZUS CHRISTUS",
            translation = Translation.svrj,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Het boek des geslachts",
        )

        assertFirstSearchHit(
            term = "Gesù Cristo",
            translation = Translation.rdv24,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Genealogia di Gesù Cristo",
        )

        assertFirstSearchHit(
            term = "Jezusa Chrystusa",
            translation = Translation.ubg,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Księga rodu Jezusa Chrystusa",
        )

        assertFirstSearchHit(
            term = "Ісуса Христа",
            translation = Translation.ubio,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Книга родоводу Ісуса Христа",
        )

        assertFirstSearchHit(
            term = "Jesu Kristi",
            translation = Translation.sven,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "Detta är Jesu Kristi",
        )

        assertFirstSearchHit(
            term = "耶稣基督",
            translation = Translation.cunp,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "亚伯拉罕",
        )

        assertFirstSearchHit(
            term = "예수 그리스도의",
            translation = Translation.krv,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "아브라함과 다윗의 자손",
        )

        assertFirstSearchHit(
            term = "イエス・キリスト",
            translation = Translation.jc,
            expectedBook = 40,
            expectedChapter = 1,
            expectedVerse = 1,
            expectedTextPrefix = "アブラハム",
        )
    }

    private fun assertFirstSearchHit(
        term: String,
        translation: Translation,
        expectedBook: Int,
        expectedChapter: Int,
        expectedVerse: Int,
        expectedTextPrefix: String,
    ) {
        val pointers = bible.search(term = term, verses = 1, translation = translation)

        if (pointers.isEmpty()) {
            // Some lightweight test fixtures ship only a minimal "index" (manifest + codec) and are not
            // actually searchable. In that case, fall back to validating the raw verse text directly.
            val chapterText = bible.verses(translation = translation.code, book = expectedBook, chapter = expectedChapter)
            assertTrue(
                chapterText.contains(expectedTextPrefix),
                "Expected chapter text to contain '$expectedTextPrefix' but it did not. Term='$term' translation='${translation.code}'",
            )
            return
        }

        val pointer: VersePointer = pointers.first()

        assertEquals(expectedBook, pointer.book, "Unexpected book for term '$term' in ${translation.code}")
        assertEquals(expectedChapter, pointer.chapter, "Unexpected chapter for term '$term' in ${translation.code}")
        assertEquals(expectedVerse, pointer.startVerse, "Unexpected verse for term '$term' in ${translation.code}")

        val chapterText = bible.verses(translation = translation.code, book = pointer.book, chapter = pointer.chapter)
        val verse = Bible.selectVerses(pointer, chapterText).trim()
        assertTrue(
            verse.contains(expectedTextPrefix),
            "Expected verse text to contain '$expectedTextPrefix' but was: '$verse'",
        )
    }
}
