package org.gnit.bible.cli

actual class PlatformProcessRunner actual constructor() : ProcessRunner {
    actual override fun run(command: List<String>): ProcessResult {
        require(command.isNotEmpty()) { "command must not be empty" }

        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return ProcessResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }
}
