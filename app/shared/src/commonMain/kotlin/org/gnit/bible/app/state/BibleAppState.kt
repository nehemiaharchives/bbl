package org.gnit.bible.app.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import com.vanniktech.locale.Languages
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gnit.bible.Books
import org.gnit.bible.Translation
import org.gnit.bible.app.currentBible
import org.gnit.bible.app.currentPlatform
import org.gnit.bible.app.logger

enum class ReadingMode { SINGLE, BILINGUAL_SIDE, BILINGUAL_UNDER }

@Serializable
data class BibleState(
    val mainTranslation: Translation = Translation.webus,
    val subTranslation: Translation? = null,
    val readingMode: ReadingMode = ReadingMode.SINGLE,
    val book: Int = 1,
    val chapter: Int = 1,
    val fontSize: Int = 16,
    val scrollPercent: Float = 0f,
    val isZebraBackground: Boolean = false,
    val spaceBetweenVerses: Int = SPACE_BETWEEN_VERSES_MIN,
    val isFontFamilySerif: Boolean = true,
    val translationVisibility: Map<String, Boolean> = mapOf(
        Translation.webus.code to true,
        Translation.kjv.code to true
    )
) {
    fun prevBook() = copy(book = book - 1, chapter = 1)
    fun nextBook() = copy(book = book + 1, chapter = 1)
    fun changeBook(newBook: Int) = copy(book = newBook, chapter = 1)
    fun prevChapter() = copy(chapter = chapter - 1)
    fun nextChapter() = copy(chapter = chapter + 1)
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
const val SHARED_PREFERENCE_KEY_BIBLE_STATE = "bible_state"

@Composable
fun rememberBibleState(): BibleState {
    val platform = currentPlatform()
    val bible = currentBible()

    val bibleStateJson = platform.settings.getStringOrNull(SHARED_PREFERENCE_KEY_BIBLE_STATE)
    if (bibleStateJson != null) {
        val initialBibleState = bibleStateJson.toBibleState()
        logger.debug { "Bible Lifecycle sharedPreferences had initialBibleState: $initialBibleState" }
        return initialBibleState
    }

    val defaultLanguage = Languages.currentLanguageCode()
    logger.debug { "rememberBibleSate default language is $defaultLanguage" }

    val initialMainTranslation = if (defaultLanguage == "en") {
        Translation.webus
    } else {
        bible.availableTranslations().find { translation ->
            translation.languageCode == defaultLanguage
        } ?: Translation.webus
    }

    val initialBibleState = BibleState(mainTranslation = initialMainTranslation)
    logger.debug { "Bible Lifecycle sharedPreferences was null, computed initialBibleState: $initialBibleState" }
    return initialBibleState
}
