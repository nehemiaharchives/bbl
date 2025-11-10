package org.gnit.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.russhwolf.settings.Settings
import com.vanniktech.locale.Languages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gnit.bible.ui.theme.BibleTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    val isFontFamilySerif: Boolean = true
){
    fun prevBook() = copy(book = book - 1, chapter = 1)
    fun nextBook() = copy(book = book + 1, chapter = 1)
    fun changeBook(newBook: Int) = copy(book = newBook, chapter = 1)
    fun prevChapter() = copy(chapter = chapter - 1)
    fun nextChapter() = copy(chapter = chapter + 1)
    fun isLastChapter() = (chapter == Chapters.maxChapter(book))
    fun lastChapter() = Chapters.maxChapter(book)
    fun describeBookChapter() = "${mainTranslation.books()[book]} $chapter"
    fun isSingleMain(translationToCompare: Translation) = (readingMode == ReadingMode.SINGLE && mainTranslation == translationToCompare)
    fun isSideMain(translationToCompare: Translation) = (readingMode == ReadingMode.BILINGUAL_SIDE && mainTranslation == translationToCompare)
    fun isSideMainOrSub(translationToCompare: Translation) = (readingMode == ReadingMode.BILINGUAL_SIDE && (mainTranslation == translationToCompare || subTranslation == translationToCompare))
    fun isUnderMain(translationToCompare: Translation) = (readingMode == ReadingMode.BILINGUAL_UNDER && mainTranslation == translationToCompare)
    fun isUnderMainOrSub(translationToCompare: Translation) = (readingMode == ReadingMode.BILINGUAL_UNDER && (mainTranslation == translationToCompare || subTranslation == translationToCompare))
    fun narrowerSpaceBetweenVerses() = copy(spaceBetweenVerses = spaceBetweenVerses -1)
    fun widerSpaceBetweenVerses() = copy(spaceBetweenVerses = spaceBetweenVerses + 1)

    companion object { val json = Json { encodeDefaults = true }}
    fun toJson() = json.encodeToString(this)
}

fun String.toBibleState() = BibleState.json.decodeFromString<BibleState>(this)

val BibleStateSaver = Saver<BibleState, String>(
    save = { it.toJson() },
    restore = { it.toBibleState() }
)

const val BUTTON_PADDING = 5
const val BUTTON_SIZE = 35
const val BUTTON_ROUND = 5
const val BUTTON_TEXT_FONT_SIZE = 15
const val BUTTON_CONTENT_PADDING = 0

const val SPACE_BETWEEN_VERSES_MIN = 5
const val SPACE_BETWEEN_VERSES_MAX = 50

const val SHARED_PREFERENCE_NAME = "Bible"
const val SHARED_PREFERENCE_KEY_BIBLE_STATE = "bible_state"

val logger = KotlinLogging.logger {}

var am: AssetManagerImpl? = null

fun assetManager(platform: Platform): AssetManager {
    if (am == null){
        am = AssetManagerImpl(platform = platform)
    }
    return am!!
}

var bible: Bible? = null

fun bible(platform: Platform): Bible {
    if (bible == null){
        bible = Bible(assetManager(platform = platform))
        bible!!.bibleTextReader = ComposeBibleTextReader()
    }
    return bible!!
}

@Composable
fun rememberBibleState(platform: Platform): BibleState {

    lateinit var initialBibleState: BibleState
    val settings = platform.settings
    val bibleStateJson = settings?.getStringOrNull(SHARED_PREFERENCE_KEY_BIBLE_STATE)
    if (bibleStateJson != null){
        initialBibleState = bibleStateJson.toBibleState()
        logger.debug { "Bible Lifecycle sharedPreferences had initialBibleState: $initialBibleState" }
    } else {
        val defaultLanguage = Languages.currentLanguageCode()
        logger.debug { "rememberBibleSate default language is $defaultLanguage" }

        val initialMainTranslation = if(defaultLanguage == "en"){
            Translation.webus
        }else{
            bible(platform).availableTranslations().find { translation ->
                translation.languageCode == defaultLanguage
            } ?: Translation.webus
        }

        initialBibleState = BibleState(mainTranslation = initialMainTranslation)
        logger.debug { "Bible Lifecycle sharedPreferences was null, computed initialBibleState: $initialBibleState" }
    }

    return initialBibleState
}

@Composable
@Preview
fun App(platformContext: Any? = null) {
    BibleTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BibleApp(platformContext)
        }
    }
}

@Composable
fun BibleApp(platformContext: Any? = null, modifier: Modifier = Modifier) {

    logger.debug { "BibleApp called with platformContext:$platformContext" }

    val platform = getPlatform(platformContext)
    val bible: Bible = bible(platform)

    val initialBibleState = rememberBibleState(platform)
    var bibleState by rememberSaveable(stateSaver = BibleStateSaver) { mutableStateOf(initialBibleState) }

    logger.debug { "Bible Lifecycle by rememberSavable { mutableStateOf(initialBibleState) } called, bibleState:$bibleState" }

    var bibleTitle by rememberSaveable { mutableStateOf(bibleState.describeBookChapter()) }
    var zoom by remember { mutableFloatStateOf(bibleState.fontSize.toFloat()) }

    val settings = platform.settings
    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleResumeEffect(key1 = lifecycleOwner){
        onPauseOrDispose {
            logger.debug { "Bible Lifecycle onPauseOrDispose called, saving bibleState:$bibleState" }
            settings.putString(SHARED_PREFERENCE_KEY_BIBLE_STATE, bibleState.toJson())
        }
    }

    Text(bible.verses())
}
