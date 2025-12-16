package org.gnit.bible.cli

import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.ZipBibleTextReader
import org.gnit.bible.downloadableTranslationCodeList
import org.gnit.bible.getPlatform
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackCliTest {

    @Test
    fun testGeneratedZipFilesByMain() {
        val platform = getPlatform()
        platform.overridePlatformPackDir = "../server/src/main/resources/files/bblpacks"
        val zipBibleTextReader = ZipBibleTextReader(platform)
        downloadableTranslationCodeList.forEach { translationCode ->
            val genesisChapterOne = zipBibleTextReader.getChapterText(translationCode, 1, 1)
            (1..16).forEach { verseNumber ->
                assertContains(genesisChapterOne, "$verseNumber ")
            }
            logger.info { "translationCode: $translationCode\ngenesisChapterOne:\n$genesisChapterOne" }
        }
    }


    val logger = KotlinLogging.logger {}

    val tmpWorkingDirForBblPack = "/tmp/bblpack-cli-create"

    private fun deleteRecursively(fileSystem: FileSystem, path: Path) {
        if (!fileSystem.exists(path)) return

        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) {
            fileSystem.list(path).forEach { child ->
                deleteRecursively(fileSystem, child)
            }
        }
        fileSystem.delete(path)
    }

    @BeforeTest
    fun setup() {
        val fileSystem = getPlatform().fileSystem

        val tmpDir = tmpWorkingDirForBblPack.toPath()
        if (fileSystem.exists(tmpDir)) {
            deleteRecursively(fileSystem, tmpDir)
        }
        fileSystem.createDirectories(tmpDir)

        val sourceDir = "../composeApp/src/commonMain/composeResources/files/bblpacks/webus".toPath()
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
    fun testBblPackWebusInTmpDir() {
        val fileSystem = getPlatform().fileSystem

        val inputDir = (tmpWorkingDirForBblPack.toPath() / "webus").toString()
        val outputDir = tmpWorkingDirForBblPack
        val zipPath = outputDir.toPath() / "webus.zip"

        try {
            PackCli(Bible()).createBblPack(inputPathString = inputDir, outputPathString = outputDir)
            assertTrue(fileSystem.exists(zipPath), "Expected zip file to be created at $zipPath")

            val platform = getPlatform().apply { overridePlatformPackDir = outputDir }
            val zipBibleTextReader = ZipBibleTextReader(platform)

            val expected = fileSystem.read(tmpWorkingDirForBblPack.toPath() / "webus" / "webus.1.1.txt") { readUtf8() }
            val actual = zipBibleTextReader.getChapterText("webus", 1, 1)
            assertEquals(expected, actual)

            val manifestTranslation = zipBibleTextReader.getTranslationFromManifest("webus")
            assertEquals("webus", manifestTranslation.code)
        } finally {
            if (fileSystem.exists(zipPath)) {
                fileSystem.delete(zipPath)
            }
        }
    }
}
