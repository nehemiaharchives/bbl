package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.GermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.hi.HindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ne.NepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.SwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.TamilAnalyzer
import org.gnit.lucenekmp.analysis.te.TeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "en" -> EnglishAnalyzer() //common
            "es" -> SpanishAnalyzer() //common
            "pt" -> PortugueseAnalyzer() //common
            "de" -> GermanAnalyzer() //common
            "fr" -> FrenchAnalyzer() //common
            "ru" -> RussianAnalyzer() //common
            "nl" -> DutchAnalyzer() //common
            "it" -> ItalianAnalyzer() //common
            //"pl" -> MorfologikAnalyzer() //morfologik
            //"uk" -> UkrainianMorfologikAnalyzer() //morfologik
            "sv" -> SwedishAnalyzer() //common
            //"zh" -> SmartChineseAnalyzer() //smartcn
            //"ko" -> BibleKoreanAnalyzer() //nori
            //"ja" -> JapaneseAnalyzer() //kuromoji
            //"tl" -> TagalogAnalyzer() //extra
            "id" -> IndonesianAnalyzer() //common
            //"vi" -> VietnameseAnalyzer(VietnameseConfig()) //extra
            "th" -> ThaiAnalyzer() //common
            "hi" -> HindiAnalyzer() //common
            "bn" -> BengaliAnalyzer() //common
            //"mr" -> MarathiAnalyzer() //extra
            "te" -> TeluguAnalyzer() //common
            "ta" -> TamilAnalyzer() //common
            //"gu" -> GujaratiAnalyzer() //extra
            //"ur" -> UrduAnalyzer() //extra
            "ne" -> NepaliAnalyzer() //common
            else -> SimpleAnalyzer()
        }
    }
}
