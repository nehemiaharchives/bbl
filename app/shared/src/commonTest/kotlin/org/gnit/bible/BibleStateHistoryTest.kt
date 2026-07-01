package org.gnit.bible

import org.gnit.bible.app.state.BibleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleStateHistoryTest {
    @Test
    fun backClosesEmptySearch() {
        val state = BibleState().startSearch()

        val actual = assertNotNull(state.handleBack())

        assertFalse(actual.isSearchActive)
        assertEquals("", actual.searchQuery)
        assertNull(actual.submittedSearchQuery)
    }

    @Test
    fun backClosesSearchWithTypedQuery() {
        val state = BibleState()
            .startSearch()
            .copy(searchQuery = "sake")

        val actual = assertNotNull(state.handleBack())

        assertFalse(actual.isSearchActive)
        assertEquals("", actual.searchQuery)
        assertNull(actual.submittedSearchQuery)
    }

    @Test
    fun backClosesSearchResults() {
        val state = BibleState().submitSearch("Jesus wept")

        val actual = assertNotNull(state.handleBack())

        assertFalse(actual.isSearchActive)
        assertEquals("", actual.searchQuery)
        assertNull(actual.submittedSearchQuery)
    }

    @Test
    fun backAfterSearchResultClickRestoresSearchResults() {
        val webus = SupportedTranslation.WEBUS.translation
        val state = BibleState(mainTranslation = webus)
            .submitSearch("Jesus wept")
            .openSearchResult(VersePointer(webus, Books.bookNumber("john"), 11, 35))

        assertFalse(state.isSearchActive)
        assertEquals(Books.bookNumber("john"), state.book)
        assertEquals(11, state.chapter)
        assertEquals(35, state.centerVerse)

        val actual = assertNotNull(state.handleBack())

        assertTrue(actual.isSearchActive)
        assertEquals("Jesus wept", actual.searchQuery)
        assertEquals("Jesus wept", actual.submittedSearchQuery)
    }

    @Test
    fun recordReadHistoryStoresVerseAndHighlight() {
        val state = BibleState(book = Books.bookNumber("john"), chapter = 11)
            .recordReadHistory(35)

        assertEquals(35, state.highlightedVerse)
        assertNull(state.centerVerse)
        assertEquals(1, state.history.size)
        assertEquals(0, state.backStack.size)
        assertEquals("cmp read webus ${Books.bookNumber("john")} 11 35", state.history.single().command)

        val cleared = state.clearHistoryHighlight(35)
        assertNull(cleared.highlightedVerse)
    }

    @Test
    fun backAfterTwoReadHistoryRecordsRestoresPreviousDoubleTappedReadLocation() {
        val state = BibleState(book = Books.bookNumber("genesis"), chapter = 2)
            .recordReadHistory(1)
            .copy(chapter = 3, scrollPercent = 0f, centerVerse = null)
            .recordReadHistory(1)

        assertEquals(2, state.history.size)
        assertEquals(1, state.backStack.size)

        val actual = assertNotNull(state.handleBack())

        assertEquals(Books.bookNumber("genesis"), actual.book)
        assertEquals(2, actual.chapter)
        assertEquals(1, actual.centerVerse)
        assertFalse(actual.isSearchActive)
    }
}
