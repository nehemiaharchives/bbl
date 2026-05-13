package org.gnit.bible.cli

import okio.Path

expect fun markExecutable(path: Path)

expect fun environmentVariable(name: String): String?
