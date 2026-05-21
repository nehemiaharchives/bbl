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
     * Version constraint shared by bbl CLI, search helper binaries, and downloadable pack manifests.
     */
    @EncodeDefault
    val bblArtifactCompatibilityVersion: String = org.gnit.bible.bblArtifactCompatibilityVersion,

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

        // search common (embedded in cmp)
        val webus: Translation get() = SupportedTranslation.WEBUS.translation
        val kjv: Translation get() = SupportedTranslation.KJV.translation
        val rvr09: Translation get() = SupportedTranslation.RVR09.translation
        val tb: Translation get() = SupportedTranslation.TB.translation
        val delut: Translation get() = SupportedTranslation.DELUT.translation
        val lsg: Translation get() = SupportedTranslation.LSG.translation
        val sinod: Translation get() = SupportedTranslation.SINOD.translation
        val svrj: Translation get() = SupportedTranslation.SVRJ.translation
        val rdv24: Translation get() = SupportedTranslation.RDV24.translation
        val sven: Translation get() = SupportedTranslation.SVEN.translation

        // search morfologik (embedded in cmp)
        val ubg: Translation get() = SupportedTranslation.UBG.translation
        val ubio: Translation get() = SupportedTranslation.UBIO.translation

        // search smartcn (embedded in cmp)
        val cunp: Translation get() = SupportedTranslation.CUNP.translation

        // search nori (embedded in cmp)
        val krv: Translation get() = SupportedTranslation.KRV.translation

        // search kuromoji (embedded in cmp)
        val jc: Translation get() = SupportedTranslation.JC.translation

        val embeddedTranslations: List<Translation>
            get() = SupportedTranslation.entries.filter { it.embedded }.map { it.translation }

        val embeddedTranslationCodes: Array<String>
            get() = embeddedTranslations.map { it.code }.toTypedArray()

        fun hasEmbeddedTranslation(translationCode: String, readerInitialized: Boolean): Boolean {
            return readerInitialized && Translation.embeddedTranslationCodes.contains(translationCode)
        }

        // search common (downloadable in cmp)
        val ayt: Translation get() = SupportedTranslation.AYT.translation
        val th1971: Translation get() = SupportedTranslation.TH1971.translation
        val irvhin: Translation get() = SupportedTranslation.IRVHIN.translation
        val irvben: Translation get() = SupportedTranslation.IRVBEN.translation
        val irvtam: Translation get() = SupportedTranslation.IRVTAM.translation
        val irvtel: Translation get() = SupportedTranslation.IRVTEL.translation
        val npiulb: Translation get() = SupportedTranslation.NPIULB.translation

        // search extra (downloadable in cmp)
        val abtag: Translation get() = SupportedTranslation.ABTAG.translation
        val kttv: Translation get() = SupportedTranslation.KTTV.translation
        val irvguj: Translation get() = SupportedTranslation.IRVGUJ.translation
        val irvmar: Translation get() = SupportedTranslation.IRVMAR.translation
        val irvurd: Translation get() = SupportedTranslation.IRVURD.translation

        val downloadableTranslationsCli: List<Translation>
            get() = SupportedTranslation.entries.filterNot { it.embedded }.map { it.translation }

        val downloadableTranslationsCmp: List<Translation>
            get() = SupportedTranslation.entries.map { it.translation }

        val downloadableTranslationCodeListCli: List<String>
            get() = downloadableTranslationsCli.map { it.code }
    }
}

fun List<Translation>.toJson(): String {
    return translationJson.encodeToString(this)
}
