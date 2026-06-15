package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.BblVersion
import org.gnit.bible.BblVersion.VERSION
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.LoggingSetup

class Bbl(
    val bible: Bible = Bible(),
    searchBackendProvider: ((Translation) -> SearchBackend)? = null
) : CoreCliktCommand() {

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
            SearchCli(bible, backendProvider = searchBackendProvider),
            RandCli(bible),
            ListCli(bible),
            InstallCli(bible),
            UninstallCli(bible),
            ConfigCli(bible),
        )
    }

    override fun help(context: Context): String = "The bbl (Bible) command line tool."

    override fun aliases(): Map<String, List<String>> = mapOf(
        "get" to listOf("install"),
        "rm" to listOf("uninstall"),
        "remove" to listOf("uninstall"),
        "delete" to listOf("uninstall")
    )

    private fun parseVersePointerOrThrow(
        translation: Translation,
        bookTokens: List<String>,
        chapterVerse: String
    ): VersePointer {
        val bookString = bookTokens.joinToString(separator = " ") { it.lowercase() }

        val bookNumber = runCatching { Books.bookNumber(bookString) }.getOrNull()
            ?: throw UsageError("Unknown book '$bookString'. Run 'bbl list books' to see supported book names.")

        val chapterVerseSplit = chapterVerse.split(":")
        if (chapterVerseSplit.isEmpty() || chapterVerseSplit.size > 2) {
            throw UsageError("Invalid reference '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, or CHAPTER:START-END (e.g. '3', '3:16', '3:16-18').")
        }

        val chapterNumber = chapterVerseSplit[0].toIntOrNull()
            ?: throw UsageError("Invalid chapter in '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, or CHAPTER:START-END (e.g. '3', '3:16', '3:16-18').")

        val maxChapter = Books.maxChapter(bookNumber)
        if (chapterNumber !in 1..maxChapter) {
            throw UsageError("Chapter $chapterNumber is out of range for ${Books.bookNameEnglishCapital(bookNumber)}. Valid range: 1..$maxChapter.")
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

    override fun run() {
        if (versionFlag) {
            echo(
                """
                bbl version $VERSION
                While you are in front of your console, you are not alone. God is with you.
                Always go back to the Word of God especially in difficulty.
            """.trimIndent()
            )
            return
        }

        val invokedSubcommand = currentContext.invokedSubcommand
        if (invokedSubcommand != null && invokedSubcommand.commandName != "in") {
            return
        }

        versePointer = parseVersePointerOrThrow(
            translation = bible.defaultTranslationFromSettings(),
            bookTokens = book,
            chapterVerse = chapterVerse
        )
        currentContext.findOrSetObject { versePointer }

        if (invokedSubcommand != null) {
            return
        }

        if (!bible.findTranslationByCode(versePointer.translation.code)) {
            throw UsageError("Translation '${versePointer.translation.code}' is not installed. Run 'bbl install ${versePointer.translation.code}' first.")
        }

        chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
        validateVerseRangeOrThrow(versePointer, chapterText)

        selectedVerses = formatSelectedVersesFromChapterText(
            chapterText = chapterText,
            startVerse = versePointer.startVerse,
            endVerse = versePointer.endVerse
        )

        if (bible.showHeaderFromSettings()) {
            echo(Books.formatHeader(versePointer))
        }

        echo(selectedVerses.trimEnd())

        if (versePointer.startVerse != null && versePointer.endVerse != null) {
            echo("")
        }
    }
}

class In(
    private val bible: Bible
) : CoreCliktCommand() {

    val translationOverrides: List<String> by argument(help = "specify one or more translations with translation code(s)")
        .multiple()
    val versePointer by requireObject<VersePointer>()

    lateinit var selectedVerses: String

    override fun help(context: Context): String = "Read verses from the Bible in one or more translations."

    override fun run() {
        val codes = translationOverrides
            .map { it.lowercase() }
            .distinct()

        val translations = codes.map { code ->
            bible.availableTranslations().firstOrNull { it.code == code }
                ?: throw UsageError("Translation code '$code' not found")
        }

        if (translations.isEmpty()) {
            throw UsageError("Missing translation code(s)")
        }

        val firstTranslation = translations.first()

        if (translations.size == 1) {
            val translatedPointer = versePointer.copy(translation = firstTranslation)
            val chapterText =
                bible.verses(translatedPointer.translation.code, translatedPointer.book, translatedPointer.chapter)
            validateVerseRangeOrThrow(translatedPointer, chapterText)

            selectedVerses = formatSelectedVersesFromChapterText(
                chapterText = chapterText,
                startVerse = translatedPointer.startVerse,
                endVerse = translatedPointer.endVerse
            )

            if (bible.showHeaderFromSettings()) {
                echo(Books.formatHeader(translatedPointer))
            }

            echo(selectedVerses.trimEnd())
            return
        }

        if (bible.showHeaderFromSettings()) {
            val headerPointer = versePointer.copy(
                translation = firstTranslation,
                startVerse = versePointer.startVerse,
                endVerse = versePointer.endVerse
            )
            echo(Books.formatHeader(headerPointer))
        }

        translations.forEach { translation ->
            val translatedPointer = versePointer.copy(translation = translation)
            val chapterText = bible.verses(translation.code, translatedPointer.book, translatedPointer.chapter)
            validateVerseRangeOrThrow(translatedPointer, chapterText)

            selectedVerses = formatSelectedVersesFromChapterText(
                chapterText = chapterText,
                startVerse = translatedPointer.startVerse,
                endVerse = translatedPointer.endVerse
            )

            echo(selectedVerses.trimEnd())
        }
    }
}

private fun validateVerseRangeOrThrow(pointer: VersePointer, chapterText: String) {
    val start = pointer.startVerse ?: return
    val end = pointer.endVerse

    val maxVerses = Bible.splitChapterToVerses(chapterText).size
    val ref = "${Books.bookNameEnglishCapital(pointer.book)} ${pointer.chapter}"

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

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    Bbl().main(platformCommandLineArgs(args))
}

fun cliMain(args: Array<String>) {
    main(args)
}
