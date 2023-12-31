package org.gnit.bible.cli

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.*

class WindowsCommandLine {
    private var kernel32: Kernel32? = null
        get() {
            if (field == null) {
                field = Native.load("kernel32", Kernel32::class.java)
            }
            return field
        }
    private var shell32: Shell32? = null
        get() {
            if (field == null) {
                field = Native.load("shell32", Shell32::class.java)
            }
            return field
        }

    fun getCommandLineArguments(commandName: String, fallBackTo: List<String>): List<String> {
        return try {

            var ret = fullCommandLine
            var argsOnly: MutableList<String>? = null
            for (i in ret.indices) {

                if (ret[i].lowercase(Locale.getDefault()).contentEquals(commandName)) {
                    argsOnly = ArrayList()
                }

                argsOnly?.add(ret[i])
            }
            if (argsOnly != null) {
                ret = argsOnly
            }
            ret.drop(1)
        } catch (t: Throwable) {
            fallBackTo
        }
    }

    private val fullCommandLine: List<String>

        get() = try {
            val cp = kernel32!!.GetConsoleCP()
            logger.debug("console cp: $cp")

            val argc = IntByReference()
            val argv_ptr = shell32!!.CommandLineToArgvW(kernel32!!.GetCommandLineW(), argc)
            val argv = argv_ptr.getWideStringArray(0, argc.value)
            kernel32!!.LocalFree(argv_ptr)
            argv.toList()
        } catch (t: Throwable) {
            throw RuntimeException("Failed to get program arguments using JNA", t)
        }
}

internal interface Kernel32 : StdCallLibrary {
    fun GetConsoleCP(): Int
    fun GetCommandLineW(): WString?
    fun LocalFree(pointer: Pointer?): Pointer?
}

internal interface Shell32 : StdCallLibrary {
    fun CommandLineToArgvW(command_line: WString?, argc: IntByReference?): Pointer
}

fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

fun main(args: Array<String>){

    // feed following args in idea's program arguments in run/debug configurations: 聖書 in jc
    println("expected args: 聖書 in jc")

    if(isWindows()){
        val jnaArgs = WindowsCommandLine().getCommandLineArguments("search", args.toList())
        println(jnaArgs.joinToString())
    }else{
        val jvmArgs = args.joinToString()
        println("jvm args: $jvmArgs")
    }
}
