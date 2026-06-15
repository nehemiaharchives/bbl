package org.gnit.bible.cli

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.windows.CommandLineToArgvW
import platform.windows.GetCommandLineW
import platform.windows.LocalFree

@OptIn(ExperimentalForeignApi::class)
actual fun platformCommandLineArgs(args: Array<String>): Array<String> {
    memScoped {
        val argc = alloc<IntVar>()
        val commandLine = GetCommandLineW()?.wideString() ?: return args
        val argv = CommandLineToArgvW(commandLine, argc.ptr) ?: return args
        try {
            val parsed = ArrayList<String>(argc.value)
            for (index in 0 until argc.value) {
                parsed.add(argv[index]?.wideString() ?: "")
            }
            return normalizeCommandLineArgs(parsed.drop(1).toTypedArray())
        } finally {
            LocalFree(argv)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<UShortVar>.wideString(): String {
    val builder = StringBuilder()
    var index = 0
    while (true) {
        val codeUnit = this[index].toInt()
        if (codeUnit == 0) break
        builder.append(codeUnit.toChar())
        index++
    }
    return builder.toString()
}
