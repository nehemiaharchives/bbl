package org.gnit.bible

import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.state.initialBibleStateForAvailableTranslations
import org.gnit.bible.app.state.normalizedForAvailableTranslations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleStateHistoryTest {
    @Test
    fun initialStateUsesAvailableTranslationWhenPreferredLanguageIsNotAvailable() {
        val state = initialBibleStateForAvailableTranslations(
            availableCodes = setOf("jc"),
            preferredLanguageCode = "en"
        )

        assertEquals(SupportedTranslation.JC.translation, state.mainTranslation)
        assertEquals(ReadingMode.SINGLE, state.readingMode)
        assertNull(state.subTranslation)
    }

    @Test
    fun initialStatePrefersAvailableTranslationForCurrentLanguage() {
        val state = initialBibleStateForAvailableTranslations(
            availableCodes = setOf("webus", "jc"),
            preferredLanguageCode = "ja"
        )

        assertEquals(SupportedTranslation.JC.translation, state.mainTranslation)
    }

    @Test
    fun savedStateMainTranslationFallsBackToAvailableTranslation() {
        val state = BibleState(mainTranslation = SupportedTranslation.WEBUS.translation)
            .normalizedForAvailableTranslations(
                availableCodes = setOf("jc"),
                preferredLanguageCode = "en"
            )

        assertEquals(SupportedTranslation.JC.translation, state.mainTranslation)
    }

    @Test
    fun savedBilingualStateDropsUnavailableSubTranslation() {
        val state = BibleState(
            mainTranslation = SupportedTranslation.WEBUS.translation,
            subTranslation = SupportedTranslation.KRV.translation,
            readingMode = ReadingMode.BILINGUAL_SIDE
        ).normalizedForAvailableTranslations(
            availableCodes = setOf("webus"),
            preferredLanguageCode = "en"
        )

        assertEquals(SupportedTranslation.WEBUS.translation, state.mainTranslation)
        assertNull(state.subTranslation)
        assertEquals(ReadingMode.SINGLE, state.readingMode)
    }

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
        assertEquals(
            "cmp read webus ${Books.bookNumber("john")} 11 35",
            state.history.single().command
        )

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
