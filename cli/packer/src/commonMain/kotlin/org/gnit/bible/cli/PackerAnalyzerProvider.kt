package org.gnit.bible.cli

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Language
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.GermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.hi.HindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ja.ct.BibleJapaneseAnalyzer
import org.gnit.lucenekmp.analysis.ko.ct.BibleKoreanAnalyzer
import org.gnit.lucenekmp.analysis.morfologik.MorfologikAnalyzer
import org.gnit.lucenekmp.analysis.mr.MarathiAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.SwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.TamilAnalyzer
import org.gnit.lucenekmp.analysis.te.TeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer
import org.gnit.lucenekmp.analysis.tl.TagalogAnalyzer
import org.gnit.lucenekmp.analysis.uk.UkrainianMorfologikAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseConfig

class PackerAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language) }
    }

    private fun createAnalyzer(language: Language): Analyzer {
        return when (language.code) {
            "en" -> EnglishAnalyzer() //common
            "es" -> SpanishAnalyzer() //common
            "pt" -> PortugueseAnalyzer() //common
            "de" -> GermanAnalyzer() //common
            "fr" -> FrenchAnalyzer() //common
            "ru" -> RussianAnalyzer() //common
            "nl" -> DutchAnalyzer() //common
            "it" -> ItalianAnalyzer() //common
            "pl" -> MorfologikAnalyzer() //morfologik
            "uk" -> UkrainianMorfologikAnalyzer() //morfologik
            "sv" -> SwedishAnalyzer() //common
            "zh" -> SmartChineseAnalyzer() //smartcn
            "ko" -> BibleKoreanAnalyzer() //nori
            "ja" -> BibleJapaneseAnalyzer() //kuromoji
            "tl" -> TagalogAnalyzer() //extra
            "id" -> IndonesianAnalyzer() //common
            "vi" -> VietnameseAnalyzer(VietnameseConfig()) //extra
            "th" -> ThaiAnalyzer() //common
            "hi" -> HindiAnalyzer() //common
            "bn" -> BengaliAnalyzer() //common
            "mr" -> MarathiAnalyzer() //extra
            "te" -> TeluguAnalyzer() //common
            "ta" -> TamilAnalyzer() //common
            "gu" -> GujaratiAnalyzer() //extra
            "ur" -> UrduAnalyzer() //extra
            "ne" -> BibleNepaliAnalyzer() //common
            else -> SimpleAnalyzer()
        }
    }
}
