package org.gnit.bible.cli

import kotlinx.serialization.json.Json
import okio.Path
import okio.Path.Companion.toPath
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.HISTORY_FILE_NAME
import org.gnit.bible.HistoryRecord

object BblHistory {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun historyPath(bible: Bible): Path = bible.assetManager.platform.packDir.toPath() / HISTORY_FILE_NAME

    fun read(bible: Bible): List<HistoryRecord> {
        val fileSystem = bible.assetManager.fileSystem
        val path = historyPath(bible)
        if (!fileSystem.exists(path)) return emptyList()
        return runCatching {
            val payload = fileSystem.read(path) { readUtf8() }
            if (payload.isBlank()) emptyList() else json.decodeFromString<List<HistoryRecord>>(payload)
        }.getOrDefault(emptyList())
    }

    fun record(bible: Bible, command: String, force: Boolean = false) {
        if (!force && !bible.historyEnabledFromSettings()) return

        val fileSystem = bible.assetManager.fileSystem
        val path = historyPath(bible)
        val records = read(bible) + HistoryCli.historyRecordOf(command)
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) {
            writeUtf8(json.encodeToString(records))
            writeUtf8("\n")
        }
    }

    fun render(records: List<HistoryRecord>, format: org.gnit.bible.HistoryFormat): List<String> {
        val width = records.size.toString().length.coerceAtLeast(5)
        return records.mapIndexed { index, record ->
            "${(index + 1).toString().padStart(width)}  ${record.format(format)}"
        }
    }

    fun normalizeReadCommand(bookTokens: List<String>, chapterVerse: String): String {
        val bookString = bookTokens.joinToString(separator = " ") { it.lowercase() }
        val normalizedBook = runCatching {
            Books.bookNameEnglish(Books.bookNumber(bookString)) ?: bookString
        }.getOrDefault(bookString)
        return command("bbl", normalizedBook, chapterVerse)
    }

    fun command(vararg parts: String?): String {
        return parts.filterNot { it.isNullOrBlank() }.joinToString(" ")
    }
}
