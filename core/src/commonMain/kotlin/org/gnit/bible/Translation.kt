package org.gnit.bible

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val translationJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class Translation(

    /**
     * Short code for the translation eg. "kjv", "webus", "rvr09", used in following context:
     * 1. argument `translation` of [org.gnit.bible.Bible.verses] function
     * 2. argument `fileName` of [org.gnit.bible.AssetManager.download] is in the format of `"${Translation.code}.zip"`
     * 3. return value of [org.gnit.bible.AssetManager.downloadedTranslationCodes] function is a list of `Translation.code`
     */
    val code: String,

    /**
     * Language.code of the translation
     */
    val languageCode: String,

    /**
     * English name of the translation eg. "King James Version", "Luther Bible", "Chinese Union Version with New Punctuation"
     */
    val englishName: String,

    /**
     * Native name of the translation eg. "King James Version", "Lutherbibel", "新標點和合本"
     */
    val nativeName: String,

    /**
     * Year of publication eg. 1611, 1900, 1960, 1995, 2009
     */
    val year: Int,

    /**
     * Information regarding copyright if public domain it is "Public Domain" otherwise
     * eg. "Creative Commons Attribution-ShareAlike 4.0 International License by Bridge Connectivity Solutions Pvt. Ltd.",
     */
    val copyright: String,

    /**
     * In some language, name of the books are translated differently according to the translation.
     * For example, in `jc` (Japanese, Colloquial 1955) the book of ecclesiastes is translated as "伝道の書",
     * while others translate to "コヘレトの言葉" or "伝道者の書".
     * This field is a comma-separated list of 66 custom book names in order from Genesis to Revelation.
     * If null, the default book names in [org.gnit.bible.Language.bookNames]
     *
     * eg. `val customBookNames = "Genesis,Exodus,Leviticus,x,x,x,x,x,x, ... ,Revelation"`
     */
    val customBookNamesConcat: String? = null,

    @EncodeDefault
    val version: String = BblVersion.VERSION,
) {

    val language: Language
        get() = languageCode.toLanguage()

    fun shortName() = code.uppercase()

    fun books(): HashMap<Int, String> {
        val names = bookNames()
        val map = HashMap<Int, String>()
        for (i in 1..66) {
            map[i] = names[i - 1]
        }
        return map
    }

    fun bookNames(): Array<String> {
        return if (customBookNamesConcat == null) {
            languageCode.toLanguage().bookNames()
        } else {
            customBookNamesConcat.split(",").toTypedArray()
        }
    }

    fun toJson(): String {
        return translationJson.encodeToString(this)
    }

    companion object {

        fun fromJson(json: String): Translation {
            return translationJson.decodeFromString(json)
        }
    }
}

fun List<Translation>.toJson(): String {
    return translationJson.encodeToString(this)
}
