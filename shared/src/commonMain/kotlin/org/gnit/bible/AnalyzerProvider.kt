package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer

interface AnalyzerProvider {
    fun analyzerFor(language: Language): Analyzer

    fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> = emptyList()
}

class SimpleAnalyzerProvider : AnalyzerProvider {
    override fun analyzerFor(language: Language): Analyzer = SimpleAnalyzer()
}
