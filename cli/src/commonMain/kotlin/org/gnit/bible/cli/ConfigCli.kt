package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey

class ConfigCli(
    bible: Bible
) : CliktCommand(name = "config") {

    override fun help(context: Context): String = "Manage bbl config"

    init {
        subcommands(ConfigInitCli(bible))
    }

    override fun run() = Unit
}

private class ConfigInitCli(
    private val bible: Bible
) : CliktCommand(name = "init") {

    override fun help(context: Context): String = "Generate default config file"

    override fun run() {
        val platform = bible.assetManager.platform
        val settings = platform.settings

        if (settings.getStringOrNull(ConfigKey.TRANSLATION.value) == null) {
            settings.putString(ConfigKey.TRANSLATION.value, "webus")
        }
        if (settings.getStringOrNull(ConfigKey.RANDOMLY_SHOW.value) == null) {
            settings.putString(ConfigKey.RANDOMLY_SHOW.value, "verse")
        }

        val bblDir = platform.packDir.toPath().parent!!
        echo("default config file was generated at $bblDir")
    }
}
