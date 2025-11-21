package org.gnit.bible.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.gnit.bible.BibleState
import org.gnit.bible.ReadingMode
import org.gnit.bible.Translation
import org.gnit.bible.cmp.Res
import org.gnit.bible.cmp.square_empty
import org.gnit.bible.cmp.square_fill_black
import org.gnit.bible.cmp.square_half_bottom_black
import org.gnit.bible.cmp.square_half_left_black
import org.gnit.bible.cmp.square_half_right_black
import org.gnit.bible.cmp.square_half_top_black
import org.gnit.bible.ui.theme.BibleTheme
import org.jetbrains.compose.resources.vectorResource

const val BIBLE_VIEW_ICON_SPACER = 10
const val BIBLE_VIEW_ICON = 20
const val DROPDOWN_MENU_ITEM_RIGHT_PADDING = 10
const val DROPDOWN_MENU_ITEM_LEFT_PADDING = 20
const val DROPDOWN_MENU_WIDTH = 200
const val DROPDOWN_MENU_HEIGHT = 55
const val DROPDOWN_MENU_HEIGHT_EXPANDED = 82
const val DROPDOWN_MENU_MAX_HEIGHT = 360

@Composable
fun TranslationDropDownMenuItem(
    settingExpanded: Boolean,
    bibleState: BibleState,
    translationItem: Translation,
    onClickSingleIcon: () -> Unit,
    onClickSideIcon: () -> Unit,
    onClickUnderIcon: () -> Unit,
    modifier: Modifier = Modifier,
){

    val text = if (settingExpanded) translationItem.shortName() else translationItem.nativeName

    Box(modifier = modifier
        .heightIn(min = DROPDOWN_MENU_HEIGHT.dp, max = DROPDOWN_MENU_HEIGHT_EXPANDED.dp)
        .width(DROPDOWN_MENU_WIDTH.dp)
        .absolutePadding(left = DROPDOWN_MENU_ITEM_LEFT_PADDING.dp, right = DROPDOWN_MENU_ITEM_RIGHT_PADDING.dp)
    ){
        Text(
            text = text,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .clickable { onClickSingleIcon() }
        )

        if (settingExpanded){
            Row(modifier.align(Alignment.CenterEnd)) {

                Icon(
                    imageVector = vectorResource(Res.drawable.square_fill_black),
                    contentDescription = "Single Bible",
                    modifier = Modifier.size(BIBLE_VIEW_ICON.dp).clickable { onClickSingleIcon() },
                    tint = if (bibleState.isSingleMain(translationItem)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )

                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                Icon(
                    imageVector = if (bibleState.isSingleMain(translationItem)) {
                        vectorResource(Res.drawable.square_empty)
                    } else if(bibleState.isSideMain(translationItem)){
                        vectorResource(Res.drawable.square_half_left_black)
                    }else{
                        vectorResource(Res.drawable.square_half_right_black)},
                    contentDescription = "Bilingual Side Bible",
                    modifier = Modifier.size(BIBLE_VIEW_ICON.dp).clickable { onClickSideIcon() },
                    tint = if (bibleState.isSideMainOrSub(translationItem)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )

                Spacer(modifier = Modifier.width(BIBLE_VIEW_ICON_SPACER.dp))

                Icon(
                    imageVector =if (bibleState.isSingleMain(translationItem)) {
                        vectorResource(Res.drawable.square_empty)
                    } else if(bibleState.isUnderMain(translationItem)){
                        vectorResource(Res.drawable.square_half_top_black)
                    }else{
                        vectorResource(Res.drawable.square_half_bottom_black)
                    },
                    contentDescription = "Bilingual Under Bible",
                    modifier = Modifier.size(BIBLE_VIEW_ICON.dp).clickable { onClickUnderIcon() },
                    tint = if (bibleState.isUnderMainOrSub(translationItem)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebusCollapsed() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = false,
            bibleState = BibleState(),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun JcCollapsed() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = false,
            bibleState = BibleState(),
            translationItem = Translation.jc,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun KrvCollapsed() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = false,
            bibleState = BibleState(),
            translationItem = Translation.krv,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SingleMain() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SingleNoMatch() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(),
            translationItem = Translation.jc,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SideMain() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.webus, subTranslation = Translation.jc, readingMode = ReadingMode.BILINGUAL_SIDE),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SideNoMatch() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.krv, subTranslation = Translation.jc, readingMode = ReadingMode.BILINGUAL_SIDE),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SideSub() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.jc, subTranslation = Translation.webus, readingMode = ReadingMode.BILINGUAL_SIDE),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnderMain() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.webus, subTranslation = Translation.jc, readingMode = ReadingMode.BILINGUAL_UNDER),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnderNoMatch() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.krv, subTranslation = Translation.jc, readingMode = ReadingMode.BILINGUAL_UNDER),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnderSub() {
    BibleTheme {
        TranslationDropDownMenuItem(
            settingExpanded = true,
            bibleState = BibleState(mainTranslation = Translation.jc, subTranslation = Translation.webus, readingMode = ReadingMode.BILINGUAL_UNDER),
            translationItem = Translation.webus,
            onClickSingleIcon = {},
            onClickSideIcon = {},
            onClickUnderIcon = {}
        )
    }
}
