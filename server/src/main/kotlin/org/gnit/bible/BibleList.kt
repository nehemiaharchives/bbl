package org.gnit.bible

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

object BibleList {
    fun getAllTranslations(): String {
        val allTranslations = Translation.embeddedTranslations + downloadableTranslationsCli
        val jsonString = Json { prettyPrint = true }.encodeToString(allTranslations)
        return jsonString
    }
}

fun main() {

    val jsonString =  BibleList.getAllTranslations()
    val fs = FileSystem.SYSTEM
    val path = "server/src/main/resources/files/bbllist.json".toPath()
    if (fs.exists(path)) println("$path exists") else println("$path does not exist")

    fs.write(path) {
        writeUtf8(jsonString)
    }

}
