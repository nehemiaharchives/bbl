package org.gnit.bible

import com.github.ajalt.clikt.core.CliktCommand
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
}

private const val BIBLE_TRANSLATION_NAMES = "bibleTranslationNames"

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

    fun getDescription(): String {
        val abbrev = this.toString().uppercase()
        val englishName = ResourceBundle.getBundle(BIBLE_TRANSLATION_NAMES, Locale("en")).getString(this.toString())
        val nativeLocale = Locale(this.language.toString())
        val nativeName = ResourceBundle.getBundle(BIBLE_TRANSLATION_NAMES, nativeLocale)
            .getString(this.toString())
        return "$abbrev, $englishName, ($nativeName), ${nativeLocale.displayLanguage}, $year"
    }
}

fun getTranslationDescriptions(): kotlin.collections.List<String> {
    return Translation.values().map { it.getDescription() }
}

class ListCli : CliktCommand(name = "list") {
    lateinit var translationDescriptions: kotlin.collections.List<String>
    override fun run() {
        translationDescriptions = getTranslationDescriptions()
        translationDescriptions.forEach { echo(it) }
    }
}

fun main() {
    getTranslationDescriptions().forEach { println(it) }
}