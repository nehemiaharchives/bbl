package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

/**
 * `bbl help` prints out Main.kt help for class Bbl, works same as bbl -h
 * `bbl help search` prints out help message for SearchCli.kt
 * `bbl help rand` prints out help message for RandClit.kt
 * `bbl help list` prints out help message for ListCli.kt
 * `bbl hlep install` prints out help message for InstallCli.kt
 * `bbl help uninstall` prints out help message for UninstallCli.kt
 * `bbl help config` prints out help message for ConfigCli.kt
 * `bbl help history` prints out help message for HistoryCli.kt
 */
class HelpCli : CoreCliktCommand(name = "help") {

    override fun help(context: Context): String = "Show help for a command"

    private val commandArg: String? by argument(
        "command",
        help = "The command to show help for, e.g. search, rand, list, install, uninstall, config, history",
        completionCandidates = CompletionCandidates.Fixed(Bbl.subCommands)
    ).optional()

    override fun run() {
        val name = commandArg
        val bbl = currentContext.parent?.command as? CoreCliktCommand
            ?: error("HelpCli must be invoked as a subcommand")

        if (name == null) {
            echo(bbl.getFormattedHelp() ?: "")
        } else {
            val subcommand = bbl.registeredSubcommands()
                .find { it.commandName == name }
            if (subcommand != null) {
                echo(subcommand.getFormattedHelp() ?: "")
            } else {
                throw UsageError("Unknown command: $name. Try 'bbl help' for a list of available commands.")
            }
        }
    }
}
