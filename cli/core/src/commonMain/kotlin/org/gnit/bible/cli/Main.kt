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
import org.gnit.bible.BblVersion.VERSION
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.CompareBy
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.LoggingSetup

private const val OPEN_ENDED_VERSE_RANGE = -1

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
            HistoryCli(bible),
        )
    }
    override fun help(context: Context): String = """
        Read, search Holy Bible in command line
        
        bbl gen 1
        bbl john 3:16
        bbl matt 7:24-
        bbl matt 28:18-20
        bbl john 3:16 in kjv
        bbl john 3:16 in kjv tb
        bbl john 3:16 in kjv tb lsg

        bbl search Jesus Christ
        bbl search Jesus Christ limit 3
        bbl search Jesus Christ in kjv
        bbl search Jesus Christ in romans
        bbl search Jesus Christ in romans 3
        bbl search Jesus Christ in romans 5-12
        bbl search Jesus Christ in romans 5-12 in kjv
        bbl search righteous servant justify many in webus tb lsg
        bbl search riding on a donkey in minor prophets
        bbl search love one another in johns letters
        bbl search jews gentiles in paul
        bbl search Goliath in david
        bbl search Adam in nt
        bbl search "Jesus wept"
        bbl search "your faith" in gospels

        bbl rand (gospels|nt|ot|[category])
        bbl list (translations|books|categories)
        bbl (install|uninstall) kjv
        bbl config (translation kjv|searchResult 20|[key] [value])
        bbl hisotry (read|search|config)
    """.trimIndent()


    override fun aliases(): Map<String, List<String>> = mapOf(
        // alias of install
        "get" to listOf("install"),
        "pull" to listOf("install"),

        // alias of uninstall
        "rm" to listOf("uninstall"),
        "remove" to listOf("uninstall"),
        "del" to listOf("uninstall"),
        "delete" to listOf("uninstall"),

        // alias of list
        "ls" to listOf("list")
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
            throw UsageError("Invalid reference '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, CHAPTER:START-END, or CHAPTER:START- (e.g. '3', '3:16', '3:16-18', '3:16-').")
        }

        val chapterNumber = chapterVerseSplit[0].toIntOrNull()
            ?: throw UsageError("Invalid chapter in '$chapterVerse'. Use CHAPTER, CHAPTER:VERSE, CHAPTER:START-END, or CHAPTER:START- (e.g. '3', '3:16', '3:16-18', '3:16-').")

        val maxChapter = Books.maxChapter(bookNumber)
        if (chapterNumber !in 1..maxChapter) {
            throw UsageError("Chapter $chapterNumber is out of range for ${Books.bookNameEnglishCapital(bookNumber)}. Valid range: 1..$maxChapter.")
        }

        val (startVerse, endVerse) = if (chapterVerseSplit.size == 2) {
            val versePart = chapterVerseSplit[1]
            val rangeParts = versePart.split("-")
            if (rangeParts.isEmpty() || rangeParts.size > 2) {
                throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE, CHAPTER:START-END, or CHAPTER:START- (e.g. '3:16', '3:16-18', '3:16-').")
            }

            val start = rangeParts[0].toIntOrNull()
                ?: throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE, CHAPTER:START-END, or CHAPTER:START- (e.g. '3:16', '3:16-18', '3:16-').")

            val end = if (rangeParts.size == 2) {
                if (rangeParts[1].isEmpty()) {
                    OPEN_ENDED_VERSE_RANGE
                } else {
                    rangeParts[1].toIntOrNull()
                        ?: throw UsageError("Invalid verse reference '$chapterVerse'. Use CHAPTER:VERSE, CHAPTER:START-END, or CHAPTER:START- (e.g. '3:16', '3:16-18', '3:16-').")
                }
            } else {
                null
            }

            if (end != null && end != OPEN_ENDED_VERSE_RANGE && end < start) {
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
        versePointer = resolveOpenEndedVerseRange(versePointer, chapterText)
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
        BblHistory.record(bible, BblHistory.normalizeReadCommand(book, chapterVerse))
    }
}

class In(
    private val bible: Bible
) : CoreCliktCommand() {

    val translationOverrides: List<String> by argument(help = "specify one or more translations with translation code(s)")
        .multiple()
    val versePointer by requireObject<VersePointer>()

    lateinit var selectedVerses: String

    override fun help(context: Context): String = "Read verses in one or more translations. (e.g. bbl ex 20 in kjv)"

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
            var translatedPointer = versePointer.copy(translation = firstTranslation)
            val chapterText =
                bible.verses(translatedPointer.translation.code, translatedPointer.book, translatedPointer.chapter)
            translatedPointer = resolveOpenEndedVerseRange(translatedPointer, chapterText)
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
            BblHistory.record(
                bible,
                BblHistory.command("bbl", versePointerBookAndChapter(), "in", codes.joinToString(" "))
            )
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

        when (bible.compareByFromSettings()) {
            CompareBy.block -> {
                translations.forEach { translation ->
                    var translatedPointer = versePointer.copy(translation = translation)
                    val chapterText = bible.verses(translation.code, translatedPointer.book, translatedPointer.chapter)
                    translatedPointer = resolveOpenEndedVerseRange(translatedPointer, chapterText)
                    validateVerseRangeOrThrow(translatedPointer, chapterText)

                    selectedVerses = formatSelectedVersesFromChapterText(
                        chapterText = chapterText,
                        startVerse = translatedPointer.startVerse,
                        endVerse = translatedPointer.endVerse
                    )

                    echo(selectedVerses.trimEnd())
                }
            }
            CompareBy.verse -> {
                val translatedChapters = translations.map { translation ->
                    val translatedPointer = versePointer.copy(translation = translation)
                    val chapterText = bible.verses(translation.code, translatedPointer.book, translatedPointer.chapter)
                    val resolvedPointer = resolveOpenEndedVerseRange(translatedPointer, chapterText)
                    validateVerseRangeOrThrow(resolvedPointer, chapterText)
                    resolvedPointer to Bible.splitChapterToVerses(chapterText)
                }

                val startVerse = versePointer.startVerse ?: 1
                val endVerse = if (versePointer.endVerse == OPEN_ENDED_VERSE_RANGE) {
                    translatedChapters.maxOf { it.second.size }
                } else {
                    versePointer.endVerse
                        ?: versePointer.startVerse
                        ?: translatedChapters.maxOf { it.second.size }
                }

                val compared = buildString {
                    for (verseNumber in startVerse..endVerse) {
                        translatedChapters.forEach { (_, verses) ->
                            if (verseNumber <= verses.size) {
                                append(verseNumber)
                                append(' ')
                                append(verses[verseNumber - 1].trimEnd())
                                append('\n')
                            }
                        }
                    }
                }

                echo(compared.trimEnd())
            }
        }
        BblHistory.record(
            bible,
            BblHistory.command("bbl", versePointerBookAndChapter(), "in", codes.joinToString(" "))
        )
    }

    private fun versePointerBookAndChapter(): String {
        val bookName = Books.allBookNames[versePointer.book].first()
        val versePart = when {
            versePointer.startVerse != null && versePointer.endVerse != null -> {
                "${versePointer.chapter}:${versePointer.startVerse}-${versePointer.endVerse}"
            }
            versePointer.startVerse != null -> "${versePointer.chapter}:${versePointer.startVerse}"
            else -> versePointer.chapter.toString()
        }
        return "$bookName $versePart"
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

private fun resolveOpenEndedVerseRange(pointer: VersePointer, chapterText: String): VersePointer {
    if (pointer.endVerse != OPEN_ENDED_VERSE_RANGE) {
        return pointer
    }

    return pointer.copy(endVerse = Bible.splitChapterToVerses(chapterText).size)
}

fun main(args: Array<String>) {
    LoggingSetup.suppressKotlinLoggingStartupMessage()
    Bbl().main(platformCommandLineArgs(args))
}

fun cliMain(args: Array<String>) {
    main(args)
}
