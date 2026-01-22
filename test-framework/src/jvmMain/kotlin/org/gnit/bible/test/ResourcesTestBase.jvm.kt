package org.gnit.bible.test

import org.gnit.bible.Platform
import org.gnit.bible.downloadableTranslationsCmp
import org.gnit.bible.embeddedTranslationCodes
import org.gnit.bible.getPlatform
import java.io.File
import java.io.InputStream

actual abstract class ResourcesTestBase actual constructor() {
    actual fun createTestPlatform(): Platform {
        return getPlatform()
    }

    actual fun seedComposePackDirIfNeeded(platform: Platform) {
        val packDirPath = platform.packDir
        if (!packDirPath.contains("bbl_kmp_composeapp_compose_bible_test_dir")) {
            return
        }
        val serverPackDir = findServerPackDir() ?: return
        val packDir = File(packDirPath)
        if (!packDir.exists()) {
            packDir.mkdirs()
        }
        val codes = downloadableTranslationsCmp.map { it.code }
            .filterNot { embeddedTranslationCodes.contains(it) }
        codes.forEach { code ->
            val src = File(serverPackDir, "$code.zip")
            if (src.isFile) {
                copyZip(src::inputStream, File(packDir, "$code.zip"))
            }
        }
    }

    private fun copyZip(openStream: () -> InputStream, destination: File) {
        destination.parentFile?.mkdirs()
        openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun findServerPackDir(): File? {
        val userDir = System.getProperty("user.dir") ?: return null
        var current: File? = File(userDir)
        while (current != null) {
            val candidate = File(current, "server/src/main/resources/files/bblpacks")
            if (candidate.isDirectory) {
                return candidate
            }
            current = current.parentFile
        }
        return null
    }
}
