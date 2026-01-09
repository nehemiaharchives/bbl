package org.gnit.bible.analisis

import org.gnit.lucenekmp.analysis.TokenStream
import org.gnit.lucenekmp.analysis.tokenattributes.CharTermAttribute

/**
 * Very small stop filter that removes tokens whose term (already lowercased) is in the provided set.
 */
class SimpleStopFilter(
    private val input: TokenStream,
    stopWords: Set<String>
) : TokenStream() {

    private val stopSet: Set<String> = stopWords
    private val termAttr: CharTermAttribute = addAttribute(CharTermAttribute::class)

    override fun incrementToken(): Boolean {
        while (input.incrementToken()) {
            val srcTermAttr = input.getAttribute(CharTermAttribute::class)
            val srcTerm = srcTermAttr.toString()
            termAttr.setEmpty()
            termAttr.append(srcTerm)

            if (srcTerm !in stopSet) {
                return true
            }
            // skip this token and continue
        }
        return false
    }

    override fun reset() {
        input.reset()
    }

    override fun end() {
        input.end()
    }

    override fun close() {
        input.close()
    }
}
