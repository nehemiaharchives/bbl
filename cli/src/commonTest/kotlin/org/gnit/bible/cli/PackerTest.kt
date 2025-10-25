package org.gnit.bible.cli

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnit.bible.ZipBibleTextReader
import org.gnit.bible.downloadableTranslations
import org.gnit.bible.getPlatform
import kotlin.test.Test
import kotlin.test.assertContains

class PackerTest {

    val logger = KotlinLogging.logger {}

    @Test
    fun testGeneratedZipFilesByMain() {
        val platform = getPlatform()
        platform.overridePlatformPackDir = "../server/src/main/resources/files/bblpacks"
        val zipBibleTextReader = ZipBibleTextReader(platform)
        downloadableTranslations.forEach { translationCode ->
            val genesisChapterOne = zipBibleTextReader.getChapterText(translationCode, 1, 1)
            (1..16).forEach { verseNumber ->
                assertContains(genesisChapterOne, "$verseNumber ")
            }
            logger.info { "translationCode: $translationCode\ngenesisChapterOne:\n$genesisChapterOne" }
        }
    }
}
