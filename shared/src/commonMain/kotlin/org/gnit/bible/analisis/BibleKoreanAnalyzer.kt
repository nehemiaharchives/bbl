package org.gnit.bible.analisis

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.LowerCaseFilter
import org.gnit.lucenekmp.analysis.TokenStream
import org.gnit.lucenekmp.analysis.Tokenizer
import org.gnit.lucenekmp.analysis.ko.KoreanPartOfSpeechStopFilter
import org.gnit.lucenekmp.analysis.ko.KoreanReadingFormFilter
import org.gnit.lucenekmp.analysis.ko.KoreanTokenizer
import org.gnit.lucenekmp.analysis.ko.KoreanTokenizer.DecompoundMode
import org.gnit.lucenekmp.analysis.ko.POS
import org.gnit.lucenekmp.analysis.ko.dict.UserDictionary
import org.gnit.lucenekmp.jdkport.StringReader

/**
 * Analyzer for Korean that uses morphological analysis.
 * Adds optional stop words (default includes the possessive particle "의").
 */
class BibleKoreanAnalyzer(
    private val userDict: UserDictionary? = BibleKoreanUserDictionary.instance,
    private val mode: DecompoundMode = KoreanTokenizer.DEFAULT_DECOMPOUND,
    private val stopTags: Set<POS.Tag> = KoreanPartOfSpeechStopFilter.DEFAULT_STOP_TAGS,
    private val outputUnknownUnigrams: Boolean = false,
    private val stopWords: Set<String> = setOf("의")
) : Analyzer() {

    override fun createComponents(fieldName: String): TokenStreamComponents {
        val tokenizer: Tokenizer = KoreanTokenizer(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, userDict, mode, outputUnknownUnigrams)
        var stream: TokenStream = KoreanPartOfSpeechStopFilter(tokenizer, stopTags)
        stream = KoreanReadingFormFilter(stream)
        stream = LowerCaseFilter(stream)

        if (stopWords.isNotEmpty()) {
            val lowerStop = stopWords.map { it.lowercase() }.toSet()
            stream = SimpleStopFilter(stream, lowerStop)
        }

        return TokenStreamComponents(tokenizer, stream)
    }

    override fun normalize(fieldName: String, `in`: TokenStream): TokenStream {
        return LowerCaseFilter(`in`)
    }
}

private object BibleKoreanUserDictionary {
    private const val ENTRIES = "그리스도"

    val instance: UserDictionary? by lazy {
        runCatching { UserDictionary.open(StringReader(ENTRIES)) }
            .getOrElse { throw IllegalStateException("Failed to load Bible Korean user dictionary.", it) }
    }
}
