package org.gnit.bible.test

import org.gnit.bible.Platform
import org.gnit.bible.getPlatform
import java.io.File
import java.io.InputStream

actual abstract class ResourcesTestBase actual constructor() {
    actual fun createTestPlatform(): Platform {
        return getPlatform()
    }

    actual fun seedComposePackDirIfNeeded(platform: Platform) {
        val packDirPath = platform.packDir
        if (!packDirPath.contains("compose_bible_test_dir")) {
            return
        }
        val canonicalPackDir = findCanonicalPackDir() ?: return
        val packDir = File(packDirPath)
        if (!packDir.exists()) {
            packDir.mkdirs()
        }
        canonicalPackDir.listFiles { file -> file.isFile && file.extension == "zip" }
            ?.forEach { src ->
                copyZip(src::inputStream, File(packDir, src.name))
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

    private fun findCanonicalPackDir(): File? {
        val userDir = System.getProperty("user.dir") ?: return null
        var current: File? = File(userDir)
        while (current != null) {
            val candidates = listOf(
                File(current, "resources/bblpacks"),
                File(current, "bbl/resources/bblpacks")
            )
            candidates.firstOrNull { it.isDirectory }?.let { return it }
            current = current.parentFile
        }
        return null
    }
}
