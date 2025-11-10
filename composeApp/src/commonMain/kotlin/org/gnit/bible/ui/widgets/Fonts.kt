package org.gnit.bible.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.gnit.bible.Language
import org.gnit.bible.cmp.Res
import org.gnit.bible.cmp.notosans_bengali_regular
import org.gnit.bible.cmp.notosans_devanagari_regular
import org.gnit.bible.cmp.notosans_gujarati_regular
import org.gnit.bible.cmp.notosans_jp_regular_bible_jc_subset
import org.gnit.bible.cmp.notosans_kr_regular_bible_krv_subset
import org.gnit.bible.cmp.notosans_regular
import org.gnit.bible.cmp.notosans_sc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notosans_tc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notosans_thai_regular
import org.gnit.bible.cmp.notosans_telugu_regular
import org.gnit.bible.cmp.notosans_tamil_regular
import org.gnit.bible.cmp.notosans_urdu_regular
import org.gnit.bible.cmp.notoserif_bengali_regular
import org.gnit.bible.cmp.notoserif_devanagari_regular
import org.gnit.bible.cmp.notoserif_gujarati_regular
import org.gnit.bible.cmp.notoserif_jp_regular_bible_jc_subset
import org.gnit.bible.cmp.notoserif_kr_regular_bible_krv_subset
import org.gnit.bible.cmp.notoserif_regular
import org.gnit.bible.cmp.notoserif_sc_regular_bible_cupn_subset
import org.gnit.bible.cmp.notoserif_tc_regular_bible_cunp_subset
import org.gnit.bible.cmp.notoserif_thai_regular
import org.gnit.bible.cmp.notoserif_telugu_regular
import org.gnit.bible.cmp.notoserif_tamil_regular
import org.gnit.bible.cmp.notoserif_urdu_regular
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

val bengaliSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_bengali_regular, FontWeight.Normal))

val bengaliSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_bengali_regular, FontWeight.Normal))

val teluguSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_telugu_regular, FontWeight.Normal))

val teluguSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_telugu_regular, FontWeight.Normal))

val tamilSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_tamil_regular, FontWeight.Normal))

val tamilSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_tamil_regular, FontWeight.Normal))

val gujaratiSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_gujarati_regular, FontWeight.Normal))

val gujaratiSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_gujarati_regular, FontWeight.Normal))

val urduSerifFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notoserif_urdu_regular, FontWeight.Normal))

val urduSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.notosans_urdu_regular, FontWeight.Normal))

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

@Composable
fun Language.serifFontFamily() = when(this){
    Language.en, Language.es, Language.pt, Language.de, Language.fr, Language.ru, Language.nl, Language.it, Language.pl, Language.uk, Language.sv, Language.vi, Language.tl, Language.id -> enSerifFontFamily
    Language.zh -> scSerifFontFamily
    Language.zht -> tcSerifFontFamily
    Language.ko -> koSerifFontFamily
    Language.ja -> jaSerifFontFamily
    Language.hi, Language.mr, Language.ne -> devanagariSerifFontFamily
    Language.te -> teluguSerifFontFamily
    Language.ta -> tamilSerifFontFamily
    Language.gu -> gujaratiSerifFontFamily
    Language.ur -> urduSerifFontFamily
    Language.bn -> bengaliSerifFontFamily
    Language.th -> thaiSerifFontFamily
    // add more language-font pair
    else -> error("Unsupported language: $this")
}

@Composable
fun Language.sansFontFamily() = when(this){
    Language.en, Language.es, Language.pt, Language.de, Language.fr, Language.ru, Language.nl, Language.it, Language.pl, Language.uk, Language.sv, Language.vi, Language.tl, Language.id -> enSansFontFamily
    Language.zh -> scSansFontFamily
    Language.zht -> tcSansFontFamily
    Language.ko -> koSansFontFamily
    Language.ja -> jaSansFontFamily
    Language.hi, Language.mr, Language.ne -> devanagariSansFontFamily
    Language.te -> teluguSansFontFamily
    Language.ta -> tamilSansFontFamily
    Language.gu -> gujaratiSansFontFamily
    Language.ur -> urduSansFontFamily
    Language.bn -> bengaliSansFontFamily
    Language.th -> thaiSansFontFamily
    // add more language-font pair
    else -> error("Unsupported language: $this")
}
