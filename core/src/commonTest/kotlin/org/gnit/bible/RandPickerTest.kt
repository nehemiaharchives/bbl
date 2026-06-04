package org.gnit.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandPickerTest {

    private fun numbered(vararg lines: String): String =
        lines.mapIndexed { idx, text -> "${idx + 1} $text" }.joinToString("\n")

    private fun pickerWith(
        chooserValues: List<Int>
    ): RandPicker {
        val chapters = mapOf(
            (1 to 1) to webusGenesisChapterOne,
            (43 to 3) to webusJohnChapter3,
            (45 to 1) to numbered("Paul to the Romans."),
            (23 to 1) to numbered("Prophet intro."),
            (1 to 15) to numbered("After these things the word of the Lord came to Abram.")
        )

        val chooser = QueueChooser(chooserValues)

        return RandPicker(
            readChapter = { _, book, chapter ->
                chapters[book to chapter] ?: error("chapter not stubbed for book=$book chapter=$chapter")
            },
            selector = { range -> chooser(range) }
        )
    }

    @Test
    fun randomFromNt() {
        val picker = pickerWith(listOf(43, 3, 16))
        val result = picker.random(filter = Books.Category.NEW_TESTAMENT.filter, randomlyShow = RandomlyShow.verse)
        assertEquals(43, result.pointer.book)
        assertEquals(3, result.pointer.chapter)
        assertTrue(result.selection.contains("God so loved the world"))
    }

    @Test
    fun randomFromOt() {
        val picker = pickerWith(listOf(1, 1, 1))
        val result = picker.random(filter = Books.Category.OLD_TESTAMENT.filter, randomlyShow = RandomlyShow.verse)
        assertEquals(1, result.pointer.book)
        assertEquals(1, result.pointer.chapter)
        assertTrue(result.selection.startsWith("In the beginning"))
    }

    @Test
    fun randomFromProphets() {
        val picker = pickerWith(listOf(23, 1, 1))
        val result = picker.random(filter = Books.Category.PROPHETS.filter, randomlyShow = RandomlyShow.verse)
        assertTrue(result.pointer.book in 23..39)
        assertEquals("Prophet intro.", result.selection)
    }

    @Test
    fun randomFromPauline() {
        val picker = pickerWith(listOf(45, 1, 1))
        val result = picker.random(filter = Books.Category.PAULINE_EPISTLES.filter, randomlyShow = RandomlyShow.verse)
        assertTrue(result.pointer.book in 45..57)
        assertEquals("Paul to the Romans.", result.selection)
    }

    @Test
    fun randomFromAbrahamPassage() {
        val picker = pickerWith(listOf(1, 15, 1))
        val result = picker.random(filter = Books.Category.ABRAHAM.filter, randomlyShow = RandomlyShow.verse)
        assertEquals(1, result.pointer.book)
        assertTrue(result.pointer.chapter in 11..25)
        assertEquals("After these things the word of the Lord came to Abram.", result.selection)
    }
}

private class QueueChooser(values: List<Int>) : (IntRange) -> Int {
    private val queue = ArrayDeque(values)
    override fun invoke(range: IntRange): Int {
        val next = queue.removeFirst()
        return next.coerceIn(range)
    }
}
