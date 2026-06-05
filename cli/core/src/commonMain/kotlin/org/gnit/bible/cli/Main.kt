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

    override fun run() {
        if (versionFlag) {
            echo(CliSupport.versionOutput)
            return
        }

        versePointer = CliSupport.parseVersePointerOrThrow(
            translation = bible.defaultTranslationFromSettings(),
            bookTokens = book,
            chapterVerse = chapterVerse
        )
        currentContext.findOrSetObject { versePointer }

        if (currentContext.invokedSubcommand != null) {
            return
        }

        if (!bible.findTranslationByCode(versePointer.translation.code)) {
            throw UsageError("Translation '${versePointer.translation.code}' is not installed. Run 'bbl install ${versePointer.translation.code}' first.")
        }

        chapterText = bible.verses(versePointer.translation.code, versePointer.book, versePointer.chapter)
        CliSupport.validateVerseRangeOrThrow(versePointer, chapterText)

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
            val chapterText = bible.verses(translatedPointer.translation.code, translatedPointer.book, translatedPointer.chapter)
            CliSupport.validateVerseRangeOrThrow(translatedPointer, chapterText)

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
            val headerPointer = versePointer.copy(
                translation = firstTranslation,
                startVerse = versePointer.startVerse,
                endVerse = headerEndVerse
            )
            echo(Books.formatHeader(headerPointer))
        }

        val lastVerse = verseNumbers.lastOrNull() ?: return
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
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    Bbl().main(platformCommandLineArgs(args))
}

fun cliMain(args: Array<String>) {
    main(args)
}
