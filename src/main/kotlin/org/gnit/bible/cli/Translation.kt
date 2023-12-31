package org.gnit.bible.cli

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.es.SpanishAnalyzer
import org.apache.lucene.analysis.fr.FrenchAnalyzer
import org.apache.lucene.analysis.id.IndonesianAnalyzer
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.ko.KoreanAnalyzer
import org.apache.lucene.analysis.morfologik.MorfologikAnalyzer
import org.apache.lucene.analysis.ne.NepaliAnalyzer
import org.apache.lucene.analysis.nl.DutchAnalyzer
import org.apache.lucene.analysis.pt.PortugueseAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.sv.SwedishAnalyzer
import org.apache.lucene.analysis.th.ThaiAnalyzer
import org.apache.lucene.analysis.uk.UkrainianMorfologikAnalyzer
import org.gnit.bible.Language
import org.gnit.bible.Translation
import java.nio.charset.Charset
import java.util.*

fun Language.isCJK() = when (this) {
    Language.zh, Language.ja, Language.ko -> true
    else -> false
}

fun Language.getAnalyzer(): Analyzer {

    return when (this) {
        Language.en -> EnglishAnalyzer()
        Language.es -> SpanishAnalyzer()
        Language.pt -> PortugueseAnalyzer()
        Language.de -> GermanAnalyzer()
        Language.fr -> FrenchAnalyzer()
        Language.ru -> RussianAnalyzer()
        Language.nl -> DutchAnalyzer()
        Language.it -> ItalianAnalyzer()
        Language.pl -> MorfologikAnalyzer() // for Polish
        Language.uk -> UkrainianMorfologikAnalyzer() // for Ukrainian
        Language.sv -> SwedishAnalyzer()
        Language.zh -> SmartChineseAnalyzer()
        Language.ko -> KoreanAnalyzer()
        Language.ja -> JapaneseAnalyzer()
        // TODO Language.vi -> // Vietnamese https://github.com/duydo/elasticsearch-analysis-vietnamese/blob/master/src/main/java/org/apache/lucene/analysis/vi/VietnameseAnalyzer.java
        // TODO Language.tl -> // Tagalog https://stackoverflow.com/questions/55020235/adding-rare-languages-to-apache-solr
        Language.ne -> NepaliAnalyzer()
        Language.id -> IndonesianAnalyzer()
        Language.th -> ThaiAnalyzer()
        else -> StandardAnalyzer()
    }
}

val availableTranslations = listOf(
    Translation.webus,
    Translation.kjv,
    Translation.rvr09,
    Translation.tb,
    Translation.delut,
    Translation.lsg,
    Translation.sinod,
    Translation.svrj,
    Translation.rdv24,
    Translation.ubg,
    Translation.ukrk,
    Translation.sven,
    Translation.cunp,
    Translation.krv,
    Translation.jc,
)

private const val BIBLES = "bibleTranslationNames"

fun Translation.getAnalyzer() = this.language.getAnalyzer()

/**
 * CJK characters break string format length. So they have to be detected and adjusted.
 * [ref](https://qiita.com/gsy0911/items/00876d8c61ce36bd5fba)
 */
fun Translation.getDescription(format: Boolean = false): String {
    val abbrev = this.toString().uppercase()
    val englishName = ResourceBundle.getBundle(BIBLES, Locale.of("en")).getString(this.toString())
    val nativeLocale = Locale.of(this.language.toString())
    val nativeName = ResourceBundle.getBundle(BIBLES, nativeLocale).getString(this.toString())

    return if (format) {
        val byteDiff = if (this.language.isCJK()) (getByteLength(nativeName) - nativeName.length) / 2 else 0
        "%-6s| %-43s| %-${33 - byteDiff}s| %-11s| %-5d".format(
            abbrev,
            englishName,
            nativeName,
            nativeLocale.displayLanguage,
            year
        )
    } else {
        "$abbrev, $englishName ($nativeName), ${nativeLocale.displayLanguage}, $year"
    }
}

private fun getByteLength(string: String): Int {
    return string.toByteArray(Charset.forName("UTF-8")).size
}

fun getTranslationDescriptions(): List<String> {
    return availableTranslations.map { it.getDescription(format = true) }
}

fun main() {
    getTranslationDescriptions().forEach { println(it) }
}