package org.gnit.bible.app

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.Language
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.en.ct.BibleEnglishAnalyzer
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
import org.gnit.lucenekmp.analysis.bn.ct.BibleBengaliAnalyzer
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
            language == Language.uk && BibleUkrainianAnalyzer.requiresNewTestamentScope(term) ->
                listOf(Books.Category.NEW_TESTAMENT.filter)
            else -> emptyList()
        }
    }

    private fun createAnalyzer(language: Language): Analyzer {
        return when (language.code) {
            "en" -> BibleEnglishAnalyzer()
            "es" -> BibleSpanishAnalyzer()
            "pt" -> BiblePortugueseAnalyzer()
            "de" -> BibleGermanAnalyzer()
            "fr" -> FrenchAnalyzer()
            "ru" -> BibleRussianAnalyzer()
            "nl" -> DutchAnalyzer()
            "it" -> ItalianAnalyzer()
            "sv" -> BibleSwedishAnalyzer()

            "pl" -> MorfologikAnalyzer()
            "uk" -> BibleUkrainianAnalyzer()

            "zh" -> SmartChineseAnalyzer()
            "ko" -> BibleKoreanAnalyzer()
            "ja" -> BibleJapaneseAnalyzer()
            "id" -> IndonesianAnalyzer()
            "th" -> ThaiAnalyzer()
            "hi" -> BibleHindiAnalyzer()
            "bn" -> BibleBengaliAnalyzer()
            "ta" -> BibleTamilAnalyzer()
            "ne" -> BibleNepaliAnalyzer()

            "tl" -> BibleTagalogAnalyzer()
            "vi" -> BibleVietnameseAnalyzer()
            "gu" -> GujaratiAnalyzer()
            "mr" -> BibleMarathiAnalyzer()
            "te" -> BibleTeluguAnalyzer()
            "ur" -> UrduAnalyzer()

            else -> SimpleAnalyzer()
        }
    }
}
