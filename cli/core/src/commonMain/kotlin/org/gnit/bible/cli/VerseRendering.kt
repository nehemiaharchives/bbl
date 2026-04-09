package org.gnit.bible.cli

import org.gnit.bible.Bible

internal fun formatSelectedVersesFromChapterText(
    chapterText: String,
    startVerse: Int?,
    endVerse: Int?
): String {
    if (startVerse == null) {
        return chapterText
    }

    val verses = Bible.splitChapterToVerses(chapterText)

    if (endVerse == null) {
        return "$startVerse ${verses[startVerse - 1].trimEnd()}"
    }

    return (startVerse..endVerse)
        .joinToString("\n") { verseNumber ->
            "$verseNumber ${verses[verseNumber - 1].trimEnd()}"
        }
}
