package org.gnit.bible.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.gnit.bible.cmp.Res
import org.gnit.bible.cmp.notosans_devanagari_regular
import org.gnit.bible.cmp.notosans_jp_regular_bible_jc_subset
import org.gnit.bible.cmp.notosans_kr_regular_bible_krv_subset
import org.gnit.bible.cmp.notosans_regular
import org.gnit.bible.cmp.notosans_sc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notosans_tc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notosans_thai_regular
import org.gnit.bible.cmp.notoserif_devanagari_regular
import org.gnit.bible.cmp.notoserif_jp_regular_bible_jc_subset
import org.gnit.bible.cmp.notoserif_kr_regular_bible_krv_subset
import org.gnit.bible.cmp.notoserif_regular
import org.gnit.bible.cmp.notoserif_sc_regular_bible_cupn_subset
import org.gnit.bible.cmp.notoserif_tc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notoserif_thai_regular
import org.jetbrains.compose.resources.Font

val enSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_regular, FontWeight.Normal))

val enSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_regular, FontWeight.Normal))

val scSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_sc_regular_bible_cupn_subset, FontWeight.Normal))

val scSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_sc_regular_bible_cunp_subset, FontWeight.Normal))

val jaSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_jp_regular_bible_jc_subset, FontWeight.Normal))

val jaSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_jp_regular_bible_jc_subset, FontWeight.Normal))

val koSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_kr_regular_bible_krv_subset, FontWeight.Normal))

val koSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_kr_regular_bible_krv_subset, FontWeight.Normal))

val devanagariSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_devanagari_regular, FontWeight.Normal))

val devanagariSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_devanagari_regular, FontWeight.Normal))

val thaiSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_thai_regular, FontWeight.Normal))

val thaiSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_thai_regular, FontWeight.Normal))

val tcSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_tc_regular_bible_cunp_subset, FontWeight.Normal))

val tcSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_tc_regular_bible_cunp_subset, FontWeight.Normal))
