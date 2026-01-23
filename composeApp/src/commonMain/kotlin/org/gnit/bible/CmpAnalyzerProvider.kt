package org.gnit.bible

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

class CmpAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language) }
    }

    private fun createAnalyzer(language: Language): Analyzer {
        return when (language.code) {
            "en" -> EnglishAnalyzer() //common (embedded in cmp)
            "es" -> SpanishAnalyzer() //common (embedded in cmp)
            "pt" -> PortugueseAnalyzer() //common (embedded in cmp)
            "de" -> GermanAnalyzer() //common (embedded in cmp)
            "fr" -> FrenchAnalyzer() //common (embedded in cmp)
            "ru" -> RussianAnalyzer() //common (embedded in cmp)
            "nl" -> DutchAnalyzer() //common (embedded in cmp)
            "it" -> ItalianAnalyzer() //common (embedded in cmp)
            "sv" -> SwedishAnalyzer() //common (embedded in cmp)

            "pl" -> MorfologikAnalyzer() //morfologik (embedded in cmp)
            "uk" -> UkrainianMorfologikAnalyzer() //morfologik (embedded in cmp)

            "zh" -> SmartChineseAnalyzer() //smartcn (embedded in cmp)
            "ko" -> BibleKoreanAnalyzer() //nori (embedded in cmp)
            "ja" -> BibleJapaneseAnalyzer() //kuromoji (embedded in cmp)

            "id" -> IndonesianAnalyzer() //common (downloadable in cmp) ayt
            "th" -> ThaiAnalyzer() //common (downloadable in cmp) th 1971
            "hi" -> HindiAnalyzer() //common (downloadable in cmp) irvhin
            "bn" -> BengaliAnalyzer() //common (downloadable in cmp) irvben
            "ta" -> TamilAnalyzer() //common (downloadable in cmp) irvtam
            "ne" -> BibleNepaliAnalyzer() //common (downloadable in cmp) npiulb

            "tl" -> TagalogAnalyzer() //extra (downloadable in cmp) abtag
            "vi" -> VietnameseAnalyzer(VietnameseConfig()) //extra (downloadable in cmp) kttv
            "gu" -> GujaratiAnalyzer() //extra (downloadable in cmp) irvguj
            "mr" -> MarathiAnalyzer() //extra (downloadable in cmp) irvmar
            "te" -> TeluguAnalyzer() //extra (downloadable in cmp) irvtel
            "ur" -> UrduAnalyzer() //extra (downloadable in cmp) irvurd

            else -> SimpleAnalyzer()
        }
    }
}
