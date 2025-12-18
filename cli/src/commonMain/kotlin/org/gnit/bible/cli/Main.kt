package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.VersePointer
import org.gnit.bible.bookNumber
import org.gnit.bible.bookNameEnglishCapital
import org.gnit.bible.formatHeader

private fun parseVersePointerOrThrow(
    translation: org.gnit.bible.Translation,
    bookTokens: List<String>,
    chapterVerse: String
): VersePointer {
    val bookString = bookTokens.joinToString(separator = " ") { it.lowercase() }

    val bookNumber = try {
        bookNumber(bookString)
    } catch (_: Exception) {
        throw UsageError("Unknown book '$bookString'. Run 'bbl list books' to see supported book names.")
    }

    val chapterVerseSplit = chapterVerse.split(":")
    if (chapterVerseSplit.isEmpty() || chapterVerseSplit.size > 2) {
        throw UsageError("Invalid reference '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, or CHAPTER:START-END (e.g. '3', '3:16', '3:16-18').")
    }

    val chapterNumber = chapterVerseSplit[0].toIntOrNull()
        ?: throw UsageError("Invalid chapter in '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, or CHAPTER:START-END (e.g. '3', '3:16', '3:16-18').")

    val maxChapter = Books.maxChapter(bookNumber)
    if (chapterNumber !in 1..maxChapter) {
        throw UsageError("Chapter $chapterNumber is out of range for ${bookNameEnglishCapital(bookNumber)}. Valid range: 1..$maxChapter.")
    }

    val (startVerse, endVerse) = if (chapterVerseSplit.size == 2) {
        val versePart = chapterVerseSplit[1]
        val rangeParts = versePart.split("-")
        if (rangeParts.isEmpty() || rangeParts.size > 2) {
            throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE or CHAPTER:START-END (e.g. '3:16', '3:16-18').")
        }

        val start = rangeParts[0].toIntOrNull()
            ?: throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE or CHAPTER:START-END (e.g. '3:16', '3:16-18').")

        val end = if (rangeParts.size == 2) {
            rangeParts[1].toIntOrNull()
                ?: throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE or CHAPTER:START-END (e.g. '3:16', '3:16-18').")
        } else {
            null
        }

        if (end != null && end < start) {
            throw UsageError("Invalid verse range $start-$end. Start verse must be <= end verse.")
        }

        start to end
    } else {
        null to null
    }

    return VersePointer(
        translation = translation,
        book = bookNumber,
        chapter = chapterNumber,
        startVerse = startVerse,
        endVerse = endVerse
    )
}

private fun validateVerseRangeOrThrow(pointer: VersePointer, chapterText: String) {
    val start = pointer.startVerse ?: return
    val end = pointer.endVerse

    val maxVerses = Bible.splitChapterToVerses(chapterText).size
    val ref = "${bookNameEnglishCapital(pointer.book)} ${pointer.chapter}"

    if (start !in 1..maxVerses) {
        throw UsageError("Verse $start is out of range for $ref. Valid range: 1..$maxVerses.")
    }

    if (end != null) {
        if (end !in 1..maxVerses) {
            throw UsageError("Verse $end is out of range for $ref. Valid range: 1..$maxVerses.")
        }
        if (end < start) {
            throw UsageError("Invalid verse range $start-$end for $ref. Start verse must be <= end verse.")
        }
    }
}

class Bbl(
    private val bible: Bible = Bible().apply { bibleResourcesReader = CliBibleResourcesReader() }
) : CliktCommand() {

    override val invokeWithoutSubcommand = true
    val book: List<String> by argument().multiple(default = listOf("gen"))
    val chapterVerse: String by argument().default("1")
    val versionFlag by option("-v", "--version", help = "prints out software version of this program").flag()

    lateinit var versePointer: VersePointer
    lateinit var chapterText: String
    lateinit var selectedVerses: String

    init {
        subcommands(
            In(bible),
            //SearchCli(env = Environment.PRODUCTION, config),
            RandCli(bible),
            ListCli(bible),
            InstallCli(bible),
            UninstallCli(bible),
            ConfigCli(bible),

            // this is for developers to create new bbl pack zip file for bbl
            PackCli(bible),
        )
    }

    override fun aliases(): Map<String, List<String>> = mapOf(
        "get" to listOf("install"),
        "rm" to listOf("uninstall"),
        "remove" to listOf("uninstall"),
        "delete" to listOf("uninstall")
    )

    override fun run() {

        if (versionFlag) {
            echo(versionOutput)
        } else {
            versePointer = parseVersePointerOrThrow(
                translation = bible.defaultTranslationFromSettings(),
                bookTokens = book,
                chapterVerse = chapterVerse
            )

            val subCommand = currentContext.invokedSubcommand
            if (subCommand == null) {
                chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
                validateVerseRangeOrThrow(versePointer, chapterText)
                selectedVerses = Bible.selectVerses(versePointer, chapterText)
                if (bible.showHeaderFromSettings()) {
                    echo(formatHeader(versePointer))
                }
                echo(selectedVerses)
            } else {
                // going to move on subCommand
                currentContext.findOrSetObject { versePointer }
            }
        }
    }
}

class In(
    private val bible: Bible
) : CliktCommand() {

    val translationOverrides: List<String> by argument(help = "specify one or more translations with translation code(s)")
        .multiple()
    val versePointer by requireObject<VersePointer>()

    lateinit var selectedVerses: String

    override fun run() {
        val codes = translationOverrides
            .map { it.lowercase() }
            .distinct()

        val translations = codes.mapNotNull { code ->
            if (!bible.findTranslationByCode(code)) {
                echo("Translation code '$code' not found", err = true)
                null
            } else {
                bible.availableTranslations().first { it.code == code }
            }
        }

        if (translations.size != codes.size) return
        val firstTranslation = translations.first()

        if (translations.size == 1) {
            versePointer.translation = firstTranslation
            val chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
            validateVerseRangeOrThrow(versePointer, chapterText)
            selectedVerses = Bible.selectVerses(versePointer, chapterText)
            if (bible.showHeaderFromSettings()) {
                echo(formatHeader(versePointer))
            }
            echo(selectedVerses)
            return
        }

        val versesByCode = translations.associate { translation ->
            val chapterText = bible.verses(translation.code, versePointer.book, versePointer.chapter)
            translation.code to Bible.splitChapterToVerses(chapterText).map { it.trim() }.toTypedArray()
        }

        val start = versePointer.startVerse
        val end = versePointer.endVerse

        val verseNumbers = if (start == null) {
            val maxVerseCount = versesByCode.values.maxOfOrNull { it.size } ?: 0
            1..maxVerseCount
        } else {
            val maxVerseCount = versesByCode.values.maxOfOrNull { it.size } ?: 0
            val requestedLast = end ?: start
            val last = minOf(requestedLast, maxVerseCount)
            if (start > last) IntRange.EMPTY else start..last
        }

        if (bible.showHeaderFromSettings()) {
            val headerEndVerse = if (end == null) null else verseNumbers.lastOrNull()
            val headerPointer = VersePointer(
                translation = firstTranslation,
                book = versePointer.book,
                chapter = versePointer.chapter,
                startVerse = versePointer.startVerse,
                endVerse = headerEndVerse
            )
            echo(formatHeader(headerPointer))
        }

        val lastVerse = verseNumbers.last
        for (verseNumber in verseNumbers) {
            translations.forEach { translation ->
                val verses = versesByCode.getValue(translation.code)
                if (verseNumber - 1 < verses.size) {
                    echo("$verseNumber ${verses[verseNumber - 1]}")
                }
            }
            if (verseNumber != lastVerse) {
                echo("")
            }
        }
    }
}

fun main(args: Array<String>) {
    Bbl().main(args)
}
