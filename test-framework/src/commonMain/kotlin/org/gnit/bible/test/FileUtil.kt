package org.gnit.bible.test

import okio.FileSystem
import okio.Path

object FileUtil {
    fun deleteRecursively(fileSystem: FileSystem, path: Path) {
        if (!fileSystem.exists(path)) return

        val metadata = fileSystem.metadata(path)
        if (metadata.isDirectory) {
            fileSystem.list(path).forEach { child ->
                deleteRecursively(fileSystem, child)
            }
        }
        fileSystem.delete(path)
    }
}