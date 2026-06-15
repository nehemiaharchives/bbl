package org.gnit.bible.cli

expect fun platformCommandLineArgs(args: Array<String>): Array<String>

fun normalizeCommandLineArgs(args: Array<String>): Array<String> {
    return args.flatMap { arg ->
        arg.split('\u3000').filter { it.isNotEmpty() }
    }.toTypedArray()
}
