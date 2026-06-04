package org.gnit.bible.cli

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.BibleFilter
import org.gnit.bible.Language
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.ct.BibleGermanAnalyzer
import org.gnit.lucenekmp.analysis.ne.ct.BibleNepaliAnalyzer
import org.gnit.lucenekmp.analysis.sv.ct.BibleSwedishAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
        return emptyList()
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "de" -> BibleGermanAnalyzer()
            "sv" -> BibleSwedishAnalyzer()
            "bn" -> BengaliAnalyzer()
            "ne" -> BibleNepaliAnalyzer()
            else -> SimpleAnalyzer()
        }
    }
}
