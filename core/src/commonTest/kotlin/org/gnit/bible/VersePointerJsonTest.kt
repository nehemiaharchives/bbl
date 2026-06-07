package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals

class VersePointerJsonTest {

    @Test
    fun encodesAndDecodesVersePointerList() {
        val hits = listOf(
            VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 1, startVerse = 1),
            VersePointer(translation = SupportedTranslation.WEBUS.translation, book = 45, chapter = 2, startVerse = 16)
        )

        val encoded = VersePointerJson.encodeList(hits)

        assertEquals(hits, VersePointerJson.decodeList(encoded))
    }
}
