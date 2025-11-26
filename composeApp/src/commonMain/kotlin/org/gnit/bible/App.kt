package org.gnit.bible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vanniktech.locale.Languages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gnit.bible.cmp.Res
import org.gnit.bible.cmp.arrows_collapse
import org.gnit.bible.cmp.arrows_expand
import org.gnit.bible.cmp.font_switch
import org.gnit.bible.cmp.rows_white
import org.gnit.bible.cmp.rows_zebra
import org.gnit.bible.cmp.settings
import org.gnit.bible.ui.theme.BibleTheme
import org.gnit.bible.ui.widgets.BIBLE_VIEW_ICON
import org.gnit.bible.ui.widgets.BIBLE_VIEW_ICON_SPACER
import org.gnit.bible.ui.widgets.BibleButton
import org.gnit.bible.ui.widgets.BibleSlider
import org.gnit.bible.ui.widgets.DROPDOWN_MENU_HEIGHT
import org.gnit.bible.ui.widgets.DROPDOWN_MENU_ITEM_LEFT_PADDING
import org.gnit.bible.ui.widgets.DROPDOWN_MENU_ITEM_RIGHT_PADDING
import org.gnit.bible.ui.widgets.DROPDOWN_MENU_MAX_HEIGHT
import org.gnit.bible.ui.widgets.DROPDOWN_MENU_WIDTH
import org.gnit.bible.ui.widgets.TranslationDropDownMenuItem
import org.gnit.bible.ui.widgets.sansFontFamily
import org.gnit.bible.ui.widgets.serifFontFamily
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
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
const val BUTTON_SIZE = 28
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
@Preview
fun AppInAutoHideMode(){
    BibleTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ){
            BibleApp(platformContext = null, initialChromeVisible = false)
        }
    }
}

@Composable
fun BibleApp(
    platformContext: Any? = null,
    modifier: Modifier = Modifier,
    initialChromeVisible: Boolean = true
) {

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

    val chrome = rememberChromeAutoHide(initialChromeVisible)

    // Scaffold, top bar, bottom bar, content
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chrome.isVisible(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    TopBarContent(
                        bibleState = bibleState,
                        onStateChange = { bibleState = it },
                        onOpenSettings = { /* open settings screen/dialog */ },   // TODO put menu/book picker/actions here exactly as before
                        onAnyUserAction = { chrome.onUserInteraction() },
                        onDropdownVisibilityChange = { isOpen ->
                            chrome.setPause(isOpen)
                            if (isOpen) chrome.forceShow() else chrome.onUserInteraction()
                        }
                    )
                    BookControlsBar(
                        bibleState = bibleState,
                        onStateChange = { bibleState = it },
                        onAnyUserAction = { chrome.onUserInteraction() }
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = chrome.isVisible(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    ChapterControlsBar(
                        bibleState = bibleState,
                        onStateChange = { bibleState = it },
                        onAnyUserAction = { chrome.onUserInteraction() }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Reading area. We'll hook scroll + double-tap into it.
        BibleReadingArea(
            state = bibleState,
            onStateChange = { bibleState = it },
            chrome = chrome,
            chromeVisible = chrome.isVisible(),
            innerPadding = innerPadding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarContent(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onOpenSettings: () -> Unit,
    onAnyUserAction: () -> Unit,
    onDropdownVisibilityChange: (Boolean) -> Unit
) {
    val bibleTitle by remember(bibleState.book, bibleState.chapter, bibleState.mainTranslation) {
        mutableStateOf(bibleState.describeBookChapter())
    }
    val topBarHeight = (BUTTON_SIZE + BUTTON_PADDING * 2).dp

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.statusBarsPadding()
    ) {
        val titleFontFamily = if (bibleState.isFontFamilySerif) {
            bibleState.mainTranslation.language.serifFontFamily()
        } else {
            bibleState.mainTranslation.language.sansFontFamily()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = topBarHeight)
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(BUTTON_SIZE.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bibleTitle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = titleFontFamily,
                        fontSize = bibleState.fontSize.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            var menuExpanded by remember { mutableStateOf(false) }
            var settingExpanded by remember { mutableStateOf(false) }

            val dropdownScrollState = rememberScrollState()

            Box(modifier = Modifier
                .size(BUTTON_SIZE.dp)
                .wrapContentSize(Alignment.TopEnd)) {
                IconButton(onClick = {
                    onAnyUserAction()
                    menuExpanded = !menuExpanded
                    onDropdownVisibilityChange(menuExpanded)
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        onDropdownVisibilityChange(false)
                    },
                    modifier = Modifier.heightIn(max = DROPDOWN_MENU_MAX_HEIGHT.dp)
                ) {
                Column(
                    modifier = Modifier
                        .width(DROPDOWN_MENU_WIDTH.dp)
                        .heightIn(max = DROPDOWN_MENU_MAX_HEIGHT.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = (DROPDOWN_MENU_MAX_HEIGHT - DROPDOWN_MENU_HEIGHT).dp)
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(dropdownScrollState)
                        ) {
                            val translations = bible().availableTranslations()
                            translations.forEachIndexed { index, translationItem ->
                                if (index != 0) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                                TranslationDropDownMenuItem(
                                    settingExpanded = settingExpanded,
                                    bibleState = bibleState,
                                    translationItem = translationItem,
                                    onClickSingleIcon = {
                                        if (bibleState.readingMode == ReadingMode.SINGLE && bibleState.mainTranslation != translationItem) {
                                            logger.debug { "DropDownMenu $translationItem is selected, this will change mainTranslation in SingleView" }
                                            val changedState = bibleState.copy(mainTranslation = translationItem)
                                            onStateChange(changedState)
                                            menuExpanded = false
                                            onDropdownVisibilityChange(false)
                                            logger.debug { "DropdownMenuItem mainTranslation changed $bibleState" }
                                        } else if (bibleState.readingMode != ReadingMode.SINGLE) {
                                            logger.debug { "DropDownMenu Reading Mode will be changed from Bilingual(Side|Under) to Single. mainTranslation will be changed. subTranslation will be null" }
                                            val changedState = bibleState.copy(
                                                mainTranslation = translationItem,
                                                subTranslation = null,
                                                readingMode = ReadingMode.SINGLE
                                            )
                                            onStateChange(changedState)
                                            menuExpanded = false
                                            onDropdownVisibilityChange(false)
                                        }
                                    },
                                    onClickSideIcon = {
                                        if (bibleState.isSingleMain(translationItem)) {
                                            logger.debug { "DropDownMenu in SingleView, no action should be taken when clicking side icon" }
                                        } else {
                                            logger.debug { "DropDownMenu $translationItem will be added to subTranslation, and ReadingMode will be changed to SIDE" }
                                            val changedState = bibleState.copy(
                                                subTranslation = translationItem,
                                                readingMode = ReadingMode.BILINGUAL_SIDE
                                            )
                                            onStateChange(changedState)
                                            menuExpanded = false
                                            onDropdownVisibilityChange(false)
                                        }
                                    },
                                    onClickUnderIcon = {
                                        if (bibleState.isSingleMain(translationItem)) {
                                            logger.debug { "DropDownMenu in SingleView, no action should be taken when clicking under icon" }
                                        } else {
                                            logger.debug { "DropDownMenu $translationItem will be added to subTranslation, and ReadingMode will be changed to UNDER" }
                                            val changedState = bibleState.copy(
                                                subTranslation = translationItem,
                                                readingMode = ReadingMode.BILINGUAL_UNDER
                                            )
                                            onStateChange(changedState)
                                            menuExpanded = false
                                            onDropdownVisibilityChange(false)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .height(DROPDOWN_MENU_HEIGHT.dp)
                            .width(DROPDOWN_MENU_WIDTH.dp)
                            .absolutePadding(
                                left = DROPDOWN_MENU_ITEM_LEFT_PADDING.dp,
                                right = DROPDOWN_MENU_ITEM_RIGHT_PADDING.dp
                            )
                    ) {
                        HorizontalDivider(
                            thickness = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (settingExpanded) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.font_switch),
                                    contentDescription = "Switch FontFamily between Serif and SansSerif",
                                    modifier = Modifier
                                        .size(BIBLE_VIEW_ICON.dp)
                                        .clickable {
                                            onStateChange(bibleState.copy(isFontFamilySerif = !bibleState.isFontFamilySerif))
                                        },
                                    tint = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                                Icon(
                                    imageVector = vectorResource(Res.drawable.arrows_collapse),
                                    contentDescription = "Narrower space between verses",
                                    modifier = Modifier
                                        .size(BIBLE_VIEW_ICON.dp)
                                        .clickable {
                                            if (bibleState.spaceBetweenVerses != SPACE_BETWEEN_VERSES_MIN) onStateChange(
                                                bibleState.narrowerSpaceBetweenVerses()
                                            )
                                        },
                                    tint = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                                Icon(
                                    imageVector = vectorResource(Res.drawable.arrows_expand),
                                    contentDescription = "Wider space between verses",
                                    modifier = Modifier
                                        .size(BIBLE_VIEW_ICON.dp)
                                        .clickable {
                                            if (bibleState.spaceBetweenVerses != SPACE_BETWEEN_VERSES_MAX) onStateChange(
                                                bibleState.widerSpaceBetweenVerses()
                                            )
                                        },
                                    tint = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                                Icon(
                                    imageVector = vectorResource(Res.drawable.rows_white),
                                    contentDescription = "Rows with plain background",
                                    modifier = Modifier
                                        .size(BIBLE_VIEW_ICON.dp)
                                        .clickable {
                                            onStateChange(bibleState.copy(isZebraBackground = false))
                                        },
                                    tint = if (bibleState.isZebraBackground) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                                Icon(
                                    imageVector = vectorResource(Res.drawable.rows_zebra),
                                    contentDescription = "Rows with zebra background",
                                    modifier = Modifier
                                        .size(BIBLE_VIEW_ICON.dp)
                                        .clickable {
                                            onStateChange(bibleState.copy(isZebraBackground = true))
                                        },
                                    tint = if (bibleState.isZebraBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                imageVector = vectorResource(Res.drawable.settings),
                                contentDescription = "Settings",
                                modifier = Modifier
                                    .size(BIBLE_VIEW_ICON.dp)
                                    .clickable { settingExpanded = !settingExpanded },
                                tint = if (settingExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookControlsBar(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit
) {

    var bookSliderPosition by remember { mutableFloatStateOf(bibleState.book.toFloat()) }

    LaunchedEffect(bibleState.book) {
        bookSliderPosition = bibleState.book.toFloat()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp)
            .heightIn(min = BUTTON_SIZE.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        BibleButton(
            buttonText = "-",
            onClick = {
                if (bibleState.book != 1) {
                    bookSliderPosition--
                    onStateChange(bibleState.prevBook())
                    onAnyUserAction()
                    logger.debug { "BibleButton book changed ${bibleState.prevBook()}" }
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        BibleSlider(
            value = bookSliderPosition,
            onValueChange = { newValue ->
                bookSliderPosition = newValue
                onStateChange(bibleState.changeBook(newValue.roundToInt()))
                onAnyUserAction()
                logger.debug { "Slider book changed ${bibleState.changeBook(newValue.roundToInt())}" }
            },
            steps = 64,
            valueRange = 1f..66f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        BibleButton(
            buttonText = "+",
            onClick = {
                if (bibleState.book != 66) {
                    bookSliderPosition++
                    onStateChange(bibleState.nextBook())
                    onAnyUserAction()
                    logger.debug { "BibleButton book changed ${bibleState.nextBook()}" }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterControlsBar(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit
) {

    var chapterSliderPosition by remember { mutableFloatStateOf(bibleState.chapter.toFloat()) }

    LaunchedEffect(bibleState.book, bibleState.chapter) {
        chapterSliderPosition = bibleState.chapter.toFloat()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .heightIn(min = BUTTON_SIZE.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BibleButton(
            buttonText = "-",
            onClick = {
                if (bibleState.chapter != 1) {
                    chapterSliderPosition--
                    onStateChange(bibleState.prevChapter())
                    onAnyUserAction()
                    logger.debug { "BibleButton chapter changed ${bibleState.prevChapter()}" }
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        BibleSlider(
            value = chapterSliderPosition,
            onValueChange = { newValue ->
                chapterSliderPosition = newValue
                onStateChange(bibleState.copy(chapter = newValue.roundToInt()))
                onAnyUserAction()
                logger.debug { "Slider chapter slider value changed to ${newValue.roundToInt()}" }
            },
            steps = (bibleState.lastChapter() - 2).coerceAtLeast(0),
            valueRange = 1f..bibleState.lastChapter().toFloat(),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        BibleButton(
            buttonText = "+",
            onClick = {
                if (!bibleState.isLastChapter()) {
                    chapterSliderPosition++
                    onStateChange(bibleState.nextChapter())
                    onAnyUserAction()
                    logger.debug { "BibleButton chapter changed ${bibleState.nextChapter()}" }
                }
            }
        )
    }
}

@Composable
private fun BibleReadingArea(
    state: BibleState,
    onStateChange: (BibleState) -> Unit,
    chrome: ChromeAutoHide,
    chromeVisible: Boolean,
    innerPadding: PaddingValues
) {
    // provide a shared scrollState to observe.
    val scrollState = rememberScrollState()

    // 1)  Reset inactivity timer while user scrolls
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress ->
            if (inProgress && chrome.isVisible()) {
                chrome.onUserInteraction()
            }
        }
    }

    // 2) Double-tap anywhere in the reading area to show bars
    val doubleTapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                if (chrome.isVisible()) {
                    chrome.forceHide()
                } else {
                    chrome.forceShow()
                }
            },
            //onTap = { chrome.onUserInteraction } // single taps also count if needed
        )
    }

    // 3) Container that applies Scaffold padding and offsets for chrome
    val topChromePadding = 0.dp
    val bottomChromePadding = 0.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(top = topChromePadding, bottom = bottomChromePadding)
            .then(doubleTapModifier)
    ) {
        when (state.readingMode) {
            ReadingMode.SINGLE -> SingleBible(state, scrollState)
            ReadingMode.BILINGUAL_SIDE -> BilingualSideBible(state, scrollState)
            ReadingMode.BILINGUAL_UNDER -> BilingualUnderBible(state, scrollState)
        }
    }
}

private const val AUTO_HIDE_MS: Long = 60_000

@OptIn(ExperimentalTime::class)
@Composable
private fun rememberChromeAutoHide(initiallyVisible: Boolean = true): ChromeAutoHide {
    var visible by remember { mutableStateOf(initiallyVisible) }
    var lastInteraction by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var pauseHide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // background job: hide after inactivity
    LaunchedEffect(lastInteraction, pauseHide) {
        if (pauseHide) return@LaunchedEffect
        val started = lastInteraction
        delay(AUTO_HIDE_MS)
        // only hide if nothing bumped the timestamp since we started
        if (!pauseHide && lastInteraction == started) visible = false
    }

    fun bump() {
        lastInteraction = Clock.System.now().toEpochMilliseconds()
        if (!visible) visible = true
    }

    fun hide() {
        visible = false
    }

    return remember {
        ChromeAutoHide(
            isVisible = { visible },
            onUserInteraction = { bump() },
            forceShow = { visible = true; bump() },
            forceHide = { hide() },
            setPause = { pause ->
                pauseHide = pause
                if (pause) visible = true
            }
        )
    }
}

private class ChromeAutoHide(
    val isVisible: () -> Boolean,
    val onUserInteraction: () -> Unit,
    val forceShow: () -> Unit,
    val forceHide: () -> Unit,
    val setPause: (Boolean) -> Unit
)

const val VERSES_COLUMN_FILL_MAX_HEIGHT = 0.999f

fun Int.isEven() = this % 2 == 0

@Composable
fun SingleBible(bibleState: BibleState, scrollState: ScrollState) {

    logger.debug { "SingleBible bibleState: $bibleState" }

    val translation = bibleState.mainTranslation
    val book = bibleState.book
    val chapter = bibleState.chapter
    val chapterText = bible().verses(translation = translation.code, book = book, chapter = chapter)
    val verses = splitChapterToVerses(chapterText)

    ScrollableColumn(bibleState, scrollState) {
        verses.forEachIndexed { verse, text ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
            ) {
                Text(
                    text = "${verse + 1} $text",
                    style = TextStyle(
                        fontSize = bibleState.fontSize.sp,
                        fontFamily = if (bibleState.isFontFamilySerif) translation.language.serifFontFamily() else translation.language.sansFontFamily()
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SingleBiblePreview() {
    BibleTheme {
        val scrollState = rememberScrollState()
        SingleBible(
            bibleState = BibleState(),
            scrollState = scrollState
        )
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
fun BilingualSideBible(
    bibleState: BibleState,
    scrollState: ScrollState
) {
    val readingMode = bibleState.readingMode
    if (readingMode != ReadingMode.BILINGUAL_SIDE) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} but trying to put $readingMode")
    if (bibleState.subTranslation == null) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_SIDE} so subTranslation is needed but null")

    val versePairs = getVersePairs(bibleState)

    ScrollableColumn(bibleState, scrollState) {
        versePairs.forEachIndexed { verse, pair ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
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
        BilingualSideBible(
            bibleState = sideView,
            scrollState = scrollState
        )
    }
}

@Composable
fun BilingualUnderBible(
    bibleState: BibleState,
    scrollState: ScrollState
) {
    val readingMode = bibleState.readingMode
    if (readingMode != ReadingMode.BILINGUAL_UNDER) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} but trying to put $readingMode")
    if (bibleState.subTranslation == null) throw IllegalArgumentException("ReadingMode should be ${ReadingMode.BILINGUAL_UNDER} so subTranslation is needed but null")

    val versePairs = getVersePairs(bibleState)

    ScrollableColumn(bibleState, scrollState) {
        versePairs.forEachIndexed { verse, pair ->

            val background =
                if (bibleState.isZebraBackground && verse.isEven()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .absolutePadding(bottom = bibleState.spaceBetweenVerses.dp)
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
        BilingualUnderBible(
            bibleState = downView,
            scrollState = scrollState
        )
    }
}

@Composable
fun ScrollableColumn(
    bibleState: BibleState,
    scrollState: ScrollState,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
