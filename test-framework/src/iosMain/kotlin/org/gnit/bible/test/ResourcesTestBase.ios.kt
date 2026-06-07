@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.gnit.bible.test

import org.gnit.bible.SupportedTranslation

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.gnit.bible.Platform
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
        if (!packDirPath.contains("compose_bible_test_dir")) {
            return
        }
        val canonicalPackDir = findCanonicalPackDir() ?: return
        val fileSystem = FileSystem.SYSTEM
        val packDir = packDirPath.toPath()
        if (!fileSystem.exists(packDir)) {
            fileSystem.createDirectories(packDir)
        }
        val codes = SupportedTranslation.all.map { it.code }
            .filterNot { SupportedTranslation.embeddedCodes.contains(it) }
        codes.forEach { code ->
            val src = canonicalPackDir / "$code.zip"
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

    private fun findCanonicalPackDir(): Path? {
        envPath("BBL_KMP_ROOT")?.let { candidate ->
            val packDir = canonicalPackDir(candidate)
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("PWD")?.let { candidate ->
            val packDir = canonicalPackDir(candidate)
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("PROJECT_DIR")?.let { candidate ->
            val packDir = canonicalPackDir(candidate)
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        envPath("SRCROOT")?.let { candidate ->
            val packDir = canonicalPackDir(candidate)
            if (FileSystem.SYSTEM.exists(packDir)) {
                return packDir
            }
        }
        var current = currentWorkingDir() ?: return null
        val fileSystem = FileSystem.SYSTEM
        while (true) {
            val candidates = listOf(
                canonicalPackDir(current),
                current / "bbl" / "resources" / "bblpacks"
            )
            candidates.firstOrNull { fileSystem.exists(it) && fileSystem.metadata(it).isDirectory }?.let {
                return it
            }
            val parent = current.parent ?: return null
            if (parent == current) {
                return null
            }
            current = parent
        }
    }

    private fun canonicalPackDir(root: Path): Path {
        return root / "resources" / "bblpacks"
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
