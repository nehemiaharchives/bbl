package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformProcessRunnerMingwTest {
    @Test
    fun capturesStdoutAndExitCode() {
        val result = PlatformProcessRunner().run(
            listOf("cmd.exe", "/c", "echo hello")
        )

        assertEquals(0, result.exitCode)
        assertEquals("hello", result.stdout.trim())
        assertEquals("", result.stderr)
    }

    @Test
    fun capturesStderrSeparately() {
        val result = PlatformProcessRunner().run(
            listOf("cmd.exe", "/c", "echo problem 1>&2")
        )

        assertEquals(0, result.exitCode)
        assertEquals("", result.stdout)
        assertEquals("problem", result.stderr.trim())
    }

    @Test
    fun capturesNonZeroExitCode() {
        val result = PlatformProcessRunner().run(
            listOf("cmd.exe", "/c", "exit /b 7")
        )

        assertEquals(7, result.exitCode)
        assertEquals("", result.stdout)
        assertEquals("", result.stderr)
    }

    @Test
    fun preservesQuotedArgumentsWithSpaces() {
        val result = PlatformProcessRunner().run(
            listOf("cmd.exe", "/c", "echo alpha beta")
        )

        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("alpha beta"))
    }

    @Test
    fun preservesUnicodeArguments() {
        val result = PlatformProcessRunner().run(
            listOf(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "if ('Ісуса Христа' -eq 'Ісуса Христа') { 'ok' } else { exit 9 }"
            )
        )

        assertEquals(0, result.exitCode, "stdout=${result.stdout}; stderr=${result.stderr}")
        assertEquals("ok", result.stdout.trim())
    }
}
