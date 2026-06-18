package org.gnit.bible

import kotlinx.serialization.Serializable

@Serializable
data class HistoryRecord(val date: String, val timezone: String, val command: String) {
    fun format(format: HistoryFormat): String {
        return when (format) {
            HistoryFormat.command -> command
            HistoryFormat.datetimeCommand -> "$date $command"
            HistoryFormat.datetimeTimezoneCommand -> "$date $timezone $command"
        }
    }
}

enum class HistoryFormat {
    /**
     * show only command, e.g. bbl john 3:16
     */
    command,

    /**
     * show datetime and command e.g. 2026-06-17 17:44:46 bbl john 3:16
     */
    datetimeCommand,

    /**
     * show datetime, timezone, command  e.g. 2026-06-17 17:44:46 Asia/Tokyo bbl john 3:16
     */
    datetimeTimezoneCommand
}

enum class RandomlyShow { verse, chapter }

enum class CompareBy { block, verse }

enum class ConfigKey(val value: String, val aliases: List<String>, val defaultValue: String, val description: String){
    //value          //aliases           //defaultValue                      //description
    TRANSLATION(        "translation",    listOf("t", "tr"),  SupportedTranslation.WEBUS.code,    "default translation of bible, use code e.g. webus, jc"),
    SEARCH_RESULT(      "searchResult",   listOf("sr"),       100.toString(),                     "default number of search result verses"),
    RANDOMLY_SHOW(      "randomlyShow",   listOf("rs"),       RandomlyShow.verse.toString(),      "[bbl rand] option to show a verse or a chapter"),
    HEADER(             "header",         listOf("hd"),       false.toString(),                   "bbl, bbl rand, bbl search option to show header, such as Genesis 1 or John 3:16 above the verses or not"),
    COMPARE_BY(         "compareBy",      listOf("cb"),       CompareBy.block.toString(),         "when showing multiple translations, use block to print the full selected range for each translation, or verse to compare verse by verse"),
    HISTAORY_ENABLED(   "historyEnabled", listOf("he"),       true.toString(),                    "enables bbl history prints out past bbl command histories"),
    HISTAORY_FROMAT(    "historyFormat",  listOf("hf"),       HistoryFormat.command.toString(),   "history format, either command, datetimeCommand, datetimeTimezoneCommand"),
}
