package org.gnit.bible.cli

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.Translation.Companion.embeddedTranslations
import org.gnit.bible.downloadableTranslations
import org.gnit.bible.getPlatform
import org.gnit.bible.test.FileUtil.deleteRecursively
import org.gnit.bible.test.TestFixtures.tmpWorkingDirForBblPack
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexBuilderTest {

    @Test
    fun createLuceneKmpIndexTest() {
        val fileSystem = getPlatform().fileSystem
        val translationDir = tmpWorkingDirForBblPack.toPath() / "webus"
        val indexDir = translationDir / "index"

        if (fileSystem.exists(indexDir)) {
            deleteRecursively(fileSystem, indexDir)
        }

        val chapterText = fileSystem.read(translationDir / "webus.1.1.txt") { readUtf8() }
        val expectedDocCount = Bible.splitChapterToVerses(chapterText).size

        val actualDocCount =
            IndexBuilder(Bible()).createLuceneKmpIndex(
                translation = Translation.webus,
                translationDir = translationDir
            )

        assertEquals(expectedDocCount, actualDocCount)
        assertTrue(
            fileSystem.exists(indexDir) && fileSystem.metadata(indexDir).isDirectory,
            "Expected index directory to exist at $indexDir"
        )

        val entryNames = fileSystem.list(indexDir).map { it.name }
        assertTrue(
            entryNames.any { it.startsWith("segments_") },
            "Expected a segments_N file in $indexDir, got: $entryNames"
        )

        val manifestPath = indexDir / "webus.index.manifest"
        assertTrue(
            fileSystem.exists(manifestPath) && fileSystem.metadata(manifestPath).isRegularFile,
            "Expected index manifest file to exist at $manifestPath"
        )

        val manifestContent = fileSystem.read(manifestPath) { readUtf8() }
        val manifestLines = manifestContent
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        assertTrue(
            manifestLines.size >= 2,
            "Expected index manifest to contain multiple lines (files). Content:\n$manifestContent"
        )

        // Ensure entries are relative file names (not paths) and do not include the manifest itself.
        assertTrue(
            manifestLines.all { !it.contains("/") && !it.contains("\\") },
            "Expected manifest entries to be file names only. Lines: $manifestLines"
        )
        assertTrue(
            manifestLines.none { it == manifestPath.name },
            "Did not expect manifest to list itself. Lines: $manifestLines"
        )
        // Ensure lock file is never listed.
        assertTrue(
            manifestLines.none { it == "write.lock" },
            "Did not expect write.lock in index manifest. Lines: $manifestLines"
        )

        // Sanity: all listed files should exist in `indexDir`.
        manifestLines.forEach { entry ->
            val p = indexDir / entry
            assertTrue(
                fileSystem.exists(p) && fileSystem.metadata(p).isRegularFile,
                "Expected manifest entry to exist as file: $p"
            )
        }
    }

    @Test
    fun embeddedIndexExistenceTest() {

        val fs = FileSystem.SYSTEM

        embeddedTranslations.forEach { translation ->
            val translationDir =
                "../composeApp/src/commonMain/composeResources/files/bblpacks/${translation.code}".toPath()

            assertTrue(fs.exists(translationDir / "index"))
            val indexManifest = translationDir / "index" / "${translation.code}.index.manifest"
            assertTrue(fs.exists(indexManifest))
            assertTrue(fs.metadata(indexManifest).isRegularFile)
            assertTrue(fs.read(indexManifest) { readUtf8() }.isNotEmpty())
        }
    }

    @Test
    fun downloadableIndexExistenceTest() {

        val fs = FileSystem.SYSTEM

        downloadableTranslations.forEach { translation ->
            val translationDir = "../server/src/main/resources/files/bbltexts/${translation.code}".toPath()
            assertTrue(fs.exists(translationDir / "index"))
            val indexManifest = translationDir / "index" / "${translation.code}.index.manifest"
            assertTrue(fs.exists(indexManifest))
            assertTrue(fs.metadata(indexManifest).isRegularFile)
            assertTrue(fs.read(indexManifest) { readUtf8() }.isNotEmpty())
        }
    }

    @Test
    @Ignore
    fun createLuceneKmpIndexInProductionEnvTest() {
        val translation = Translation.webus
        createEmbeddedLuceneKmpIndex(translation)
    }
}
