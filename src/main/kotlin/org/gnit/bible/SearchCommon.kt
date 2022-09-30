package org.gnit.bible

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.apache.lucene.analysis.ko.KoreanAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

fun getAnalyzer(translation: Translation): Analyzer {

    return when (translation) {
        Translation.webus, Translation.kjv -> StandardAnalyzer()
        Translation.cunp -> SmartChineseAnalyzer()
        Translation.krv -> KoreanAnalyzer()
        Translation.jc -> JapaneseAnalyzer()
    }
}

fun copyIndex(ramDirectory: ByteBuffersDirectory, destination: Path?) {
    val fsDirectory = FSDirectory.open(destination)
    Arrays.stream(ramDirectory.listAll())
        .forEach { fileName ->
            try {
                // IOContext is null because in fact is not used (at least for the moment)
                fsDirectory.copyFrom(ramDirectory, fileName, fileName, null)
            } catch (e: IOException) {
                logger.error(e.message)
            }
        }
}
