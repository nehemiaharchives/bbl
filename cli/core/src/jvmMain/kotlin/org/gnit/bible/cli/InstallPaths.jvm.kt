package org.gnit.bible.cli

import okio.Path
import java.io.File

actual fun markExecutable(path: Path) {
    File(path.toString()).setExecutable(true, false)
}

actual fun environmentVariable(name: String): String? {
    return System.getenv(name)?.takeIf { it.isNotBlank() }
}
