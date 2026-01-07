package org.gnit.bible.cli

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Language
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.de.GermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.SwedishAnalyzer

class CommonAnalyzerProvider : AnalyzerProvider {
    private val cache = mutableMapOf<String, Analyzer>()

    override fun analyzerFor(language: Language): Analyzer {
        return cache.getOrPut(language.code) { createAnalyzer(language.code) }
    }

    private fun createAnalyzer(code: String): Analyzer {
        return when (code) {
            "en" -> EnglishAnalyzer()
            "es" -> SpanishAnalyzer()
            "pt" -> PortugueseAnalyzer()
            "de" -> GermanAnalyzer()
            "fr" -> FrenchAnalyzer()
            "ru" -> RussianAnalyzer()
            "nl" -> DutchAnalyzer()
            "it" -> ItalianAnalyzer()
            "sv" -> SwedishAnalyzer()
            else -> SimpleAnalyzer()
        }
    }
}
