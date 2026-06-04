package org.gnit.bible.cli

import com.github.ajalt.clikt.core.UsageError
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.BblVersion

object CliSupport {
    val versionOutput = """
        bbl version ${BblVersion.cliVersion}
        While you are in front of your console, you are not alone. God is with you.
        Always go back to the Word of God especially in difficulty.
    """.trimIndent()

    fun parseVersePointerOrThrow(
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

    fun validateVerseRangeOrThrow(pointer: VersePointer, chapterText: String) {
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
}
