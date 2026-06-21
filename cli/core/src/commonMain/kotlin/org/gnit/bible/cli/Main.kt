package org.gnit.bible.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.gnit.bible.*
import org.gnit.bible.BblVersion.VERSION

private const val OPEN_ENDED_VERSE_RANGE = -1

class Bbl(
    val bible: Bible = Bible(),
    searchBackendProvider: ((Translation) -> SearchBackend)? = null
) : CoreCliktCommand() {

    override val invokeWithoutSubcommand = true

    val book: List<String> by argument(completionCandidates = CompletionCandidates.Fixed(bookNames + subCommands)).multiple(default = listOf("gen"))
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
            HelpCli(),
            CompletionCommand(),
        )
    }

    override fun help(context: Context): String = """
        Read, search Holy Bible in command line
        
        bbl gen 1                                   read a chapter of default bible
        bbl john 3:16                               show a specific verse
        bbl matt 7:24-                              from a verse to end of the chapter
        bbl matt 28:18-20                           read range of verses
        bbl john 3:16 in kjv                        read a verse in specific bible
        bbl john 3:16 in kjv tb                     compare kjv and tb
        bbl john 3:16 in kjv tb lsg ..              compare 3 or more translations
        bbl john 3:16 in en de fr es                specify language name or lang code
        bbl search Jesus Christ                     search entire bible by terms
        bbl s Jesus Christ limit 3                  specify number of search results
        bbl s Jesus Christ in kjv                   search in other version of bible
        bbl s Jesus Christ in romans                filter by a book
        bbl s Jesus Christ in rom 3                 filter by a chapter
        bbl s Jesus in rom 5-12                     filter by chapter range
        bbl s Jesus in rom 5-12 in kjv              chapter range and in other bible
        bbl s jews gentiles in paul                 filter by category i.e. set of books
        bbl s "Jesus wept"                          exact search by double quotation
        bbl s "your faith" in gospels               exact search filtered by category
        bbl rand (gospels|nt|ot|[category])         random verse from all or part of bible
        bbl list (translations|books|categories)    list bibles and filters
        bbl (install|uninstall) kjv                 download/delete one or more bible(s)
        bbl config ([key]|translation)              show config value of [key]
        bbl config ([key] [value]|translation kjv)  set config [key] to [value]
        bbl hisotry (read|search|config)            show or filter past commands
        bbl help [sub command]                      learn how to use bbl and sub commands
    """.trimIndent()

    companion object {
        val bookNames = setOf( "genesis", "exodus", "leviticus", "numbers", "deuteronomy", "joshua", "judges", "ruth", "1samuel", "2samuel", "1kings", "2kings", "1chronicles", "2chronicles", "ezra", "nehemiah", "esther", "job", "psalms", "proverbs", "ecclesiastes", "songofsolomon", "isaiah", "jeremiah", "lamentations", "ezekiel", "daniel", "hosea", "joel", "amos", "obadiah", "jonah", "micah", "nahum", "habakkuk", "zephaniah", "haggai", "zechariah", "malachi", "matthew", "mark", "luke", "john", "acts", "romans", "1corinthians", "2corinthians", "galatians", "ephesians", "philippians", "colossians", "1thessalonians", "2thessalonians", "1timothy", "2timothy", "titus", "philemon", "hebrews", "james", "1peter", "2peter", "1john", "2john", "3john", "jude", "revelation")
        val subCommands = setOf("search", "rand", "list", "install", "uninstall", "config", "history", "help", "generate-completion")
    }

    override fun aliases(): Map<String, List<String>> = mapOf(

        "s" to listOf("search"),
        "r" to listOf("rand"),
        "ls" to listOf("list"),

        "get" to listOf("install"),
        "pull" to listOf("install"),

        "rm" to listOf("uninstall"),
        "remove" to listOf("uninstall"),
        "del" to listOf("uninstall"),
        "delete" to listOf("uninstall"),

        "conf" to listOf("config"),
        "c" to listOf("config"),

        "h" to listOf("history"),

        "completion" to listOf("generate-completion"),
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
        val translationCodeOrLanguages = translationOverrides
            .map { it.lowercase() }
            .distinct()

        val translations: List<Translation> = translationCodeOrLanguages.map { translationCodeOrLanguage ->
            var found: Translation? = bible.availableTranslations().firstOrNull { it.code == translationCodeOrLanguage }
            if (found == null){
                val foundLanguage: Language = Language.parse(translationCodeOrLanguage)
                    ?: throw UsageError("$translationCodeOrLanguage is not either translation code or language")
                found = SupportedTranslation.defaultTranslationOf(foundLanguage)
            }
            found
        }

        if (translations.isEmpty()) {
            throw UsageError("Missing translation code(s) or language")
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
                BblHistory.command("bbl", versePointerBookAndChapter(), "in", translationCodeOrLanguages.joinToString(" "))
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
            BblHistory.command("bbl", versePointerBookAndChapter(), "in", translationCodeOrLanguages.joinToString(" "))
        )
    }

    private fun versePointerBookAndChapter(): String {
        return formatBookChapterVerse(
            versePointer.book, versePointer.chapter,
            versePointer.startVerse, versePointer.endVerse
        )
    }
}

internal fun formatBookChapterVerse(
    bookNumber: Int, chapter: Int,
    startVerse: Int?, endVerse: Int?
): String {
    val bookName = Books.allBookNames[bookNumber].first()
    val versePart = when {
        startVerse != null && endVerse != null -> {
            if (endVerse == OPEN_ENDED_VERSE_RANGE) {
                "$chapter:$startVerse-"
            } else {
                "$chapter:$startVerse-$endVerse"
            }
        }
        startVerse != null -> "$chapter:$startVerse"
        else -> chapter.toString()
    }
    return "$bookName $versePart"
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
