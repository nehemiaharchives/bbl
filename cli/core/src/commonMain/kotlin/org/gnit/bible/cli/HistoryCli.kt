package org.gnit.bible.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.gnit.bible.Bible
import org.gnit.bible.HistoryRecord
import kotlin.time.Clock

class HistoryCli(
    private val bible: Bible
) : CoreCliktCommand(name = "history") {

    private val filter: String? by argument(
        help = "Optional filter: read/r, search/s, config/c"
    ).optional()

    override fun help(context: Context): String = "Show bbl command history"

    override fun run() {
        val records = when (filter?.lowercase()) {
            null -> BblHistory.read(bible)
            "r", "read" -> BblHistory.read(bible).filter { isReadCommand(it.command) }
            "s", "search", "saerch" -> BblHistory.read(bible).filter { it.command.startsWith("bbl search") }
            "c", "config" -> BblHistory.read(bible).filter { it.command.startsWith("bbl config") }
            else -> throw UsageError("Unknown history filter '$filter'. Use one of: read, search, config")
        }

        BblHistory.render(records, bible.historyFormatFromSettings()).forEach { echo(it) }
        BblHistory.record(bible, BblHistory.command("bbl history", filter))
    }

    private fun isReadCommand(command: String): Boolean {
        val knownNonReadCommands = setOf(
            "search",
            "config",
            "history",
            "list",
            "ls",
            "install",
            "get",
            "pull",
            "uninstall",
            "rm",
            "remove",
            "del",
            "delete",
            "rand"
        )
        val commandName = command.removePrefix("bbl ").substringBefore(" ")
        return command.startsWith("bbl ") && commandName !in knownNonReadCommands
    }

    companion object {
        val fmt = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char('T')
            hour()
            char(':')
            minute()
            char(':')
            second()
        }

        fun historyRecordOf(command: String): HistoryRecord {
            val timeZone = TimeZone.currentSystemDefault()
            return HistoryRecord(
                date = Clock.System.now().toLocalDateTime(timeZone).format(fmt),
                timezone = timeZone.toString(),
                command = command
            )
        }
    }
}
