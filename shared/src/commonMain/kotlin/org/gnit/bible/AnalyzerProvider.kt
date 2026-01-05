package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer

interface AnalyzerProvider {
    fun analyzerFor(language: Language): Analyzer
}
