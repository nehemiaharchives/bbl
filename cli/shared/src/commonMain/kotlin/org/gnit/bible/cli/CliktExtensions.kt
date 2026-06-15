package org.gnit.bible.cli

import com.github.ajalt.clikt.core.*

data class CliktTestResult(
    val stdout: String,
    val stderr: String,
    val output: String,
    val statusCode: Int
)

fun CoreCliktCommand.test(argv: String): CliktTestResult {
    return test(argv.split(" ").filter { it.isNotEmpty() })
}

fun CoreCliktCommand.test(): CliktTestResult {
    return test(emptyList())
}

fun CoreCliktCommand.test(argv: List<String>): CliktTestResult {
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    var statusCode = 0
    val normalizedArgv = normalizeCommandLineArgs(argv.toTypedArray()).toList()

    this.context {
        echoMessage = { _, message, trailingNewline, err ->
            val builder = if (err) stderr else stdout
            builder.append(message)
            if (trailingNewline) builder.append("\n")
        }
        exitProcess = { statusCode = it }
    }

    try {
        this.parse(normalizedArgv)
    } catch (e: PrintHelpMessage) {
        stdout.append("Help message printed\n") 
        statusCode = e.statusCode
    } catch (e: PrintCompletionMessage) {
        stdout.append(e.message).append("\n")
        statusCode = e.statusCode
    } catch (e: PrintMessage) {
        val builder = if (e.printError) stderr else stdout
        builder.append(e.message)
        builder.append("\n")
        statusCode = e.statusCode
    } catch (e: CliktError) {
        stderr.append(e.message ?: "").append("\n")
        statusCode = e.statusCode
    } catch (e: Exception) {
        stderr.append(e.message ?: e.toString()).append("\n")
        statusCode = 1
    }

    val out = stdout.toString()
    val err = stderr.toString()
    return CliktTestResult(out, err, out + err, statusCode)
}
