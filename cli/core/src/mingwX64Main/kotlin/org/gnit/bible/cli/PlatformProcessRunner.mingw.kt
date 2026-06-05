package org.gnit.bible.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.NativePlacement
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import platform.windows.CloseHandle
import platform.windows.CREATE_ALWAYS
import platform.windows.CreateFileA
import platform.windows.CreateProcessW
import platform.windows.FILE_ATTRIBUTE_NORMAL
import platform.windows.FILE_SHARE_DELETE
import platform.windows.FILE_SHARE_READ
import platform.windows.FILE_SHARE_WRITE
import platform.windows.GENERIC_WRITE
import platform.windows.GetExitCodeProcess
import platform.windows.GetLastError
import platform.windows.GetStdHandle
import platform.windows.HANDLE
import platform.windows.INFINITE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.PROCESS_INFORMATION
import platform.windows.SECURITY_ATTRIBUTES
import platform.windows.STARTF_USESTDHANDLES
import platform.windows.STARTUPINFOW
import platform.windows.STD_INPUT_HANDLE
import platform.windows.WaitForSingleObject
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
actual class PlatformProcessRunner actual constructor() : ProcessRunner {
    actual override fun run(command: List<String>): ProcessResult {
        require(command.isNotEmpty()) { "command must not be empty" }

        val stdoutPath = temporaryOutputPath("stdout")
        val stderrPath = temporaryOutputPath("stderr")

        var stdoutHandle: HANDLE? = null
        var stderrHandle: HANDLE? = null
        var processHandle: HANDLE? = null
        var threadHandle: HANDLE? = null

        try {
            memScoped {
                val securityAttributes = alloc<SECURITY_ATTRIBUTES>()
                securityAttributes.nLength = sizeOf<SECURITY_ATTRIBUTES>().toUInt()
                securityAttributes.lpSecurityDescriptor = null
                securityAttributes.bInheritHandle = 1

                stdoutHandle = createOutputHandle(stdoutPath, securityAttributes.ptr)
                stderrHandle = createOutputHandle(stderrPath, securityAttributes.ptr)

                val startupInfo = alloc<STARTUPINFOW>()
                startupInfo.cb = sizeOf<STARTUPINFOW>().toUInt()
                startupInfo.dwFlags = STARTF_USESTDHANDLES.toUInt()
                startupInfo.hStdInput = GetStdHandle(STD_INPUT_HANDLE)
                startupInfo.hStdOutput = stdoutHandle
                startupInfo.hStdError = stderrHandle

                val processInformation = alloc<PROCESS_INFORMATION>()
                val commandLine = command.joinToString(" ") { it.quoteForCreateProcess() }
                val commandLineBuffer = wideMutableBuffer(commandLine)
                val created = CreateProcessW(
                    lpApplicationName = null,
                    lpCommandLine = commandLineBuffer,
                    lpProcessAttributes = null,
                    lpThreadAttributes = null,
                    bInheritHandles = 1,
                    dwCreationFlags = 0u,
                    lpEnvironment = null,
                    lpCurrentDirectory = null,
                    lpStartupInfo = startupInfo.ptr,
                    lpProcessInformation = processInformation.ptr
                )
                if (created == 0) {
                    val error = GetLastError().toInt()
                    return ProcessResult(
                        exitCode = 127,
                        stdout = "",
                        stderr = "CreateProcess failed with Windows error $error: $commandLine"
                    )
                }

                processHandle = processInformation.hProcess
                threadHandle = processInformation.hThread
                WaitForSingleObject(processHandle, INFINITE)

                val exitCode = alloc<UIntVar>()
                if (GetExitCodeProcess(processHandle, exitCode.ptr) == 0) {
                    val error = GetLastError().toInt()
                    return ProcessResult(
                        exitCode = 127,
                        stdout = readAndDelete(stdoutPath),
                        stderr = "GetExitCodeProcess failed with Windows error $error"
                    )
                }

                closeHandle(stdoutHandle)
                stdoutHandle = null
                closeHandle(stderrHandle)
                stderrHandle = null

                return ProcessResult(
                    exitCode = exitCode.value.toInt(),
                    stdout = readAndDelete(stdoutPath),
                    stderr = readAndDelete(stderrPath)
                )
            }
        } finally {
            closeHandle(stdoutHandle)
            closeHandle(stderrHandle)
            closeHandle(threadHandle)
            closeHandle(processHandle)
            deleteIfExists(stdoutPath)
            deleteIfExists(stderrPath)
        }
    }

    private fun createOutputHandle(
        path: Path,
        securityAttributes: CPointer<SECURITY_ATTRIBUTES>
    ): HANDLE? {
        val handle = CreateFileA(
            lpFileName = path.toString(),
            dwDesiredAccess = GENERIC_WRITE.toUInt(),
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE or FILE_SHARE_DELETE).toUInt(),
            lpSecurityAttributes = securityAttributes,
            dwCreationDisposition = CREATE_ALWAYS.toUInt(),
            dwFlagsAndAttributes = FILE_ATTRIBUTE_NORMAL.toUInt(),
            hTemplateFile = null
        )
        if (handle == INVALID_HANDLE_VALUE) {
            error("CreateFile failed with Windows error ${GetLastError().toInt()}: $path")
        }
        return handle
    }

    private fun closeHandle(handle: HANDLE?) {
        if (handle != null && handle != INVALID_HANDLE_VALUE) {
            CloseHandle(handle)
        }
    }

    private fun readAndDelete(path: Path): String {
        val text = if (FileSystem.SYSTEM.exists(path)) {
            FileSystem.SYSTEM.read(path) { readUtf8() }
        } else {
            ""
        }
        deleteIfExists(path)
        return text
    }

    private fun deleteIfExists(path: Path) {
        runCatching {
            if (FileSystem.SYSTEM.exists(path)) {
                FileSystem.SYSTEM.delete(path)
            }
        }
    }

    private fun temporaryOutputPath(kind: String): Path {
        val base = getenv("TEMP")?.toKString()
            ?: getenv("TMP")?.toKString()
            ?: "."
        return base.toPath() / "bbl-process-${Random.nextLong().toString(16)}-$kind.tmp"
    }

    private fun NativePlacement.wideMutableBuffer(value: String): CPointer<UShortVar> {
        val buffer = allocArray<UShortVar>(value.length + 1)
        value.forEachIndexed { index, char -> buffer[index] = char.code.toUShort() }
        buffer[value.length] = 0u
        return buffer
    }

    private fun String.quoteForCreateProcess(): String {
        if (isNotEmpty() && none { it.isWhitespace() || it == '"' }) return this
        val result = StringBuilder(length + 2)
        result.append('"')
        var backslashes = 0
        for (c in this) {
            when (c) {
                '\\' -> backslashes++
                '"' -> {
                    repeat(backslashes * 2 + 1) { result.append('\\') }
                    result.append('"')
                    backslashes = 0
                }
                else -> {
                    repeat(backslashes) { result.append('\\') }
                    result.append(c)
                    backslashes = 0
                }
            }
        }
        repeat(backslashes * 2) { result.append('\\') }
        result.append('"')
        return result.toString()
    }
}
