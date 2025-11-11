package org.gnit.bible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.russhwolf.settings.Settings
import com.vanniktech.locale.Languages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gnit.bible.ui.theme.BibleTheme
import org.gnit.bible.ui.widgets.sansFontFamily
import org.gnit.bible.ui.widgets.serifFontFamily
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
) {
    fun prevBook() = copy(book = book - 1, chapter = 1)
    fun nextBook() = copy(book = book + 1, chapter = 1)
    fun changeBook(newBook: Int) = copy(book = newBook, chapter = 1)
    fun prevChapter() = copy(chapter = chapter - 1)
    fun nextChapter() = copy(chapter = chapter + 1)
    fun isLastChapter() = (chapter == Chapters.maxChapter(book))
    fun lastChapter() = Chapters.maxChapter(book)
    fun describeBookChapter() = "${mainTranslation.books()[book]} $chapter"
    fun isSingleMain(translationToCompare: Translation) =
        (readingMode == ReadingMode.SINGLE && mainTranslation == translationToCompare)

    fun isSideMain(translationToCompare: Translation) =
        (readingMode == ReadingMode.BILINGUAL_SIDE && mainTranslation == translationToCompare)

    fun isSideMainOrSub(translationToCompare: Translation) =
        (readingMode == ReadingMode.BILINGUAL_SIDE && (mainTranslation == translationToCompare || subTranslation == translationToCompare))

    fun isUnderMain(translationToCompare: Translation) =
        (readingMode == ReadingMode.BILINGUAL_UNDER && mainTranslation == translationToCompare)

    fun isUnderMainOrSub(translationToCompare: Translation) =
        (readingMode == ReadingMode.BILINGUAL_UNDER && (mainTranslation == translationToCompare || subTranslation == translationToCompare))

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

fun assetManager(): AssetManager {
    if (am == null) {
        am = AssetManagerImpl(platform = platform)
    }
    return am!!
}

var bible: Bible? = null

fun bible(): Bible {
    if (bible == null) {
        bible = Bible(assetManager())
        bible!!.bibleTextReader = ComposeBibleTextReader()
    }
    return bible!!
}

lateinit var platform: Platform

@Composable
fun rememberBibleState(): BibleState {

    lateinit var initialBibleState: BibleState

    val bibleStateJson = platform.settings.getStringOrNull(SHARED_PREFERENCE_KEY_BIBLE_STATE)
    if (bibleStateJson != null) {
        initialBibleState = bibleStateJson.toBibleState()
        logger.debug { "Bible Lifecycle sharedPreferences had initialBibleState: $initialBibleState" }
    } else {
        val defaultLanguage = Languages.currentLanguageCode()
        logger.debug { "rememberBibleSate default language is $defaultLanguage" }

        val initialMainTranslation = if (defaultLanguage == "en") {
            Translation.webus
        } else {
            bible().availableTranslations().find { translation ->
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

    platform = getPlatform(platformContext)
    val bible: Bible = bible()

    val initialBibleState = rememberBibleState()
    var bibleState by rememberSaveable(stateSaver = BibleStateSaver) {
        mutableStateOf(
            initialBibleState
        )
    }

    logger.debug { "Bible Lifecycle by rememberSavable { mutableStateOf(initialBibleState) } called, bibleState:$bibleState" }

    var bibleTitle by rememberSaveable { mutableStateOf(bibleState.describeBookChapter()) }
    var zoom by remember { mutableFloatStateOf(bibleState.fontSize.toFloat()) }


    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleResumeEffect(key1 = lifecycleOwner) {
        onPauseOrDispose {
            logger.debug { "Bible Lifecycle onPauseOrDispose called, saving bibleState:$bibleState" }
            platform.settings.putString(SHARED_PREFERENCE_KEY_BIBLE_STATE, bibleState.toJson())
        }
    }

    val chrome = rememberChromeAutoHide()

    // Scaffold, top bar, bottom bar, content
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chrome.isVisible(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopBarContent(
                    title = bibleState.describeBookChapter(),
                    // TODO put menu/book picker/actions here exactly as before
                    onAnyUserAction = { chrome.onUserInteraction() }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = chrome.isVisible(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomBarContent(
                    state = bibleState,
                    onStateChange = { bibleState = it },
                    onAnyUserAction = { chrome.onUserInteraction() }
                )
            }
        }
    ) { innerPadding ->
        // Reading area. We'll hook scroll + double-tap into it.
        BibleReadingArea(
            state = bibleState,
            onStateChange = { bibleState = it },
            contentPadding = innerPadding,
            chrome = chrome
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarContent(
    title: String,
    onAnyUserAction: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            // TODO put existing menu / book picker here.
            // call onAnyUserAction() in their onClick handlers
        }
    )
}

@Composable
fun BottomBarContent(
    state: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit
) {
    BottomAppBar {
        // - slider + for chapter here
        // call onAnyUserAction() in their onClick handlers
    }
}

@Composable
private fun BibleReadingArea(
    state: BibleState,
    onStateChange: (BibleState) -> Unit,
    contentPadding: PaddingValues,
    chrome: ChromeAutoHide
) {
    // provide a shared scrollState to observe.
    val scrollState = rememberScrollState()

    // 1)  Reset inactivity timer while user scrolls
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress -> if (inProgress) chrome.onUserInteraction() }
    }

    // 2) Double-tap anywhere in the reading area to show bars
    val doubleTapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { chrome.forceShow() },
            //onTap = { chrome.onUserInteraction } // single taps also count if needed
        )
    }

    // 3) Container that applies Scaffold padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .then(doubleTapModifier)
    ) {
        when (state.readingMode) {
            ReadingMode.SINGLE -> SingleBible(state, scrollState, contentPadding)
            ReadingMode.BILINGUAL_SIDE -> BilingualSideBible(state, scrollState, contentPadding)
            ReadingMode.BILINGUAL_UNDER -> BilingualUnderBible(state, scrollState, contentPadding)
        }
    }
}

private const val AUTO_HIDE_MS: Long = 10_000

@OptIn(ExperimentalTime::class)
@Composable
private fun rememberChromeAutoHide(): ChromeAutoHide {
    var visible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    val scope = rememberCoroutineScope()

    // background job: hide after inactivity
    LaunchedEffect(lastInteraction) {
        val started = lastInteraction
        delay(AUTO_HIDE_MS)
        // only hide if nothing bumped the timestamp since we started
        if (lastInteraction == started) visible = false
    }

    fun bump() {
        lastInteraction = Clock.System.now().toEpochMilliseconds()
        if (!visible) visible = true
    }

    return remember {
        ChromeAutoHide(
            isVisible = { visible },
            onUserInteraction = { bump() },
            forceShow = { visible = true; bump() }
        )
    }
}

private class ChromeAutoHide(
    val isVisible: () -> Boolean,
    val onUserInteraction: () -> Unit,
    val forceShow: () -> Unit
)

const val VERSES_COLUMN_FILL_MAX_HEIGHT = 0.999f

fun Int.isEven() = this % 2 == 0

@Composable
fun SingleBible(bibleState: BibleState, scrollState: ScrollState, contentPadding: PaddingValues) {

    logger.debug { "SingleBible bibleState: $bibleState" }

    val translation = bibleState.mainTranslation
    val book = bibleState.book
    val chapter = bibleState.chapter
    val chapterText = bible().verses(translation = translation.code, book = book, chapter = chapter)
    val verses = splitChapterToVerses(chapterText)

    ScrollableColumn(bibleState, scrollState, contentPadding) {
        verses.forEachIndexed { verse, text ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Text(
                text = "${verse + 1} $text",
                style = TextStyle(
                    fontSize = bibleState.fontSize.sp,
                    fontFamily = if (bibleState.isFontFamilySerif) translation.language.serifFontFamily() else translation.language.sansFontFamily()
                ),
                modifier = Modifier
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
                    .background(background)
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SingleBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        SingleBible(bibleState = BibleState(), scrollState = scrollState, contentPadding = PaddingValues(0.dp))
    }
}

fun splitChapterToVerses(aChapter: String): Array<String> {
    return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
}

fun addEmptyEntryToMakeSameSize(
    listA: List<String>,
    listB: List<String>
): Pair<List<String>, List<String>> {
    // Find the longer list
    val longerList = if (listA.size > listB.size) listA else listB
    val shorterList = if (listA.size < listB.size) listA else listB

    // Add empty strings to the shorter list to match the length of the longer list
    val paddedShorterList = shorterList.toMutableList()
    repeat(longerList.size - shorterList.size) {
        paddedShorterList.add("")
    }

    // Preserve original order of listA and listB
    val newListA = if (listA == longerList) longerList else paddedShorterList
    val newListB = if (listA == longerList) paddedShorterList else longerList

    return newListA to newListB
}

@Composable
private fun getVersePairs(bibleState: BibleState): List<Pair<String, String>> {
    val mainTranslation = bibleState.mainTranslation
    val subTranslation = bibleState.subTranslation
        ?: throw IllegalArgumentException("subTranslation is required but null")

    val book = bibleState.book
    val chapter = bibleState.chapter

    val mainChapterText =
        bible().verses(translation = mainTranslation.code, book = book, chapter = chapter)
    val subChapterText =
        bible().verses(translation = subTranslation.code, book = book, chapter = chapter)

    val mainVerses = splitChapterToVerses(mainChapterText)
    val subVerses = splitChapterToVerses(subChapterText)

    val verses = if (mainVerses.size == subVerses.size) {
        mainVerses.zip(subVerses).toList()
    } else {
        val newPair = addEmptyEntryToMakeSameSize(mainVerses.toList(), subVerses.toList())
        val newMainVerse = newPair.first
        val newSubVerse = newPair.second
        newMainVerse.zip(newSubVerse)
    }
    return verses
}

@Composable
fun BilingualSideBible(bibleState: BibleState, scrollState: ScrollState, contentPadding: PaddingValues) {
    val readingMode = bibleState.readingMode
    if (readingMode != ReadingMode.BILINGUAL_SIDE) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} but trying to put $readingMode")
    if (bibleState.subTranslation == null) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} so subTranslation is needed but null")

    val versePairs = getVersePairs(bibleState)

    ScrollableColumn(bibleState, scrollState, contentPadding) {
        versePairs.forEachIndexed { verse, pair ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Row(
                modifier = Modifier
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
                    .background(background)
            ) {
                Text(
                    text = "${verse + 1} ${pair.first}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${verse + 1} ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

val sideView = BibleState(Translation.jc, Translation.webus, ReadingMode.BILINGUAL_SIDE)

@Preview(showBackground = true)
@Composable
fun BilingualSideBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualSideBible(bibleState = sideView, scrollState = scrollState, contentPadding = PaddingValues(0.dp))
    }
}

@Composable
fun BilingualUnderBible(bibleState: BibleState, scrollState: ScrollState, contentPadding: PaddingValues) {
    val readingMode = bibleState.readingMode
    if (readingMode != ReadingMode.BILINGUAL_UNDER) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} but trying to put $readingMode")
    if (bibleState.subTranslation == null) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} so subTranslation is needed but null")

    val versePairs = getVersePairs(bibleState)

    ScrollableColumn(bibleState, scrollState, contentPadding) {
        versePairs.forEachIndexed { verse, pair ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Column(modifier = Modifier.background(background)) {
                Text(
                    text = "${verse + 1} ${pair.first}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.mainTranslation.language.serifFontFamily() else bibleState.mainTranslation.language.sansFontFamily()
                    ),
                )
                Text(
                    text = "${verse + 1} ${pair.second}",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) bibleState.subTranslation.language.serifFontFamily() else bibleState.subTranslation.language.sansFontFamily()
                    ),
                    modifier = Modifier.absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
                )
            }
        }
    }
}

val downView = BibleState(Translation.jc, Translation.webus, ReadingMode.BILINGUAL_UNDER)

@Preview(showBackground = true)
@Composable
fun BilingualUnderBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        BilingualUnderBible(bibleState = downView, scrollState = scrollState, contentPadding = PaddingValues(0.dp))
    }
}

@Composable
fun ScrollableColumn(
    bibleState: BibleState,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scrollState)
    ) {
        content()
    }
    LaunchedEffect(bibleState.book) { scrollState.scrollTo(0) }
    LaunchedEffect(bibleState.chapter) { scrollState.scrollTo(0) }
    LaunchedEffect(bibleState.readingMode) {
        val scrollValue = (scrollState.maxValue * bibleState.scrollPercent).toInt()
        scrollState.scrollTo(scrollValue)
    }

    val sharedPreferences = platform.settings
    LaunchedEffect(scrollState) {
        val lastScrollValue = scrollState.value
        var pendingSaveJob: Job? = null

        snapshotFlow { scrollState.value }
            .collect { newValue ->
                if (newValue != lastScrollValue) {
                    pendingSaveJob?.cancel()
                    pendingSaveJob = launch {
                        delay(200)
                        if (!scrollState.isScrollInProgress) {
                            val scrollPercent = computeScrollPercent(newValue, scrollState)
                            sharedPreferences.putString(
                                SHARED_PREFERENCE_KEY_BIBLE_STATE,
                                bibleState.copy(scrollPercent = scrollPercent).toJson()
                            )
                            logger.debug { "ScrollableColumn Saved scroll scrollPercent: $scrollPercent" }
                        }
                    }
                }
            }
    }
}

private fun computeScrollPercent(scrollValue: Int, scrollState: ScrollState): Float {
    val totalScrollableHeight = scrollState.maxValue
    return scrollValue.toFloat() / totalScrollableHeight
}
