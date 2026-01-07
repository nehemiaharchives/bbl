package org.gnit.bible.cli

import kotlinx.cinterop.*
import kotlinx.cinterop.get
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix._exit
import platform.posix.close
import platform.posix.dup2
import platform.posix.execvp
import platform.posix.fork
import platform.posix.pipe
import platform.posix.read
import platform.posix.waitpid

actual class PlatformProcessRunner actual constructor() : ProcessRunner {
    override fun run(command: List<String>): ProcessResult {
        require(command.isNotEmpty()) { "command must not be empty" }

        val (stdoutRead, stdoutWrite) = createPipe()
        val (stderrRead, stderrWrite) = createPipe()

        val pid = fork()
        if (pid == 0) {
            dup2(stdoutWrite, STDOUT_FILENO)
            dup2(stderrWrite, STDERR_FILENO)

            close(stdoutRead)
            close(stderrRead)
            close(stdoutWrite)
            close(stderrWrite)

            memScoped {
                val argv = allocArray<CPointerVar<ByteVar>>(command.size + 1)
                command.forEachIndexed { index, arg ->
                    argv[index] = arg.cstr.getPointer(this)
                }
                argv[command.size] = null
                execvp(command[0], argv)
            }

            _exit(127)
        }

        close(stdoutWrite)
        close(stderrWrite)

        val stdout = readAll(stdoutRead)
        val stderr = readAll(stderrRead)

        close(stdoutRead)
        close(stderrRead)

        val exitCode = memScoped {
            val status = alloc<IntVar>()
            waitpid(pid, status.ptr, 0)
            (status.value shr 8) and 0xFF
        }

        return ProcessResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }

    private fun createPipe(): Pair<Int, Int> = memScoped {
        val fds = allocArray<IntVar>(2)
        if (pipe(fds) != 0) {
            error("pipe() failed")
        }
        fds[0] to fds[1]
    }

    private fun readAll(fd: Int): String {
        val buffer = ByteArray(4096)
        val sb = StringBuilder()

        while (true) {
            val bytesRead = buffer.usePinned { pinned ->
                read(fd, pinned.addressOf(0), buffer.size.toULong())
            }
            if (bytesRead <= 0) break
            sb.append(buffer.decodeToString(0, bytesRead.toInt()))
        }

        return sb.toString()
    }
}
