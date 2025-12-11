package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandCliTest {

    // New Testament book names (books 40-66)
    private val ntBooks = listOf(
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans",
        "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
        "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    )

    // Old Testament book names (books 1-39)
    private val otBooks = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1st Samuel", "2nd Samuel",
        "1st Kings", "2nd Kings", "1st Chronicles", "2nd Chronicles",
        "Ezra", "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
        "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah",
        "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah",
        "Haggai", "Zechariah", "Malachi"
    )

    // Prophet book names (books 23-39)
    private val prophetBooks = listOf(
        "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel",
        "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah",
        "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi"
    )

    // Pauline Epistles book names (books 45-57)
    private val paulBooks = listOf(
        "Romans", "1 Corinthians", "2 Corinthians", "Galatians",
        "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
        "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon"
    )

    @Test
    fun `bbl rand nt`() {
        val command = Bbl()
        val result = command.test(listOf("rand", "nt"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(
            ntBooks.any { firstLine.startsWith(it) },
            "Output should start with a New Testament book name. Got: $firstLine"
        )
        // Header should contain chapter:verse pattern (e.g., "John 3:16")
        assertTrue(
            firstLine.matches(Regex(".+ \\d+:\\d+")),
            "Header should match 'BookName chapter:verse' pattern. Got: $firstLine"
        )
    }

    @Test
    fun `bbl rand ot`() {
        val command = Bbl()
        val result = command.test(listOf("rand", "ot"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(
            otBooks.any { firstLine.startsWith(it) },
            "Output should start with an Old Testament book name. Got: $firstLine"
        )
        assertTrue(
            firstLine.matches(Regex(".+ \\d+:\\d+")),
            "Header should match 'BookName chapter:verse' pattern. Got: $firstLine"
        )
    }

    @Test
    fun `bbl rand prophets`() {
        val command = Bbl()
        val result = command.test(listOf("rand", "prophets"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(
            prophetBooks.any { firstLine.startsWith(it) },
            "Output should start with a Prophet book name. Got: $firstLine"
        )
        assertTrue(
            firstLine.matches(Regex(".+ \\d+:\\d+")),
            "Header should match 'BookName chapter:verse' pattern. Got: $firstLine"
        )
    }

    @Test
    fun `bbl rand paul`() {
        val command = Bbl()
        val result = command.test(listOf("rand", "paul"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        assertTrue(
            paulBooks.any { firstLine.startsWith(it) },
            "Output should start with a Pauline Epistle book name. Got: $firstLine"
        )
        assertTrue(
            firstLine.matches(Regex(".+ \\d+:\\d+")),
            "Header should match 'BookName chapter:verse' pattern. Got: $firstLine"
        )
    }

    @Test
    fun `bbl rand abraham`() {
        val command = Bbl()
        val result = command.test(listOf("rand", "abraham"))

        assertEquals(0, result.statusCode, "Command should succeed")
        assertTrue(result.stdout.isNotBlank(), "Output should not be empty")

        val firstLine = result.stdout.lines().first()
        // Abraham passage is Genesis 11:27 to 25:11
        assertTrue(
            firstLine.startsWith("Genesis"),
            "Output should start with 'Genesis' for Abraham passage. Got: $firstLine"
        )
        assertTrue(
            firstLine.matches(Regex("Genesis \\d+:\\d+")),
            "Header should match 'Genesis chapter:verse' pattern. Got: $firstLine"
        )
        // Verify chapter is within Abraham's story range (chapters 11-25)
        val chapterMatch = Regex("Genesis (\\d+):\\d+").find(firstLine)
        val chapter = chapterMatch?.groupValues?.get(1)?.toIntOrNull()
        assertTrue(
            chapter != null && chapter in 11..25,
            "Chapter should be between 11 and 25 for Abraham passage. Got chapter: $chapter"
        )
    }
}
