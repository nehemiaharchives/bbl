package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.CompareBy
import org.gnit.bible.ConfigKey
import org.gnit.bible.CONFIG_FILE_NAME
import org.gnit.bible.HistoryFormat
import org.gnit.bible.RandomlyShow
import org.gnit.bible.SupportedTranslation

class ConfigCli(
    private val bible: Bible
) : CoreCliktCommand(name = "config") {

    override fun help(context: Context): String = """
        View or change settings (shortcuts: bbl conf, bbl c)

        bbl config init                 Create the default config file
        bbl config <key>                Show the current value
        bbl config <key> <value>        Set a value

        Keys:
        translation (t, tr) <code>
            Default Bible translation. Run `bbl list translations` for codes. Default: webus
        searchResult (sr) <positive integer>
            Maximum verses returned by search. Default: 100
        randomlyShow (rs) verse|chapter
            What `bbl rand` displays. Default: verse
        header (hd) true|false
            Show a reference heading above verses. Default: false
        compareBy (cb) block|verse
            Multiple translations grouped by translation or by verse. Default: block
        historyEnabled (he) true|false
            Save commands for `bbl history`. Default: true
        historyFormat (hf) command|datetimeCommand|datetimeTimezoneCommand
            Show commands alone, with date/time, or with date/time and timezone. Default: command
    """.trimIndent()

    override val invokeWithoutSubcommand: Boolean = true

    private val key: String? by argument(
        help = "Config key (e.g. translation, searchResult, randomlyShow, header, compareBy, historyEnabled, historyFormat)",
        completionCandidates = CompletionCandidates.Fixed(configKeyCompletions)
    ).optional()
    private val value: String? by argument(
        help = "Config value, for translation: webus, for searchResult: 10, for randomlyShow: verse, for header/historyEnabled: true, for compareBy: block, for historyFormat: command",
        completionCandidates = configValueCompletionCandidates
    ).optional()

    init {
        subcommands(ConfigInitCli(bible))
    }

    override fun run(){
        if (currentContext.invokedSubcommand != null) return

        val platform = bible.assetManager.platform
        val settings = platform.configSettings
        val historyWasEnabled = bible.historyEnabledFromSettings()

        val nonNullKey = key ?: throw UsageError("ConfigCli Missing config key. Example: bbl config translation")

        if (nonNullKey == "init") {
            if (value != null) throw UsageError("ConfigCli init doesn't accept a value. Run: bbl config init")
            val bblDir = generateDefaultConfig(bible)
            echo("default config file was generated at $bblDir")
            BblHistory.record(bible, "bbl config init", force = historyWasEnabled)
            return
        }

        val configKey = ConfigKey.entries.firstOrNull { it.value == nonNullKey || it.aliases.contains(nonNullKey) }
            ?: throw UsageError(
                "ConfigCli Unknown config key '$nonNullKey'. Available keys: ${ConfigKey.entries.joinToString(", ") { it.value }}"
            )

        if (value == null) {
            val existing = when (configKey) {
                ConfigKey.SEARCH_RESULT -> settings.getIntOrNull(configKey.value)?.toString()
                else -> settings.getStringOrNull(configKey.value)
            } ?: throw UsageError("ConfigCli Config '${configKey.value}' is not set. Run: bbl config ${configKey.value} <value>")
            echo(existing)
            BblHistory.record(bible, BblHistory.command("bbl config", configKey.value), force = historyWasEnabled)
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

        if (configKey == ConfigKey.COMPARE_BY) {
            val valid = CompareBy.entries.any { it.name == newValue }
            if (!valid) {
                throw UsageError(
                    "ConfigCli Invalid value '$newValue' for '${configKey.value}'. " +
                        "Valid values: ${CompareBy.entries.joinToString(", ") { it.name }}"
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

        if (configKey == ConfigKey.HISTAORY_ENABLED) {
            val valid = newValue.toBooleanStrictOrNull() != null
            if (!valid) {
                throw UsageError("ConfigCli Invalid value '$newValue' for '${configKey.value}'. Valid values: true, false")
            }
        }

        if (configKey == ConfigKey.HISTAORY_FROMAT) {
            val valid = HistoryFormat.entries.any { it.name == newValue }
            if (!valid) {
                throw UsageError(
                    "ConfigCli Invalid value '$newValue' for '${configKey.value}'. " +
                        "Valid values: ${HistoryFormat.entries.joinToString(", ") { it.name }}"
                )
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
        echo("${configKey.value} set to $newValue")
        BblHistory.record(bible, BblHistory.command("bbl config", configKey.value, newValue), force = historyWasEnabled)
    }

    companion object {
        private val configKeyCompletions = ConfigKey.entries
            .flatMap { key -> listOf(key.value) }
            .toSet()

        private val configValueCompletionCandidates = CompletionCandidates.Custom { shell ->
            when (shell) {
                CompletionCandidates.Custom.ShellType.BASH -> bashConfigValueCompletion()
                CompletionCandidates.Custom.ShellType.FISH -> fishConfigValueCompletion()
                CompletionCandidates.Custom.ShellType.POWERSHELL -> powershellConfigValueCompletion()
            }
        }

        private fun valuesForConfigKey(rawKey: String): Set<String> {
            return when (ConfigKey.entries.firstOrNull { key -> key.value == rawKey || rawKey in key.aliases }) {
                ConfigKey.TRANSLATION -> SupportedTranslation.all.map { it.code }.toSet()
                ConfigKey.RANDOMLY_SHOW -> RandomlyShow.entries.map { it.name }.toSet()
                ConfigKey.HEADER -> setOf("true", "false")
                ConfigKey.COMPARE_BY -> CompareBy.entries.map { it.name }.toSet()
                ConfigKey.HISTAORY_ENABLED -> setOf("true", "false")
                ConfigKey.HISTAORY_FROMAT -> HistoryFormat.entries.map { it.name }.toSet()
                ConfigKey.SEARCH_RESULT, null -> emptySet()
            }
        }

        private fun bashConfigValueCompletion(): String {
            val cases = configKeyValueCases { key, values ->
                """        $key) COMPREPLY=(${ '$' }(compgen -W '$values' -- "${ '$' }word")) ;;"""
            }
            return """
                local word="${ '$' }{COMP_WORDS[${ '$' }COMP_CWORD]}"
                local key="${ '$' }{COMP_WORDS[${ '$' }((COMP_CWORD - 1))]}"
                case "${ '$' }key" in
                $cases
                esac
            """.trimIndent()
        }

        private fun fishConfigValueCompletion(): String {
            val cases = configKeyValueCases { key, values ->
                "case $key; echo $values"
            }
            return """
                "(switch (commandline -opc)[-1]
                $cases
                end)"
            """.trimIndent()
        }

        private fun powershellConfigValueCompletion(): String {
            val cases = configKeyValueCases { key, values ->
                val items = values.split(" ").joinToString("; ") { "'$it'" }
                "'$key' { $items }"
            }
            return """
                ${'$'}(
                    ${'$'}keyOffset = if ([string]::IsNullOrEmpty(${'$'}wordToComplete)) { 1 } else { 2 }
                    ${'$'}key = ${'$'}commandAst.CommandElements[${'$'}commandAst.CommandElements.Count - ${'$'}keyOffset].Value
                    switch (${'$'}key) {
                $cases
                    }
                )
            """.trimIndent()
        }

        private fun configKeyValueCases(caseFactory: (String, String) -> String): String {
            return ConfigKey.entries
                .filter { key -> key != ConfigKey.SEARCH_RESULT }
                .flatMap { key ->
                    val values = valuesForConfigKey(key.value).joinToString(" ")
                    (listOf(key.value) + key.aliases).map { caseKey -> caseFactory(caseKey, values) }
                }
                .joinToString("\n")
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
    if (settings.getStringOrNull(ConfigKey.COMPARE_BY.value) == null) {
        settings.putString(ConfigKey.COMPARE_BY.value, ConfigKey.COMPARE_BY.defaultValue)
    }
    if (settings.getStringOrNull(ConfigKey.HISTAORY_ENABLED.value) == null) {
        settings.putString(ConfigKey.HISTAORY_ENABLED.value, ConfigKey.HISTAORY_ENABLED.defaultValue)
    }
    if (settings.getStringOrNull(ConfigKey.HISTAORY_FROMAT.value) == null) {
        settings.putString(ConfigKey.HISTAORY_FROMAT.value, ConfigKey.HISTAORY_FROMAT.defaultValue)
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
        val historyWasEnabled = bible.historyEnabledFromSettings()
        val bblDir = generateDefaultConfig(bible)
        echo("default config file was generated at $bblDir")
        BblHistory.record(bible, "bbl config init", force = historyWasEnabled)
    }
}
