package org.gnit.bible.test

import okio.Path.Companion.toPath
import org.gnit.bible.FixturePackResourcesReader
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
