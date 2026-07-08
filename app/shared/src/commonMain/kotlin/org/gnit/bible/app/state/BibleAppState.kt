package org.gnit.bible.app.state

import org.gnit.bible.SupportedTranslation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import com.vanniktech.locale.Languages
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gnit.bible.Books
import org.gnit.bible.HistoryRecord
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.app.EmbeddedPackRegistry
import org.gnit.bible.app.currentAssetManager
import org.gnit.bible.app.currentPlatform
import org.gnit.bible.app.logger

enum class ReadingMode { SINGLE, BILINGUAL_SIDE, BILINGUAL_UNDER }

@Serializable
data class BibleState(
    val mainTranslation: Translation = SupportedTranslation.WEBUS.translation,
    val subTranslation: Translation? = null,
    val readingMode: ReadingMode = ReadingMode.SINGLE,
    val book: Int = 1,
    val chapter: Int = 1,
    val fontSize: Int = 16,
    val scrollPercent: Float = 0f,
    val centerVerse: Int? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val submittedSearchQuery: String? = null,
    val isZebraBackground: Boolean = false,
    val spaceBetweenVerses: Int = SPACE_BETWEEN_VERSES_MIN,
    val isFontFamilySerif: Boolean = true,
    val highlightedVerse: Int? = null,
    val history: List<HistoryRecord> = emptyList(),
    val backStack: List<HistoryRecord> = emptyList(),
    val translationVisibility: Map<String, Boolean> = mapOf(
        SupportedTranslation.WEBUS.translation.code to true,
        SupportedTranslation.KJV.translation.code to true
    )
) {
    fun prevBook() = copy(book = book - 1, chapter = 1, scrollPercent = 0f, centerVerse = null)
    fun nextBook() = copy(book = book + 1, chapter = 1, scrollPercent = 0f, centerVerse = null)
    fun changeBook(newBook: Int) =
        copy(book = newBook, chapter = 1, scrollPercent = 0f, centerVerse = null)

    fun prevChapter() = copy(chapter = chapter - 1, scrollPercent = 0f, centerVerse = null)
    fun nextChapter() = copy(chapter = chapter + 1, scrollPercent = 0f, centerVerse = null)
    fun startSearch() = copy(isSearchActive = true, searchQuery = "", submittedSearchQuery = null)
    fun submitSearch(query: String) =
        copy(isSearchActive = true, searchQuery = query, submittedSearchQuery = query)

    fun clearSearch() = copy(isSearchActive = false, searchQuery = "", submittedSearchQuery = null)
    fun openSearchResult(pointer: VersePointer): BibleState {
        val searchRecord = searchHistoryRecord(submittedSearchQuery ?: searchQuery)
        return copy(
            book = pointer.book,
            chapter = pointer.chapter,
            scrollPercent = 0f,
            centerVerse = pointer.startVerse ?: 1,
            isSearchActive = false,
            searchQuery = "",
            submittedSearchQuery = null,
            backStack = if (searchRecord != null) backStack + searchRecord else backStack
        )
    }

    fun recordReadHistory(verse: Int): BibleState {
        val record = readHistoryRecord(verse)
        val previousReadRecord = history.lastOrNull()
            ?.takeIf { it.command != record.command }
        val nextBackStack = if (
            previousReadRecord != null &&
            backStack.lastOrNull()?.command != previousReadRecord.command
        ) {
            backStack + previousReadRecord
        } else {
            backStack
        }
        return copy(
            highlightedVerse = verse,
            history = history + record,
            backStack = nextBackStack
        )
    }

    fun clearHistoryHighlight(verse: Int): BibleState {
        return if (highlightedVerse == verse) copy(highlightedVerse = null) else this
    }

    fun handleBack(): BibleState? {
        if (isSearchActive) return clearSearch()
        val previous = backStack.lastOrNull() ?: return null
        val restored =
            restoreHistoryRecord(previous) ?: return copy(backStack = backStack.dropLast(1))
        return restored.copy(backStack = backStack.dropLast(1))
    }

    fun readHistoryRecord(verse: Int? = centerVerse): HistoryRecord {
        return cmpHistoryRecord("cmp read ${mainTranslation.code} $book $chapter ${verse ?: 0}")
    }

    fun searchHistoryRecord(query: String): HistoryRecord? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return cmpHistoryRecord("cmp search ${mainTranslation.code} $trimmed")
    }

    fun isLastChapter() = chapter == Books.maxChapter(book)
    fun lastChapter() = Books.maxChapter(book)
    fun describeBookChapter() = "${mainTranslation.books()[book]} $chapter"
    fun isSingleMain(translationToCompare: Translation) =
        readingMode == ReadingMode.SINGLE && mainTranslation == translationToCompare

    fun isSideMain(translationToCompare: Translation) =
        readingMode == ReadingMode.BILINGUAL_SIDE && mainTranslation == translationToCompare

    fun isSideMainOrSub(translationToCompare: Translation) =
        readingMode == ReadingMode.BILINGUAL_SIDE && (mainTranslation == translationToCompare || subTranslation == translationToCompare)

    fun isUnderMain(translationToCompare: Translation) =
        readingMode == ReadingMode.BILINGUAL_UNDER && mainTranslation == translationToCompare

    fun isUnderMainOrSub(translationToCompare: Translation) =
        readingMode == ReadingMode.BILINGUAL_UNDER && (mainTranslation == translationToCompare || subTranslation == translationToCompare)

    fun narrowerSpaceBetweenVerses() = copy(spaceBetweenVerses = spaceBetweenVerses - 1)
    fun widerSpaceBetweenVerses() = copy(spaceBetweenVerses = spaceBetweenVerses + 1)

    companion object {
        val json = Json { encodeDefaults = true }
    }

    fun toJson() = json.encodeToString(this)

    private fun restoreHistoryRecord(record: HistoryRecord): BibleState? {
        return when {
            record.command.startsWith("cmp read ") -> restoreReadRecord(record)
            record.command.startsWith("cmp search ") -> restoreSearchRecord(record)
            else -> null
        }
    }

    private fun restoreReadRecord(record: HistoryRecord): BibleState? {
        val parts = record.command.split(" ")
        if (parts.size < 6) return null
        val translation = translationByCode(parts[2]) ?: return null
        val restoredBook = parts[3].toIntOrNull() ?: return null
        val restoredChapter = parts[4].toIntOrNull() ?: return null
        val restoredVerse = parts[5].toIntOrNull()?.takeIf { it > 0 }
        return copy(
            mainTranslation = translation,
            book = restoredBook,
            chapter = restoredChapter,
            scrollPercent = 0f,
            centerVerse = restoredVerse,
            isSearchActive = false,
            searchQuery = "",
            submittedSearchQuery = null
        )
    }

    private fun restoreSearchRecord(record: HistoryRecord): BibleState? {
        val prefixParts = record.command.split(" ", limit = 4)
        if (prefixParts.size < 4) return null
        val translation = translationByCode(prefixParts[2]) ?: return null
        val query = prefixParts[3]
        return copy(
            mainTranslation = translation,
            isSearchActive = true,
            searchQuery = query,
            submittedSearchQuery = query
        )
    }
}

fun String.toBibleState() = BibleState.json.decodeFromString<BibleState>(this)

val BibleStateSaver = Saver<BibleState, String>(
    save = { it.toJson() },
    restore = { it.toBibleState() }
)

fun BibleState.withTranslationVisibility(code: String, shown: Boolean): BibleState {
    val updated = translationVisibility.toMutableMap()
    updated[code] = shown
    return copy(translationVisibility = updated)
}

const val SPACE_BETWEEN_VERSES_MIN = 5
const val SPACE_BETWEEN_VERSES_MAX = 50
const val historySaveEventColorTransitionDurationSeconds: Int = 2
const val SHARED_PREFERENCE_KEY_BIBLE_STATE = "bible_state"

private fun cmpHistoryRecord(command: String): HistoryRecord {
    return HistoryRecord(date = "", timezone = "", command = command)
}

private fun translationByCode(code: String): Translation? {
    return SupportedTranslation.entries.firstOrNull { it.translation.code == code }?.translation
}

fun initialBibleStateForAvailableTranslations(
    availableCodes: Set<String>,
    preferredLanguageCode: String?
): BibleState {
    return BibleState(
        mainTranslation = chooseAvailableTranslation(
            availableCodes = availableCodes,
            preferredLanguageCode = preferredLanguageCode
        )
    ).normalizedForAvailableTranslations(
        availableCodes = availableCodes,
        preferredLanguageCode = preferredLanguageCode
    )
}

fun BibleState.normalizedForAvailableTranslations(
    availableCodes: Set<String>,
    preferredLanguageCode: String?
): BibleState {
    val normalizedCodes = availableCodes.map { it.lowercase() }.toSet()
    val fallbackTranslation = chooseAvailableTranslation(normalizedCodes, preferredLanguageCode)
    val normalizedMainTranslation = mainTranslation.takeIf { it.code in normalizedCodes }
        ?: fallbackTranslation
    val normalizedSubTranslation = subTranslation?.takeIf { it.code in normalizedCodes }
    val normalizedReadingMode =
        if (readingMode == ReadingMode.SINGLE || normalizedSubTranslation != null) {
            readingMode
        } else {
            ReadingMode.SINGLE
        }

    return copy(
        mainTranslation = normalizedMainTranslation,
        subTranslation = normalizedSubTranslation,
        readingMode = normalizedReadingMode
    )
}

private fun chooseAvailableTranslation(
    availableCodes: Set<String>,
    preferredLanguageCode: String?
): Translation {
    val normalizedCodes = availableCodes.map { it.lowercase() }.toSet()
    val availableTranslations = SupportedTranslation.all.filter { it.code in normalizedCodes }
    return availableTranslations.firstOrNull { translation ->
        translation.languageCode == preferredLanguageCode
    } ?: availableTranslations.firstOrNull()
    ?: SupportedTranslation.WEBUS.translation
}

@Composable
fun currentAvailableTranslationCodes(): Set<String> {
    val assetManager = currentAssetManager()
    val downloadedCodes = runCatching { assetManager.downloadedTranslationCodes() }
        .getOrElse { emptyList() }
    return EmbeddedPackRegistry.embeddedCodes + downloadedCodes
}

@Composable
fun rememberBibleState(): BibleState {
    val platform = currentPlatform()
    val availableCodes = currentAvailableTranslationCodes()
    val defaultLanguage = Languages.currentLanguageCode()

    val bibleStateJson = platform.settings.getStringOrNull(SHARED_PREFERENCE_KEY_BIBLE_STATE)
    if (bibleStateJson != null) {
        val initialBibleState = bibleStateJson.toBibleState().normalizedForAvailableTranslations(
            availableCodes = availableCodes,
            preferredLanguageCode = defaultLanguage
        )
        logger.debug { "Bible Lifecycle sharedPreferences had initialBibleState: $initialBibleState" }
        return initialBibleState
    }

    logger.debug { "rememberBibleSate default language is $defaultLanguage" }

    val initialBibleState = initialBibleStateForAvailableTranslations(
        availableCodes = availableCodes,
        preferredLanguageCode = defaultLanguage
    )
    logger.debug { "Bible Lifecycle sharedPreferences was null, computed initialBibleState: $initialBibleState" }
    return initialBibleState
}
