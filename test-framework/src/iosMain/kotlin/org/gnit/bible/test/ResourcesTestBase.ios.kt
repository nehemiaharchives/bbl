@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.gnit.bible.test

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.gnit.bible.Platform
import org.gnit.bible.downloadableTranslationsCmp
import org.gnit.bible.embeddedTranslationCodes
import org.gnit.bible.getPlatform
import platform.posix.PATH_MAX
import platform.posix.getenv
import platform.posix.getcwd

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
        val fileSystem = FileSystem.SYSTEM
        val packDir = packDirPath.toPath()
        if (!fileSystem.exists(packDir)) {
            fileSystem.createDirectories(packDir)
        }
        val codes = downloadableTranslationsCmp.map { it.code }
            .filterNot { embeddedTranslationCodes.contains(it) }
        codes.forEach { code ->
            val src = serverPackDir / "$code.zip"
            if (fileSystem.exists(src) && fileSystem.metadata(src).isRegularFile) {
                copyZip(fileSystem, src, packDir / "$code.zip")
            }
        }
    }

    private fun copyZip(fileSystem: FileSystem, source: Path, destination: Path) {
        destination.parent?.let { fileSystem.createDirectories(it) }
        val input = fileSystem.source(source)
        val output = fileSystem.sink(destination).buffer()
        try {
            output.writeAll(input)
        } finally {
            runCatching { output.close() }
            runCatching { input.close() }
        }
    }

    private fun findServerPackDir(): Path? {
        envPath("BBL_KMP_ROOT")?.let { candidate ->
            val packDir = candidate / "server" / "src" / "main" / "resources" / "files" / "bblpacks"
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("PWD")?.let { candidate ->
            val packDir = candidate / "server" / "src" / "main" / "resources" / "files" / "bblpacks"
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("PROJECT_DIR")?.let { candidate ->
            val packDir = candidate / "server" / "src" / "main" / "resources" / "files" / "bblpacks"
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("SRCROOT")?.let { candidate ->
            val packDir = candidate / "server" / "src" / "main" / "resources" / "files" / "bblpacks"
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        var current = currentWorkingDir() ?: return null
        val fileSystem = FileSystem.SYSTEM
        while (true) {
            val candidate = current / "server" / "src" / "main" / "resources" / "files" / "bblpacks"
            if (fileSystem.exists(candidate) && fileSystem.metadata(candidate).isDirectory) {
                return candidate
            }
            val parent = current.parent ?: return null
            if (parent == current) {
                return null
            }
            current = parent
        }
    }

    private fun currentWorkingDir(): Path? = memScoped {
        val buffer = allocArray<ByteVar>(PATH_MAX)
        val result = getcwd(buffer, PATH_MAX.toULong())
        result?.toKString()?.toPath()
    }

    private fun envPath(name: String): Path? {
        return getenv(name)?.toKString()?.takeIf { it.isNotBlank() }?.toPath()
    }
}
