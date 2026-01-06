package org.gnit.bible.test

import okio.Path.Companion.toPath
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.FixturePackResourcesReader
import org.gnit.bible.Language
import org.gnit.bible.SearchEngine
import org.gnit.bible.Translation
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.core.SimpleAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FixturePackTest {

    private fun reader(): FixturePackResourcesReader {
        // Test harness must provide the resource root in a Kotlin-common-friendly way.
        // For JVM tests, the working directory is the repo root in our run config.
        return FixturePackResourcesReader(resourcesRoot = "src/commonTest/resources".toPath())
    }

    @Test
    fun fixturePackHasReadableChapterAndIndexManifest() {
        val reader = reader()

        val chapter = reader.getChapterText("fixture", book = 1, chapter = 1)
        assertTrue(chapter.startsWith("1 In the beginning"))

        val indexFiles = reader.listIndexFiles("fixture")
        assertEquals(listOf("_0.cfs"), indexFiles)

        val bytes = reader.readIndexFile("fixture", "_0.cfs")
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun fixturePackLayoutIsCompatibleWithSearchEngineResourceContract() {
        val reader = reader()
        val provider = object : AnalyzerProvider {
            override fun analyzerFor(language: Language): Analyzer = SimpleAnalyzer()
        }

        // We don't include a real Lucene index in the fixture (keeping it tiny).
        // This test only ensures SearchEngine can *attempt* to load the index bytes.
        val engine = SearchEngine(reader, provider)
        assertFails {
            engine.search(
                term = "beginning",
                translation = Translation.fromJson(reader.readByPath("bblpacks/fixture/fixture.manifest.json"))
            )
        }
    }
}