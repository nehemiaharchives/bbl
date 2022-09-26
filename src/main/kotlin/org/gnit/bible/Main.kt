package org.gnit.bible

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import org.slf4j.LoggerFactory

const val DEFAULT_TRANSLATION = "webus"

val logger = LoggerFactory.getLogger("bbl")

data class VersePointer(
    var translation: String = DEFAULT_TRANSLATION,
    val book: Int = 0,
    val chapter: Int = 0,
    val startVerse: Int? = null,
    val endVerse: Int? = null
)

fun parse(translation: String, book: List<String>, chapterVerse: String): VersePointer {

    val bookString = book.joinToString(separator = " ") { it.lowercase() }

    val bookNumber = parseBook(bookString)

    val chapterVerseSplit = chapterVerse.split(":")

    val chapterNumber = chapterVerseSplit[0].toInt()

    val startVerse = if (chapterVerseSplit.size == 2) chapterVerseSplit[1].split("-")[0].toInt() else null

    val endVerse =
        if (chapterVerseSplit.size == 2 && chapterVerse.contains("-")) chapterVerseSplit[1].split("-")[1].toInt() else null

    return VersePointer(
        translation = translation,
        book = bookNumber,
        chapter = chapterNumber,
        startVerse = startVerse,
        endVerse = endVerse
    )
}

fun readFromResources(versePointer: VersePointer): String {
    val path =
        "/data/${versePointer.translation}/${versePointer.translation}.${versePointer.book}.${versePointer.chapter}.txt"

    val text = object {}.javaClass.getResourceAsStream(path)?.use { it.reader(Charsets.UTF_8).readText() }

    return text!!
}

fun splitChapterToVerses(aChapter: String): Array<String> {
    return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
}

fun selectVerses(versePointer: VersePointer, aChapter: String): String {

    val start = versePointer.startVerse
    val end = versePointer.endVerse

    var selected = aChapter

    if (start != null) {

        val verses = splitChapterToVerses(aChapter)

        if (end == null) {
            selected = start.toString() + " " + verses[start - 1]
        } else {
            val list = mutableListOf<String>()

            (start..end).forEach { verseNumber ->
                list.add(verseNumber.toString() + " " + verses[verseNumber - 1])
            }

            selected = list.joinToString("\n")
        }
    }

    return selected
}

class Bbl(val config: Config) : CliktCommand(invokeWithoutSubcommand = true) {

    val book: List<String> by argument().multiple(default = listOf("gen"))
    val chapterVerse: String by argument().default("1")

    lateinit var versePointer: VersePointer
    lateinit var chapterText: String
    lateinit var selectedVerses: String

    override fun run() {

        val translation = config.translation
        versePointer = parse(translation, book, chapterVerse)

        val subcommand = currentContext.invokedSubcommand
        if (subcommand == null) {

            chapterText = readFromResources(versePointer)
            selectedVerses = selectVerses(versePointer, chapterText)
            echo(selectedVerses)

        } else {
            //going to move on subcommand
            currentContext.findOrSetObject { versePointer }
        }
    }
}

class In : CliktCommand(){

    val translationOverride: String by argument()
    val versePointer by requireObject<VersePointer>()

    lateinit var selectedVerses: String

    override fun run() {
        versePointer.translation = translationOverride
        selectedVerses = selectVerses(versePointer, readFromResources(versePointer))
        echo(selectedVerses)
    }
}

fun main(args: Array<String>) = Bbl(readConfigFromFileSystem()).subcommands(In()).main(args)
