package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.ConfigKey
import org.gnit.bible.CONFIG_FILE_NAME
import org.gnit.bible.RandomlyShow
import org.gnit.bible.SupportedTranslation

class ConfigCli(
    private val bible: Bible
) : CoreCliktCommand(name = "config") {

    override fun help(context: Context): String = "Manage bbl config"

    override val invokeWithoutSubcommand: Boolean = true

    private val key: String? by argument(help = "Config key (e.g. translation, searchResult, randomlyShow, header)").optional()
    private val value: String? by argument(help = "Config value, for translation: webus, for searchResult: 10, for randomlyShow: verse, for header: true").optional()

    init {
        subcommands(ConfigInitCli(bible))
    }

    override fun run(){
        if (currentContext.invokedSubcommand != null) return

        val platform = bible.assetManager.platform
        val settings = platform.configSettings

        val nonNullKey = key ?: throw UsageError("ConfigCli Missing config key. Example: bbl config translation")

        if (nonNullKey == "init") {
            if (value != null) throw UsageError("ConfigCli init doesn't accept a value. Run: bbl config init")
            val bblDir = generateDefaultConfig(bible)
            echo("default config file was generated at $bblDir")
            return
        }

        val configKey = ConfigKey.entries.firstOrNull { it.value == nonNullKey }
            ?: throw UsageError(
                "ConfigCli Unknown config key '$nonNullKey'. Available keys: ${ConfigKey.entries.joinToString(", ") { it.value }}"
            )

        if (value == null) {
            val existing = when (configKey) {
                ConfigKey.SEARCH_RESULT -> settings.getIntOrNull(configKey.value)?.toString()
                else -> settings.getStringOrNull(configKey.value)
            } ?: throw UsageError("ConfigCli Config '${configKey.value}' is not set. Run: bbl config ${configKey.value} <value>")
            echo(existing)
            return
        }

        val newValue = requireNotNull(value)

        if (configKey == ConfigKey.RANDOMLY_SHOW) {
            val valid = RandomlyShow.entries.any { it.name == newValue }
            if (!valid) {
                throw UsageError(
                    "ConfigCli Invalid value '$newValue' for '${configKey.value}'. " +
                        "Valid values: ${RandomlyShow.entries.joinToString(", ") { it.name }}"
                )
            }
        }

        if (configKey == ConfigKey.SEARCH_RESULT) {
            val valid = newValue.toIntOrNull()?.let { it > 0 } == true
            if (!valid) {
                throw UsageError("ConfigCli Invalid value '$newValue' for '${configKey.value}'. Value must be a positive integer")
            }
        }

        if (configKey == ConfigKey.HEADER) {
            val valid = newValue.toBooleanStrictOrNull() != null
            if (!valid) {
                throw UsageError("ConfigCli Invalid value '$newValue' for '${configKey.value}'. Valid values: true, false")
            }
        }

        if (configKey == ConfigKey.TRANSLATION) {
            val valid = bible.availableTranslationCodes().contains(newValue)
            if (!valid) {
                if (SupportedTranslation.downloadableCodes.contains(newValue)) {
                    echo("Translation '$newValue' is downloadable but not installed. Run: bbl install $newValue", err = true)
                    throw UsageError("Translation '$newValue' is not installed.")
                }
                throw UsageError(
                    "Invalid translation code '$newValue'. " +
                        "Available translation codes: ${bible.availableTranslationCodes().joinToString(", ")}"
                )
            }
        }

        if (configKey == ConfigKey.SEARCH_RESULT) {
            settings.putInt(configKey.value, newValue.toInt())
        } else {
            settings.putString(configKey.value, newValue)
        }
    }
}

private fun generateDefaultConfig(bible: Bible): Path {
    val platform = bible.assetManager.platform
    val settings = platform.configSettings

    // CLI no longer embeds default translations. If the default translation pack isn't installed,
    // fail fast and ask the user to install it.
    val defaultTranslation = "webus"
    if (!bible.availableTranslationCodes().contains(defaultTranslation)) {
        // keep message user-friendly; tests look for "bbl install"
        throw UsageError("Default translation '$defaultTranslation' is not installed. Run: bbl install $defaultTranslation")
    }

    if (settings.getStringOrNull(ConfigKey.TRANSLATION.value) == null) {
        settings.putString(ConfigKey.TRANSLATION.value, defaultTranslation)
    }
    if (settings.getStringOrNull(ConfigKey.RANDOMLY_SHOW.value) == null) {
        settings.putString(ConfigKey.RANDOMLY_SHOW.value, "verse")
    }
    if (settings.getIntOrNull(ConfigKey.SEARCH_RESULT.value) == null) {
        settings.putInt(ConfigKey.SEARCH_RESULT.value, ConfigKey.SEARCH_RESULT.defaultValue.toInt())
    }
    if (settings.getStringOrNull(ConfigKey.HEADER.value) == null) {
        settings.putString(ConfigKey.HEADER.value, ConfigKey.HEADER.defaultValue)
    }

    return platform.packDir.toPath().parent!! / CONFIG_FILE_NAME
}

private class ConfigInitCli(
    private val bible: Bible
) : CoreCliktCommand(name = "init") {

    override fun help(context: Context): String {
        val bblDir = bible.assetManager.platform.packDir.toPath().parent
        return "Generate default config file at $bblDir/$CONFIG_FILE_NAME"
    }

    override fun run() {
        val bblDir = generateDefaultConfig(bible)
        echo("default config file was generated at $bblDir")
    }
}
