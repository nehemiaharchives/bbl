package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.ct.BibleBengaliAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.ct.BibleEnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.ct.BibleSpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.hi.ct.BibleHindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.ct.BiblePortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.ct.BibleRussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.ct.BibleTamilAnalyzer
import org.gnit.lucenekmp.analysis.te.ct.BibleTeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
        return when {
            language == Language.ru && BibleRussianAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            language == Language.sv && BibleSwedishAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            else -> emptyList()
        }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "en" -> BibleEnglishAnalyzer() //common
            "es" -> BibleSpanishAnalyzer() //common
            "pt" -> BiblePortugueseAnalyzer() //common
            "de" -> BibleGermanAnalyzer() //common
            "fr" -> FrenchAnalyzer() //common
            "ru" -> BibleRussianAnalyzer() //common
            "nl" -> DutchAnalyzer() //common
            "it" -> ItalianAnalyzer() //common
            //"pl" -> MorfologikAnalyzer() //morfologik
            //"uk" -> UkrainianMorfologikAnalyzer() //morfologik
            "sv" -> BibleSwedishAnalyzer() //common
            //"zh" -> SmartChineseAnalyzer() //smartcn
            //"ko" -> BibleKoreanAnalyzer() //nori
            //"ja" -> JapaneseAnalyzer() //kuromoji
            //"tl" -> TagalogAnalyzer() //extra
            "id" -> IndonesianAnalyzer() //common
            //"vi" -> VietnameseAnalyzer(VietnameseConfig()) //extra
            "th" -> ThaiAnalyzer() //common
            "hi" -> BibleHindiAnalyzer() //common
            "bn" -> BibleBengaliAnalyzer() //common
            //"mr" -> MarathiAnalyzer() //extra
            "te" -> BibleTeluguAnalyzer() //common
            "ta" -> BibleTamilAnalyzer() //common
            //"gu" -> GujaratiAnalyzer() //extra
            //"ur" -> UrduAnalyzer() //extra
            "ne" -> BibleNepaliAnalyzer() //common
            else -> SimpleAnalyzer()
        }
    }
}
