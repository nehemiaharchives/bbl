package org.gnit.bible.cli

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

interface ProcessRunner {
    fun run(command: List<String>): ProcessResult
}

expect class PlatformProcessRunner() : ProcessRunner
