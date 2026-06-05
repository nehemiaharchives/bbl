package org.gnit.bible

import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.gnit.bible.app.Res
import org.jetbrains.compose.resources.InternalResourceApi

@OptIn(InternalResourceApi::class)
internal fun seedComposePackDirFromResources(platform: Platform) {
    if (!platform.isIos()) {
        return
    }

    val packDirPath = platform.packDir
    if (!packDirPath.contains("compose_bible_test_dir")) {
        return
    }

    val fileSystem = platform.fileSystem
    val packDir = packDirPath.toPath()
    fileSystem.createDirectories(packDir)

    SupportedTranslation.entries
        .filterNot { it.embedded }
        .map { it.translation.code }
        .forEach { code ->
            val destination = packDir / "$code.zip"
            val bytes = runBlocking {
                Res.readBytes("files/bblpackzips/$code.zip")
            }
            val existingSize = runCatching { fileSystem.metadata(destination).size }.getOrNull()
            if (existingSize == bytes.size.toLong()) {
                return@forEach
            }
            fileSystem.write(destination) {
                write(bytes)
            }
        }
}
