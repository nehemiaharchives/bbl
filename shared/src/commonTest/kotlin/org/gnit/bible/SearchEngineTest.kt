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

    @Test
    fun searchAppliesBibleFiltersFromAnalyzerProvider() {
        val directory = buildIndexDirectory(
            IndexedVerse(book = 4, chapter = 14, verse = 30, text = "jesus"),
            IndexedVerse(book = 40, chapter = 1, verse = 1, text = "jesus")
        )
        val reader = DirectoryBibleResourcesReader(directory)
        val provider = object : AnalyzerProvider {
            override fun analyzerFor(language: Language): Analyzer = SimpleAnalyzer()

            override fun bibleFiltersFor(language: Language, term: String): List<BibleFilter> {
                return listOf(Books.Category.NEW_TESTAMENT.filter)
            }
        }

        val engine = SearchEngine(reader, provider)
        val results = engine.search(term = "jesus", translation = Translation.webus)

        assertEquals(VersePointer(Translation.webus, 40, 1, 1), results.first())
    }

    @Test
    fun searchReturnsCanonicalOrderWithoutBibleFilter() {
        val directory = buildIndexDirectory(
            IndexedVerse(book = 4, chapter = 14, verse = 30, text = "jesus"),
            IndexedVerse(book = 40, chapter = 1, verse = 1, text = "jesus")
        )
        val reader = DirectoryBibleResourcesReader(directory)
        val provider = object : AnalyzerProvider {
            override fun analyzerFor(language: Language): Analyzer = SimpleAnalyzer()
        }

        val engine = SearchEngine(reader, provider)
        val results = engine.search(term = "jesus", translation = Translation.webus)

        assertEquals(VersePointer(Translation.webus, 4, 14, 30), results.first())
    }

    private fun buildIndexDirectory(vararg verses: IndexedVerse): ByteBuffersDirectory {
        val directory = ByteBuffersDirectory()
        val config = IndexWriterConfig(SimpleAnalyzer())
        val indexVerses = verses.toList().ifEmpty {
            listOf(IndexedVerse(book = 1, chapter = 1, verse = 1, text = "the quick brown fox"))
        }
        IndexWriter(directory, config).use { writer ->
            indexVerses.forEach { indexedVerse ->
                val doc = Document().apply {
                    add(IntPoint("book", indexedVerse.book))
                    add(StoredField("book", indexedVerse.book))

                    add(IntPoint("chapter", indexedVerse.chapter))
                    add(StoredField("chapter", indexedVerse.chapter))

                    add(IntPoint("verse", indexedVerse.verse))
                    add(StoredField("verse", indexedVerse.verse))

                    add(Field("text", indexedVerse.text, TextField.TYPE_STORED))
                }
                writer.addDocument(doc)
            }
        }
        return directory
    }

    private data class IndexedVerse(
        val book: Int,
        val chapter: Int,
        val verse: Int,
        val text: String
    )

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
