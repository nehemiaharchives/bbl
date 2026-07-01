package org.gnit.bible.app

import org.gnit.bible.SupportedTranslation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.ReadingMode
import org.gnit.bible.app.state.SPACE_BETWEEN_VERSES_MAX
import org.gnit.bible.app.state.SPACE_BETWEEN_VERSES_MIN
import org.gnit.bible.app.ui.theme.BibleTheme
import org.gnit.bible.app.ui.widgets.BIBLE_VIEW_ICON
import org.gnit.bible.app.ui.widgets.BIBLE_VIEW_ICON_SPACER
import org.gnit.bible.app.ui.widgets.DROPDOWN_MENU_HEIGHT
import org.gnit.bible.app.ui.widgets.DROPDOWN_MENU_ITEM_LEFT_PADDING
import org.gnit.bible.app.ui.widgets.DROPDOWN_MENU_ITEM_RIGHT_PADDING
import org.gnit.bible.app.ui.widgets.DROPDOWN_MENU_MAX_HEIGHT
import org.gnit.bible.app.ui.widgets.DROPDOWN_MENU_WIDTH
import org.gnit.bible.app.ui.widgets.TranslationDropDownMenuItem
import org.gnit.bible.app.ui.widgets.sansFontFamily
import org.gnit.bible.app.ui.widgets.serifFontFamily
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarContent(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onAnyUserAction: () -> Unit,
    onDropdownVisibilityChange: (Boolean) -> Unit,
    onOpenTranslationManager: () -> Unit,
    hideDropdown: Boolean = false,
    reopenDropdown: Boolean = false,
    onDropdownReopened: () -> Unit = {},
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchRequested: () -> Unit = {},
    onSearchSubmit: () -> Unit = {},
    onSearchCancel: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var settingExpanded by remember { mutableStateOf(false) }
    val bible = currentBible()

    val bibleTitle by remember(bibleState.book, bibleState.chapter, bibleState.mainTranslation) {
        mutableStateOf(bibleState.describeBookChapter())
    }
    val translations = remember(bible, bibleState.translationVisibility) {
        availableTranslationsSafe(bible, bibleState.translationVisibility)
    }

    LaunchedEffect(hideDropdown) {
        if (hideDropdown && menuExpanded) {
            menuExpanded = false
            onDropdownVisibilityChange(false)
        }
    }

    LaunchedEffect(reopenDropdown) {
        if (reopenDropdown) {
            menuExpanded = true
            onDropdownVisibilityChange(true)
            onDropdownReopened()
        }
    }

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
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(BUTTON_SIZE.dp)) {
                // need some space
            }

            Box(
                modifier = Modifier
                    .padding(top = max(min(bibleState.fontSize, 10), 5).dp)
                    .weight(1f)
                    .then(
                        if (isSearchActive) {
                            Modifier
                        } else {
                            Modifier.clickable {
                                onAnyUserAction()
                                onSearchRequested()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchActive) {
                    val focusRequester = remember { FocusRequester() }
                    val keyboard = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = titleFontFamily,
                            fontSize = (max(min(bibleState.fontSize * 1.4F, 40.0F), 16F)).sp
                        ),

                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onSearchCancel) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close search"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                onSearchSubmit()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Text(
                        text = bibleTitle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = titleFontFamily,
                            fontSize = (max(min(bibleState.fontSize * 1.4F, 40.0F), 16F)).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            val dropdownScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .size(BUTTON_SIZE.dp)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                if (!hideDropdown && !isSearchActive) {
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

                    TranslationDropdownMenu(
                        expanded = menuExpanded,
                        settingExpanded = settingExpanded,
                        bibleState = bibleState,
                        translations = translations,
                        dropdownScrollState = dropdownScrollState,
                        onExpandedChange = { isExpanded ->
                            menuExpanded = isExpanded
                            onDropdownVisibilityChange(isExpanded)
                        },
                        onSettingExpandedChange = { settingExpanded = it },
                        onStateChange = onStateChange,
                        onTranslationLongPress = {
                            menuExpanded = false
                            onDropdownVisibilityChange(false)
                            onOpenTranslationManager()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationDropdownMenu(
    expanded: Boolean,
    settingExpanded: Boolean,
    bibleState: BibleState,
    translations: List<Translation>,
    dropdownScrollState: ScrollState,
    onExpandedChange: (Boolean) -> Unit,
    onSettingExpandedChange: (Boolean) -> Unit,
    onStateChange: (BibleState) -> Unit,
    onTranslationLongPress: (Translation) -> Unit
) {
    val inspectionMode = LocalInspectionMode.current
    val popupOffset = with(LocalDensity.current) {
        IntOffset(x = 0, y = BUTTON_SIZE.dp.roundToPx())
    }

    if (!expanded && !inspectionMode) {
        return
    }

    if (inspectionMode) {
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(DROPDOWN_MENU_WIDTH.dp)
                .height(DROPDOWN_MENU_MAX_HEIGHT.dp)
        ) {
            DropdownMenuContent(
                settingExpanded = settingExpanded,
                bibleState = bibleState,
                translations = translations,
                dropdownScrollState = dropdownScrollState,
                onExpandedChange = onExpandedChange,
                onSettingExpandedChange = onSettingExpandedChange,
                onStateChange = onStateChange,
                onTranslationLongPress = onTranslationLongPress
            )
        }
    } else {
        Popup(
            alignment = Alignment.TopEnd,
            offset = popupOffset,
            onDismissRequest = { onExpandedChange(false) },
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .width(DROPDOWN_MENU_WIDTH.dp)
                    .height(DROPDOWN_MENU_MAX_HEIGHT.dp)
            ) {
                DropdownMenuContent(
                    settingExpanded = settingExpanded,
                    bibleState = bibleState,
                    translations = translations,
                    dropdownScrollState = dropdownScrollState,
                    onExpandedChange = onExpandedChange,
                    onSettingExpandedChange = onSettingExpandedChange,
                    onStateChange = onStateChange,
                    onTranslationLongPress = onTranslationLongPress
                )
            }
        }
    }
}

@Composable
private fun DropdownMenuContent(
    settingExpanded: Boolean,
    bibleState: BibleState,
    translations: List<Translation>,
    dropdownScrollState: ScrollState,
    onExpandedChange: (Boolean) -> Unit,
    onSettingExpandedChange: (Boolean) -> Unit,
    onStateChange: (BibleState) -> Unit,
    onTranslationLongPress: (Translation) -> Unit
) {
    Box(
        modifier = Modifier
            .width(DROPDOWN_MENU_WIDTH.dp)
            .height(DROPDOWN_MENU_MAX_HEIGHT.dp)
    ) {
        Box(
            modifier = Modifier
                .height((DROPDOWN_MENU_MAX_HEIGHT - DROPDOWN_MENU_HEIGHT).dp)
                .clipToBounds()
                .align(Alignment.TopStart)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dropdownScrollState)
            ) {
                translations.forEachIndexed { index, translationItem ->
                    if (index != 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    }
                    TranslationDropDownMenuItem(
                        settingExpanded = settingExpanded,
                        bibleState = bibleState,
                        translationItem = translationItem,
                        onClickSingleIcon = {
                            if (bibleState.readingMode == ReadingMode.SINGLE && bibleState.mainTranslation != translationItem) {
                                logger.debug { "DropDownMenu $translationItem is selected, this will change mainTranslation in SingleView" }
                                val changedState =
                                    bibleState.copy(mainTranslation = translationItem)
                                onStateChange(changedState)
                                onExpandedChange(false)
                                logger.debug { "DropdownMenuItem mainTranslation changed $bibleState" }
                            } else if (bibleState.readingMode != ReadingMode.SINGLE) {
                                logger.debug { "DropDownMenu Reading Mode will be changed from Bilingual(Side|Under) to Single. mainTranslation will be changed. subTranslation will be null" }
                                val changedState = bibleState.copy(
                                    mainTranslation = translationItem,
                                    subTranslation = null,
                                    readingMode = ReadingMode.SINGLE
                                )
                                onStateChange(changedState)
                                onExpandedChange(false)
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
                                onExpandedChange(false)
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
                                onExpandedChange(false)
                            }
                        },
                        onLongPress = { onTranslationLongPress(translationItem) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(DROPDOWN_MENU_HEIGHT.dp)
                .width(DROPDOWN_MENU_WIDTH.dp)
                .align(Alignment.BottomStart)
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .absolutePadding(
                        left = DROPDOWN_MENU_ITEM_LEFT_PADDING.dp,
                        right = DROPDOWN_MENU_ITEM_RIGHT_PADDING.dp
                    )
                    .align(Alignment.Center),
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
                        .clickable { onSettingExpandedChange(!settingExpanded) },
                    tint = if (settingExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun availableTranslationsSafe(
    bible: Bible,
    visibility: Map<String, Boolean> = emptyMap()
): List<Translation> =
    runCatching { bible.availableTranslations() }.getOrElse { SupportedTranslation.embeddedTranslations }
        .filter { visibility[it.code] ?: true }

@Preview
@Composable
private fun TranslationDropdownMenuPreview() {
    BibleTheme {
        TranslationDropdownMenu(
            expanded = true,
            settingExpanded = true,
            bibleState = BibleState(),
            translations = previewTranslationList,
            dropdownScrollState = rememberScrollState(),
            onExpandedChange = {},
            onSettingExpandedChange = {},
            onStateChange = {},
            onTranslationLongPress = {}
        )
    }
}

@Preview
@Composable
private fun TranslationDropdownMenuPreview_SettingsCollapsed() {
    BibleTheme {
        TranslationDropdownMenu(
            expanded = true,
            settingExpanded = false,
            bibleState = BibleState(),
            translations = previewTranslationList,
            dropdownScrollState = rememberScrollState(),
            onExpandedChange = {},
            onSettingExpandedChange = {},
            onStateChange = {},
            onTranslationLongPress = {}
        )
    }
}

private val previewTranslationList = listOf(
    SupportedTranslation.WEBUS.translation,
    SupportedTranslation.KJV.translation,
    SupportedTranslation.RVR09.translation,
    SupportedTranslation.TB.translation,
    SupportedTranslation.DELUT.translation,
    SupportedTranslation.LSG.translation,
    SupportedTranslation.SINOD.translation,
    SupportedTranslation.UBIO.translation
)
