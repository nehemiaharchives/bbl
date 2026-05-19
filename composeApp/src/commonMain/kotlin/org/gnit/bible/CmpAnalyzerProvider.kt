package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.ct.BibleSpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.hi.HindiAnalyzer
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

    override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
        return when {
            language == Language.ru && BibleRussianAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            language == Language.sv && BibleSwedishAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            else -> emptyList()
        }
    }

    private fun createAnalyzer(language: Language): Analyzer {
        return when (language.code) {
            "en" -> EnglishAnalyzer() //common (embedded in cmp)
            "es" -> BibleSpanishAnalyzer() //common (embedded in cmp)
            "pt" -> BiblePortugueseAnalyzer() //common (embedded in cmp)
            "de" -> BibleGermanAnalyzer() //common (embedded in cmp)
            "fr" -> FrenchAnalyzer() //common (embedded in cmp)
            "ru" -> BibleRussianAnalyzer() //common (embedded in cmp)
            "nl" -> DutchAnalyzer() //common (embedded in cmp)
            "it" -> ItalianAnalyzer() //common (embedded in cmp)
            "sv" -> BibleSwedishAnalyzer() //common (embedded in cmp)

            "pl" -> MorfologikAnalyzer() //morfologik (embedded in cmp)
            "uk" -> UkrainianMorfologikAnalyzer() //morfologik (embedded in cmp)

            "zh" -> SmartChineseAnalyzer() //smartcn (embedded in cmp)
            "ko" -> BibleKoreanAnalyzer() //nori (embedded in cmp)
            "ja" -> BibleJapaneseAnalyzer() //kuromoji (embedded in cmp)

            "id" -> IndonesianAnalyzer() //common (downloadable in cmp) ayt
            "th" -> ThaiAnalyzer() //common (downloadable in cmp) th 1971
            "hi" -> HindiAnalyzer() //common (downloadable in cmp) irvhin
            "bn" -> BengaliAnalyzer() //common (downloadable in cmp) irvben
            "ta" -> BibleTamilAnalyzer() //common (downloadable in cmp) irvtam
            "ne" -> BibleNepaliAnalyzer() //common (downloadable in cmp) npiulb

            "tl" -> TagalogAnalyzer() //extra (downloadable in cmp) abtag
            "vi" -> VietnameseAnalyzer(VietnameseConfig()) //extra (downloadable in cmp) kttv
            "gu" -> GujaratiAnalyzer() //extra (downloadable in cmp) irvguj
            "mr" -> BibleMarathiAnalyzer() //extra (downloadable in cmp) irvmar
            "te" -> BibleTeluguAnalyzer() //extra (downloadable in cmp) irvtel
            "ur" -> UrduAnalyzer() //extra (downloadable in cmp) irvurd

            else -> SimpleAnalyzer()
        }
    }
}
