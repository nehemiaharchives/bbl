package org.gnit.bible

data class VersePointer(
    var translation: Translation = Translation.webus,
    val book: Int = 0,
    val chapter: Int = 0,
    val startVerse: Int? = null,
    val endVerse: Int? = null
)
