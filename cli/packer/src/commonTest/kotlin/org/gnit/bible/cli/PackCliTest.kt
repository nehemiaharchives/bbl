package org.gnit.bible.cli

import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.ZipBibleResourcesReader
import org.gnit.bible.getPlatform
import org.gnit.bible.test.FileUtil.deleteRecursively
import org.gnit.bible.test.TestFixtures
import org.gnit.bible.test.TestFixtures.tmpWorkingDirForBblPack
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackCliTest {

    private val logger = KotlinLogging.logger {}

    @BeforeTest
    fun setup() {
        val fileSystem = getPlatform().fileSystem

        val tmpDir = tmpWorkingDirForBblPack.toPath()
        if (fileSystem.exists(tmpDir)) {
            deleteRecursively(fileSystem, tmpDir)
        }
        fileSystem.createDirectories(tmpDir)

        val targetDir = tmpDir / "webus"
        fileSystem.createDirectories(targetDir)

        // Minimal set: manifest + one chapter file to keep the test fast.
        // Do NOT depend on composeApp resources (tests may run with a different working dir).
        val manifestPath = targetDir / "webus$MANIFEST_JSON_POSTFIX"
        fileSystem.write(manifestPath) { writeUtf8(Translation.webus.toJson()) }

        // Create a tiny chapter file; keep it deterministic and small.
        // The indexer splits verses by leading verse numbers ("1 ", "2 ", etc.).
        val chapterText = """
            ${TestFixtures.WEBUS_GENESIS_1_1.trimEnd()}
            2 And the earth was formless and empty.
            3 God said, Let there be light.
        """.trimIndent() + "\n"

        fileSystem.write(targetDir / "webus.1.1.txt") { writeUtf8(chapterText) }
    }

    @Test
    fun testGeneratedZipFilesByMain() {
        // This test used to assume that "real" packs are already committed under server resources.
        // In CI / local dev that’s often not true, and it makes the test flaky.
        // Instead, create a minimal pack and verify ZipBibleResourcesReader can load it.

        val fileSystem = getPlatform().fileSystem

        val inputDir = (tmpWorkingDirForBblPack.toPath() / "webus").toString()
        val outputDir = tmpWorkingDirForBblPack
        val zipPath = outputDir.toPath() / "webus.zip"

        try {
            PackCli(Bible()).createBblPack(inputPathString = inputDir, outputPathString = outputDir)
            assertTrue(fileSystem.exists(zipPath), "Expected zip file to be created at $zipPath")

            val platform = getPlatform().apply { overridePlatformPackDir = outputDir }
            val zipBibleResourcesReader = ZipBibleResourcesReader(platform)

            // Smoke-check that reading works.
            val genesisChapterOne = zipBibleResourcesReader.getChapterText("webus", 1, 1)
            assertTrue(genesisChapterOne.isNotBlank(), "Expected non-empty Genesis 1 for webus")
            assertContains(genesisChapterOne, "1 ")
        } finally {
            if (fileSystem.exists(zipPath)) {
                fileSystem.delete(zipPath)
            }
        }
    }

    @Test
    fun testBblPackWebusInTmpDir() {
        val fileSystem = getPlatform().fileSystem

        val inputDir = (tmpWorkingDirForBblPack.toPath() / "webus").toString()
        val outputDir = tmpWorkingDirForBblPack
        val zipPath = outputDir.toPath() / "webus.zip"

        try {
            PackCli(Bible()).createBblPack(inputPathString = inputDir, outputPathString = outputDir)
            assertTrue(fileSystem.exists(zipPath), "Expected zip file to be created at $zipPath")

            val platform = getPlatform().apply { overridePlatformPackDir = outputDir }
            val zipBibleResourcesReader = ZipBibleResourcesReader(platform)

            val expected =
                fileSystem.read(tmpWorkingDirForBblPack.toPath() / "webus" / "webus.1.1.txt") { readUtf8() }
            val actual = zipBibleResourcesReader.getChapterText("webus", 1, 1)
            assertEquals(expected, actual)

            val manifestTranslation = zipBibleResourcesReader.getTranslationFromManifest("webus")
            assertEquals("webus", manifestTranslation.code)
            assertEquals(bblCliVersion, manifestTranslation.bblVersion)

            // Verify lucene-kmp index files are included in the zip (and lock is not).
            val zipEntries = mutableListOf<String>()
            runBlocking {
                val zipFile = ZipFile(com.oldguy.common.io.File(zipPath.toString()))
                try {
                    zipFile.open()
                    zipEntries.addAll(zipFile.entries.map { it.name.replace('\\', '/') })
                } finally {
                    runCatching { zipFile.close() }.getOrNull()
                }
            }

            assertTrue(
                zipEntries.any { it.startsWith("index/segments_") },
                "Expected lucene-kmp index segments file in zip. Entries: ${zipEntries.take(50)}"
            )
            assertTrue(
                zipEntries.any {
                    it.startsWith("index/_") && (it.endsWith(".cfs") || it.endsWith(".si") || it.endsWith(".cfe"))
                },
                "Expected lucene-kmp index data files in zip. Entries: ${zipEntries.take(50)}"
            )
            assertTrue(
                zipEntries.none { it.endsWith("index/write.lock") },
                "Did not expect write.lock to be included in bblpack zip"
            )
        } finally {
            if (fileSystem.exists(zipPath)) {
                fileSystem.delete(zipPath)
            }
        }
    }

    @Test
    fun testBblPackBakesCurrentVersionIntoManifest() {
        val fileSystem = getPlatform().fileSystem

        val inputPath = tmpWorkingDirForBblPack.toPath() / "webus"
        val manifestPath = inputPath / "webus$MANIFEST_JSON_POSTFIX"
        val oldManifest = Translation.webus.copy(bblVersion = "0.0.1").toJson()
        fileSystem.write(manifestPath) { writeUtf8(oldManifest) }

        PackCli(Bible()).createBblPack(
            inputPathString = inputPath.toString(),
            outputPathString = tmpWorkingDirForBblPack,
            updateIndexOnly = true
        )

        val manifestTranslation = Translation.fromJson(fileSystem.read(manifestPath) { readUtf8() })
        assertEquals(bblCliVersion, manifestTranslation.bblVersion)
    }

    /**
     * This is not test but walk around the problem of PackCli.main() IntelliJ Edit view execute icon not working. So usually need to put @Ignore.
     * Run this only when creating/updating bblpack.
     */
    @Ignore
    @Test
    fun packTranslation() {
        packTranslation("sven")
    }
}
