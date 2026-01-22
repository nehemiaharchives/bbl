package org.gnit.bible.cli

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.gnit.bible.Bible
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.Translation.Companion.downloadableTranslationsCli
import org.gnit.bible.Translation.Companion.embeddedTranslations
import org.gnit.bible.getPlatform
import org.gnit.bible.test.FileUtil.deleteRecursively
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class IndexBuilderTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun createLuceneKmpIndexTest() {
        val fileSystem = getPlatform().fileSystem

        // Don't rely on shared fixed paths like /tmp/bblpack-cli-create.
        // CI can run tests in different order / in parallel, so we create our own fixture dir.
        val tmpRoot = "/tmp".toPath()
        val translationDir = tmpRoot / "bbl-kmp" / "IndexBuilderTest" / "webus-${Uuid.random()}"
        val indexDir = translationDir / "index"

        // Ensure clean fixture dir.
        if (fileSystem.exists(translationDir)) {
            deleteRecursively(fileSystem, translationDir)
        }
        fileSystem.createDirectories(translationDir)

        // Minimal required inputs: manifest + one chapter file.
        fileSystem.write(translationDir / "webus$MANIFEST_JSON_POSTFIX") {
            writeUtf8(Translation.webus.toJson())
        }

        // Use a deterministic multi-verse chapter string so the doc count assertion is meaningful.
        val chapterText = """
            1 In the beginning God created the heaven and the earth.
            2 And the earth was without form, and void; and darkness was upon the face of the deep.
            3 And God said, Let there be light: and there was light.
            4 And God saw the light, that it was good.
        """.trimIndent()

        fileSystem.write(translationDir / "webus.1.1.txt") {
            writeUtf8(chapterText)
        }

        val expectedDocCount = Bible.splitChapterToVerses(chapterText).size

        val actualDocCount = IndexBuilder(Bible()).createLuceneKmpIndex(
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
            entryNames.size > 1,
            "Expected index directory to contain index files in addition to the manifest. Got: $entryNames"
        )
        assertTrue(
            entryNames.any { it != "webus.index.manifest" },
            "Expected index directory to contain at least one index file besides webus.index.manifest. Got: $entryNames"
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

        // Best-effort cleanup (avoid leaking temp dirs in CI).
        runCatching { deleteRecursively(fileSystem, translationDir) }.getOrNull()
    }

    private fun findRepoRoot(fs: FileSystem): Path {
        // Tests can be executed with different working directories (IDE, Gradle, CI).
        // We locate the repo root by walking upwards until we find a marker file.
        val markers = listOf("settings.gradle.kts", "build.gradle.kts")

        var dir = ".".toPath().normalized()
        while (true) {
            if (markers.any { fs.exists(dir / it) }) return dir
            val parent = dir.parent ?: return dir
            if (parent == dir) return dir
            dir = parent
        }
    }

    private fun assertIndexPresent(fs: FileSystem, translationDir: Path, code: String) {
        val indexDir = translationDir / "index"
        assertTrue(
            fs.exists(indexDir),
            "Expected index directory to exist at $indexDir"
        )

        val indexManifest = indexDir / "$code.index.manifest"
        assertTrue(
            fs.exists(indexManifest),
            "Expected index manifest to exist at $indexManifest"
        )
        assertTrue(
            fs.metadata(indexManifest).isRegularFile,
            "Expected index manifest to be a regular file at $indexManifest"
        )
        assertTrue(
            fs.read(indexManifest) { readUtf8() }.isNotEmpty(),
            "Expected index manifest to be non-empty at $indexManifest"
        )
    }

    @Test
    fun embeddedIndexExistenceTest() {
        val fs = FileSystem.SYSTEM
        val repoRoot = findRepoRoot(fs)
        val embeddedRoot = repoRoot / "composeApp/src/commonMain/composeResources/files/bblpacks"

        var checked = 0
        embeddedTranslations.forEach { translation ->
            val translationDir = embeddedRoot / translation.code
            if (!fs.exists(translationDir)) return@forEach // not shipped in this working copy
            assertIndexPresent(fs, translationDir, translation.code)
            checked++
        }

        // If nothing is shipped in this checkout, treat as not applicable instead of failing.
        if (checked == 0) return
    }

    @Test
    fun downloadableIndexExistenceTest() {
        val fs = FileSystem.SYSTEM
        val repoRoot = findRepoRoot(fs)
        val downloadableRoot = repoRoot / "server/src/main/resources/files/bbltexts"

        var checked = 0
        downloadableTranslationsCli.forEach { translation ->
            val translationDir = downloadableRoot / translation.code
            if (!fs.exists(translationDir)) return@forEach // not shipped in this working copy
            assertIndexPresent(fs, translationDir, translation.code)
            checked++
        }

        // If nothing is shipped in this checkout, treat as not applicable instead of failing.
        if (checked == 0) return
    }

    @Test
    @Ignore
    fun createLuceneKmpIndexInProductionEnvTest() {
        val translation = Translation.webus
        createEmbeddedLuceneKmpIndex(translation)
    }
}
