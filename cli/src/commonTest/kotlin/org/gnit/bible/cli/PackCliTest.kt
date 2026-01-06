package org.gnit.bible.cli

import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.ZipBibleResourcesReader
import org.gnit.bible.downloadableTranslationCodeList
import org.gnit.bible.getPlatform
import org.gnit.bible.test.FileUtil.deleteRecursively
import org.gnit.bible.test.TestFixtures.tmpWorkingDirForBblPack
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackCliTest {

    val logger = KotlinLogging.logger {}

    @BeforeTest
    fun setup() {
        val fileSystem = getPlatform().fileSystem

        val tmpDir = tmpWorkingDirForBblPack.toPath()
        if (fileSystem.exists(tmpDir)) {
            deleteRecursively(fileSystem, tmpDir)
        }
        fileSystem.createDirectories(tmpDir)

        val sourceDir =
            "../composeApp/src/commonMain/composeResources/files/bblpacks/webus".toPath()
        assertTrue(
            fileSystem.exists(sourceDir) && fileSystem.metadata(sourceDir).isDirectory,
            "Expected webus bblpack dir to exist at $sourceDir"
        )

        val targetDir = tmpDir / "webus"
        fileSystem.createDirectories(targetDir)

        // Minimal set: manifest + one chapter file to keep the test fast.
        val manifestPath = targetDir / "webus$MANIFEST_JSON_POSTFIX"
        fileSystem.write(manifestPath) { writeUtf8(Translation.webus.toJson()) }

        val chapterPath = sourceDir / "webus.1.1.txt"
        assertTrue(
            fileSystem.exists(chapterPath) && fileSystem.metadata(chapterPath).isRegularFile,
            "Expected chapter file to exist at $chapterPath"
        )
        val chapterText = fileSystem.read(chapterPath) { readUtf8() }
        fileSystem.write(targetDir / "webus.1.1.txt") { writeUtf8(chapterText) }
    }

    @Test
    fun testGeneratedZipFilesByMain() {
        val platform = getPlatform()
        platform.overridePlatformPackDir = "../server/src/main/resources/files/bblpacks"
        val zipBibleResourcesReader = ZipBibleResourcesReader(platform)
        downloadableTranslationCodeList.forEach { translationCode ->
            val genesisChapterOne = zipBibleResourcesReader.getChapterText(translationCode, 1, 1)

            // Packs in tests/fixtures are intentionally minimal (often only Gen 1:1),
            // so don't assert specific verse ranges here.
            assertTrue(genesisChapterOne.isNotBlank(), "Expected non-empty Genesis 1 for $translationCode")
            assertContains(genesisChapterOne, "1 ")

            logger.info { "translationCode: $translationCode\ngenesisChapterOne:\n$genesisChapterOne" }
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
                    it.startsWith("index/_") && (it.endsWith(".cfs") || it.endsWith(".si") || it.endsWith(
                        ".cfe"
                    ))
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
}
