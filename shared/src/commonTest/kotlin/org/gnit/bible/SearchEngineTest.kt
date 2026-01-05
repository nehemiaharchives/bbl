package org.gnit.bible

import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.document.Document
import org.gnit.lucenekmp.document.Field
import org.gnit.lucenekmp.document.IntPoint
import org.gnit.lucenekmp.document.StoredField
import org.gnit.lucenekmp.document.TextField
import org.gnit.lucenekmp.index.IndexWriter
import org.gnit.lucenekmp.index.IndexWriterConfig
import org.gnit.lucenekmp.store.ByteBuffersDirectory
import org.gnit.lucenekmp.store.IOContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchEngineTest {

    @Test
    fun analyzerProviderInvoked() {
        val directory = buildIndexDirectory()
        val reader = DirectoryBibleResourcesReader(directory)
        val provider = object : AnalyzerProvider {
            var calls = 0
            override fun analyzerFor(language: Language): Analyzer {
                calls += 1
                return SimpleAnalyzer()
            }
        }

        val engine = SearchEngine(reader, provider)
        val results = engine.search(term = "the", translation = Translation.webus)

        assertTrue(results.isNotEmpty(), "Expected at least one search result")
        assertEquals(1, provider.calls)
    }

    @Test
    fun searchFallsBackToSimpleAnalyzerForStopWords() {
        val directory = buildIndexDirectory()
        val reader = DirectoryBibleResourcesReader(directory)
        val provider = object : AnalyzerProvider {
            override fun analyzerFor(language: Language): Analyzer = EnglishAnalyzer()
        }

        val engine = SearchEngine(reader, provider)
        val results = engine.search(term = "the", translation = Translation.webus)

        assertTrue(results.isNotEmpty(), "Expected fallback search result for stop-word query")
    }

    private fun buildIndexDirectory(): ByteBuffersDirectory {
        val directory = ByteBuffersDirectory()
        val config = IndexWriterConfig(SimpleAnalyzer())
        IndexWriter(directory, config).use { writer ->
            val doc = Document().apply {
                add(IntPoint("book", 1))
                add(StoredField("book", 1))

                add(IntPoint("chapter", 1))
                add(StoredField("chapter", 1))

                add(IntPoint("verse", 1))
                add(StoredField("verse", 1))

                add(Field("text", "the quick brown fox", TextField.TYPE_STORED))
            }
            writer.addDocument(doc)
        }
        return directory
    }

    private class DirectoryBibleResourcesReader(
        private val directory: ByteBuffersDirectory
    ) : BibleResourcesReader {
        override fun chapterFile(translation: String, book: Int, chapter: Int): String = ""

        override fun readByPath(path: String): String = ""

        override fun listIndexFiles(translation: String): List<String> {
            return directory.listAll().toList()
        }

        override fun readIndexFile(translation: String, name: String): ByteArray {
            directory.openInput(name, IOContext.READONCE).use { input ->
                val length = input.length().toInt()
                val bytes = ByteArray(length)
                input.readBytes(bytes, 0, length)
                return bytes
            }
        }
    }
}
