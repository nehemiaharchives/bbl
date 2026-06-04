package org.gnit.bible.test

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.FixturePackResourcesReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixturePackTest {

    private fun reader(): FixturePackResourcesReader {
        val fs = FakeFileSystem()
        val root = "/fixture_resources".toPath()
        val baseDir = root / "bblpacks" / "fixture"

        fs.createDirectories(baseDir / "index")
        fs.write<Unit>(baseDir / "fixture.1.1.txt") { writeUtf8("1 In the beginning...") }
        fs.write<Unit>(baseDir / "index" / "fixture.index.manifest") { writeUtf8("_0.cfs\n") }
        fs.write<Unit>(baseDir / "index" / "_0.cfs") { write(byteArrayOf(1, 2, 3)) }

        return FixturePackResourcesReader(resourcesRoot = root, fileSystem = fs)
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
