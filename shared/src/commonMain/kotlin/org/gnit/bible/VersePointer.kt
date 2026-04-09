package org.gnit.bible

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VersePointer(
    var translation: Translation = Translation.webus,
    val book: Int = 0,
    val chapter: Int = 0,
    val startVerse: Int? = null,
    val endVerse: Int? = null
){
    override fun toString(): String {
        return "${translation.code}|${bookNameEnglishCapital(book)}($book):$chapter:$startVerse${if (endVerse != null) "-$endVerse" else ""}"
    }
}

object VersePointerJson {
    private val json = Json

    fun encode(pointer: VersePointer): String = json.encodeToString(pointer)

    fun decode(payload: String): VersePointer = json.decodeFromString(payload)

    fun encodeList(pointers: List<VersePointer>): String = json.encodeToString(pointers)

    fun decodeList(payload: String): List<VersePointer> {
        if (payload.isBlank()) return emptyList()
        return json.decodeFromString(payload)
    }
}
