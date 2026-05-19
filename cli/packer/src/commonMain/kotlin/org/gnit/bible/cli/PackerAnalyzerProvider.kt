package org.gnit.bible.cli

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Language
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.ct.BibleBengaliAnalyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.ct.BibleSpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.hi.ct.BibleHindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ja.ct.BibleJapaneseAnalyzer
import org.gnit.lucenekmp.analysis.ko.ct.BibleKoreanAnalyzer
import org.gnit.lucenekmp.analysis.morfologik.MorfologikAnalyzer
import org.gnit.lucenekmp.analysis.mr.ct.BibleMarathiAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.ct.BiblePortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.ct.BibleRussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.ct.BibleTamilAnalyzer
import org.gnit.lucenekmp.analysis.te.ct.BibleTeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer
import org.gnit.lucenekmp.analysis.tl.ct.BibleTagalogAnalyzer
import org.gnit.lucenekmp.analysis.uk.ct.BibleUkrainianAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.ct.BibleVietnameseAnalyzer

class PackerAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language) }
    }

    private fun createAnalyzer(language: Language): Analyzer {
        return when (language.code) {
            "en" -> EnglishAnalyzer() //common
            "es" -> BibleSpanishAnalyzer() //common
            "pt" -> BiblePortugueseAnalyzer() //common
            "de" -> BibleGermanAnalyzer() //common
            "fr" -> FrenchAnalyzer() //common
            "ru" -> BibleRussianAnalyzer() //common
            "nl" -> DutchAnalyzer() //common
            "it" -> ItalianAnalyzer() //common
            "pl" -> MorfologikAnalyzer() //morfologik
            "uk" -> BibleUkrainianAnalyzer() //morfologik
            "sv" -> BibleSwedishAnalyzer() //common
            "zh" -> SmartChineseAnalyzer() //smartcn
            "ko" -> BibleKoreanAnalyzer() //nori
            "ja" -> BibleJapaneseAnalyzer() //kuromoji
            "tl" -> BibleTagalogAnalyzer() //extra
            "id" -> IndonesianAnalyzer() //common
            "vi" -> BibleVietnameseAnalyzer() //extra
            "th" -> ThaiAnalyzer() //common
            "hi" -> BibleHindiAnalyzer() //common
            "bn" -> BibleBengaliAnalyzer() //common
            "mr" -> BibleMarathiAnalyzer() //extra
            "te" -> BibleTeluguAnalyzer() //common
            "ta" -> BibleTamilAnalyzer() //common
            "gu" -> GujaratiAnalyzer() //extra
            "ur" -> UrduAnalyzer() //extra
            "ne" -> BibleNepaliAnalyzer() //common
            else -> SimpleAnalyzer()
        }
    }
}
