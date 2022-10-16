package org.gnit.bible

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.es.SpanishAnalyzer
import org.apache.lucene.analysis.fr.FrenchAnalyzer
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.ko.KoreanAnalyzer
import org.apache.lucene.analysis.morfologik.MorfologikAnalyzer
import org.apache.lucene.analysis.nl.DutchAnalyzer
import org.apache.lucene.analysis.pt.PortugueseAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.sv.SwedishAnalyzer
import org.apache.lucene.analysis.uk.UkrainianMorfologikAnalyzer
import java.nio.charset.Charset
import java.util.*

enum class Language {
    en, es, pt, de, fr, ru, nl, it, pl, uk, sv, zh, ko, ja;

    fun getAnalyzer(): Analyzer {

        return when (this) {
            en -> EnglishAnalyzer()
            es -> SpanishAnalyzer()
            pt -> PortugueseAnalyzer()
            de -> GermanAnalyzer()
            fr -> FrenchAnalyzer()
            ru -> RussianAnalyzer()
            nl -> DutchAnalyzer()
            it -> ItalianAnalyzer()
            pl -> MorfologikAnalyzer() // for Polish
            uk -> UkrainianMorfologikAnalyzer() // for Ukrainian
            sv -> SwedishAnalyzer()
            zh -> SmartChineseAnalyzer()
            ko -> KoreanAnalyzer()
            ja -> JapaneseAnalyzer()
        }
    }

    fun isCJK() = when (this) {
        zh, ja, ko -> true
        else -> false
    }
}

private const val BIBLES = "bibleTranslationNames"

/**
Priority is based on population of software developers by country.
[ref1](https://aster.cloud/2021/11/18/how-many-software-developers-are-there-in-the-world/)
[ref2](https://qubit-labs.com/how-many-programmers-in-the-world/)
 */
enum class Translation(val language: Language, val year: Int) {
    // English World
    webus(Language.en, 2000), // webus 206 english, World English Bible
    kjv(Language.en, 1611), //King James Version

    // Latin America
    rvr09(Language.es, 1909), // rvr09,        Spanish,    Reina Valera
    tb(Language.pt, 1917),    // tb,           Portuguese, Tradução Brasileira

    // Europe
    delut(Language.de, 1912), // delut,        German,     Lutherbibel

    // UK already included in English above
    lsg(Language.fr, 1910),  // lsg,          French,     Louis Segond Bible
    sinod(Language.ru, 1876), // СИНОД(sinod), Russian,    Синодальный перевод
    svrj(Language.nl, 1888), // SV-RJ(svrj),  Dutch,      Statenvertaling Jongbloed-editie
    rdv24(Language.it, 1924), // rdv24,        Italian,    Revised Diodati Version

    // Spain  already included in Spanish above
    ubg(Language.pl, 2017), // ubg,          Polish,     Uwspółcześniona Biblia gdańska
    ubio(Language.uk, 1962), // ubio,         Ukrainian,  Біблія в пер. Івана Огієнка
    sven(Language.sv, 1917), // sven,         swedish,    1917 års kyrkobibel

    // North East Asia
    cunp(Language.zh, 1919),// cunp 48 chinese, Chinese Union Version with New Punctuation
    krv(Language.ko, 1961),// krv 88 korean, Korean Revised Version, 개역한글
    jc(Language.ja, 1955);

    // South and South East Asia
    // TODO India(none English), Vietnam, Singapore, Philippines

    fun getAnalyzer() = this.language.getAnalyzer()

    override fun toString(): String {
        return super.toString().lowercase()
    }

    /**
     * CJK characters break string format length. So they have to be detected and adjusted.
     * [ref](https://qiita.com/gsy0911/items/00876d8c61ce36bd5fba)
     */
    fun getDescription(format: Boolean = false): String {
        val abbrev = this.toString().uppercase()
        val englishName = ResourceBundle.getBundle(BIBLES, Locale("en")).getString(this.toString())
        val nativeLocale = Locale(this.language.toString())
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
}

private fun getByteLength(string: String): Int {
    return string.toByteArray(Charset.forName("UTF-8")).size
}

fun getTranslationDescriptions(): List<String> {
    return Translation.values().map { it.getDescription(format = true) }
}

fun main() {
    getTranslationDescriptions().forEach { println(it) }
}