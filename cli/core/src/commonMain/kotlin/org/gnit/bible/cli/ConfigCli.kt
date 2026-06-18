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
import org.gnit.bible.CompareBy
import org.gnit.bible.ConfigKey
import org.gnit.bible.CONFIG_FILE_NAME
import org.gnit.bible.HISTORY_FILE_NAME
import org.gnit.bible.HistoryFormat
import org.gnit.bible.RandomlyShow
import org.gnit.bible.SupportedTranslation

class ConfigCli(
    private val bible: Bible
) : CoreCliktCommand(name = "config") {

    override fun help(context: Context): String = """
        Create, view or change settings
        
        Available keys and values with example command:
        
        # generate default config file in [USER HOME]/.bbl/$CONFIG_FILE_NAME
        bbl config init  to generate a default config file
        
        # view config
        bbl config [key]
        
        # change config
        bbl config [key] [value]
        
        # view current translation
        bbl config translation
        
        # change translation ref: bbl list translation
        bbl config translation kjv
        bbl john 3:16
            16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.

        # shortuct
        bbl conf tr kjv
        bbl c t kjv
        
        # number of search result verses (default: 100)
        bbl config searchResult 3
        bbl search Jesus Christ
            Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.
            Matthew 1:16 Jacob became the father of Joseph, the husband of Mary, from whom was born Jesus, who is called Christ.
            Matthew 1:18 Now the birth of Jesus Christ was like this: After his mother, Mary, was engaged to Joseph, before they came together, she was found pregnant by the Holy Spirit.

        # shortuct
        bbl c sr 3
        
        # bbl rand to show a verse or a chapter (default: verse)
        bbl config randomlyShow chapter
        bbl rand 
            (outputs a random chapter)
        
        # shortcut
        bbl c rs chapter
        
        # bbl, bbl rand, bbl search to enable/disable header, e.g. "Genesis 1". "John 3:16" (default: false)
        bbl config header true
        bbl matt 28:18-20
            Matthew 28:18-20
            18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
            19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
            20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
        
        # shortcut
        bbl c hd true
            
        # multiple translation comparison layout, either block or verse (default: block)        
        bbl config compareBy verse
        bbl matt 28:18-20 in webus jc
            18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
            18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。
            19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
            19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、
            20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
            20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。

        bbl config compareBy block  
        bbl matt 28:18-20 in webus jc
            18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
            19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
            20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
            18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。
            19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、
            20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。
        
        # shortuct
        bbl c cb verse
        
        # enables history stored in [USER HOME]/.bbl/$HISTORY_FILE_NAME (default: true)
        bbl config historyEnabled true
        bbl history
            1  bbl genesis 1
            2  bbl john 3:16 
            3  bbl matthew 28:18-20 in kjv
            4  bbl search Jesus
        # shortuct
        bbl c he false
        
        # history format, either command, datetimeCommand, datetimeTimezoneCommand (default: command)
        bbl config historyFormat datetimeCommand
        bbl history
            1  2026-06-18 02:20:45 bbl genesis 1
            2  2026-06-18 02:20:46 bbl john 3:16 
            3  2026-06-18 02:20:46 bbl matthew 28:18-20 in kjv
            4  2026-06-18 03:25:22 bbl search Jesus
        
        # shortuct
        bbl c hf command
    """.trimIndent()

    override val invokeWithoutSubcommand: Boolean = true

    private val key: String? by argument(help = "Config key (e.g. translation, searchResult, randomlyShow, header, compareBy, historyEnabled, historyFormat)").optional()
    private val value: String? by argument(help = "Config value, for translation: webus, for searchResult: 10, for randomlyShow: verse, for header/historyEnabled: true, for compareBy: block, for historyFormat: command").optional()

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
