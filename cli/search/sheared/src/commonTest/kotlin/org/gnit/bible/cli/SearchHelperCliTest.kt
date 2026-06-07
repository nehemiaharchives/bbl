package org.gnit.bible.cli

import org.gnit.bible.Bible
import org.gnit.bible.SimpleAnalyzerProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestSearchHelper(
    bible: Bible,
    analyzerProvider: org.gnit.bible.AnalyzerProvider
) : SearchHelperCli(
    searchHelperBinaryName = "test",
    bible = bible,
    analyzerProvider = analyzerProvider
)

class SearchHelperCliTest {
    private val bible = Bible()
    private val analyzerProvider = SimpleAnalyzerProvider()
    private val helper = TestSearchHelper(bible, analyzerProvider)

    @Test
    fun testAnalyzerProvider() {
        val enAnalyzer = helper.analyzerProvider.analyzerFor(org.gnit.bible.Language.en)
        val esAnalyzer = helper.analyzerProvider.analyzerFor(org.gnit.bible.Language.es)
        assertNotNull(enAnalyzer)
        assertNotNull(esAnalyzer)
    }

    @Test
    fun testValidateAndResolveChapterRange_whenNoBookAndNoChapters() {
        val (start, end) = validateChapterRange(null, null, null, 100)
        assertEquals(null, start)
        assertEquals(null, end)
    }

    @Test
    fun testValidateAndResolveChapterRange_whenBookAndChapters() {
        val (start, end) = validateChapterRange(1, 1, 5, 100)
        assertEquals(1, start)
        assertEquals(5, end)
    }

    @Test
    fun testValidateAndResolveChapterRange_whenChapterWithoutBook() {
        try {
            validateChapterRange(null, 1, null, 100)
            assertTrue(false, "Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("Chapter options require --book"))
        }
    }

    @Test
    fun testValidateAndResolveChapterRange_whenEndChapterWithoutBook() {
        try {
            validateChapterRange(null, null, 5, 100)
            assertTrue(false, "Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("Chapter options require --book"))
        }
    }

    @Test
    fun testValidateAndResolveChapterRange_whenEndChapterWithoutStartChapter() {
        try {
            validateChapterRange(1, null, 5, 100)
            assertTrue(false, "Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("--end-chapter requires --chapter"))
        }
    }

    @Test
    fun testValidateAndResolveChapterRange_whenEndChapterLessThanStartChapter() {
        try {
            validateChapterRange(1, 5, 3, 100)
            assertTrue(false, "Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("--end-chapter must be >= --chapter"))
        }
    }

    @Test
    fun testValidateAndResolveChapterRange_whenVersesLessThanOrEqualZero() {
        try {
            validateChapterRange(null, null, null, 0)
            assertTrue(false, "Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("--verses must be > 0"))
        }
    }

    private fun validateChapterRange(
        bookNumber: Int?,
        startChapter: Int?,
        endChapter: Int?,
        verses: Int
    ): Pair<Int?, Int?> {
        if (bookNumber == null && (startChapter != null || endChapter != null)) {
            throw com.github.ajalt.clikt.core.UsageError("Chapter options require --book")
        }
        val start = startChapter
        val end = endChapter
        if (start != null && bookNumber == null) throw com.github.ajalt.clikt.core.UsageError("--chapter requires --book")
        if (end != null && bookNumber == null) throw com.github.ajalt.clikt.core.UsageError("--end-chapter requires --book")
        if (end != null && start == null) throw com.github.ajalt.clikt.core.UsageError("--end-chapter requires --chapter")
        if (start != null && end != null && end < start) throw com.github.ajalt.clikt.core.UsageError("--end-chapter must be >= --chapter")
        if (verses <= 0) throw com.github.ajalt.clikt.core.UsageError("--verses must be > 0")
        return start to end
    }
}