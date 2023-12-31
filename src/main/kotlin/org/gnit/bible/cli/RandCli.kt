package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kotlin.random.Random
import kotlin.random.nextInt

enum class RandomlyShow { verse, chapter }

class RandCli(val config: Config) : CliktCommand(name = "rand") {

    val narrowDown: String? by argument().optional()

    lateinit var versePointer: VersePointer
    lateinit var selectedVerses: String

    override fun run() {
        val randomBook = when (narrowDown) {
            "ot", "ot verse", "ot chapter" -> Random.nextInt(1..39)
            "nt", "nt verse", "nt chapter" -> Random.nextInt(40..66)
            "g", "g verse", "g chapter" -> Random.nextInt(40 .. 43)
            else -> Random.nextInt(1..66)
        }
        logger.debug("randomBook: $randomBook")
        val randomChapter = Random.nextInt(1..Chapters.maxChapter(randomBook))
        logger.debug("randomChapter: $randomChapter")

        versePointer = VersePointer(
            translation = config.translation,
            book = randomBook,
            chapter = randomChapter
        )
        val path = chapterTextPath(versePointer)
        val aChapter = selectVerses(versePointer, getResourceReader().readText(path))

        val randomBookName = bookNameCapital(randomBook)

        val narrowDownIsNull = narrowDown == null
        val doesNotSpecifyChapterOrVerse = arrayOf("ot", "nt", "g").contains(narrowDown)

        if (
            (narrowDown != null && narrowDown!!.contains("verse")) ||
            ((narrowDownIsNull || doesNotSpecifyChapterOrVerse) && config.randomlyShow == RandomlyShow.verse)
        ) {
            val splitVerses = splitChapterToVerses(aChapter)
            val randomVerse = Random.nextInt(1, splitVerses.size)
            logger.debug("showing random verse")
            selectedVerses = splitVerses[randomVerse - 1]
            echo("$randomBookName $randomChapter:$randomVerse")
            echo(selectedVerses)
        }

        if (
            (narrowDown != null && narrowDown!!.contains("chapter")) ||
            ((narrowDownIsNull || doesNotSpecifyChapterOrVerse) && config.randomlyShow == RandomlyShow.chapter)
        ) {
            logger.debug("showing random chapter")
            selectedVerses = aChapter
            echo("$randomBookName $randomChapter")
            echo(selectedVerses)
        }
    }
}