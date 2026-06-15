package org.gnit.bible.cli

actual fun platformCommandLineArgs(args: Array<String>): Array<String> = normalizeCommandLineArgs(args)
