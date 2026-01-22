package org.gnit.bible

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
){

    val language: Language
        get() = languageCode.toLanguage()

    fun shortName() = code.uppercase()

    fun books(): HashMap<Int, String> {
        val names = bookNames()
        val map = HashMap<Int, String>()
        for(i in 1..66){
            map[i] = names[i-1]
        }
        return map
    }

    fun bookNames(): Array<String> {
        return if(customBookNamesConcat == null) {
            languageCode.toLanguage().bookNames()
        }else{
            customBookNamesConcat.split(",").toTypedArray()
        }
    }

    fun toJson(): String {
        return Json.encodeToString( this)
    }

    companion object {

        fun fromJson(json: String): Translation {
            return Json.decodeFromString(json)
        }

        // search common (embedded in cmp)
        val webus = Translation(code = "webus", languageCode = Language.en.code, englishName = "World English Bible", nativeName = "World English Bible", year = 2000, copyright = "Public Domain")
        val kjv = Translation(code = "kjv", languageCode = Language.en.code, englishName = "King James Version", nativeName = "King James Version", year = 1611, copyright = "Public Domain")
        val rvr09 = Translation(code = "rvr09", languageCode = Language.es.code, englishName = "Reina-Valera", nativeName = "Reina-Valera", year = 1909, copyright = "Public Domain")
        val tb = Translation(code = "tb", languageCode = Language.pt.code, englishName = "Brazilian Translation", nativeName = "Tradução Brasileira", year = 1917, copyright = "Public Domain")
        val delut = Translation(code = "delut", languageCode = Language.de.code, englishName = "Luther Bible", nativeName = "Lutherbibel", year = 1912, copyright = "Public Domain")
        val lsg = Translation(code = "lsg", languageCode = Language.fr.code, englishName = "Louis Segond", nativeName = "Bible Segond", year = 1910, copyright = "Public Domain")
        val sinod = Translation(code = "sinod", languageCode = Language.ru.code, englishName = "Russian Synodal Bible", nativeName = "Синодальный перевод", year = 1876, copyright = "Public Domain")
        val svrj = Translation(code = "svrj", languageCode = Language.nl.code, englishName = "Statenvertaling Jongbloed edition", nativeName = "Statenvertaling Jongbloed-editie", year = 1888, copyright = "Public Domain")
        val rdv24 = Translation(code = "rdv24", languageCode = Language.it.code, englishName = "Revised Diodati Version", nativeName = "Versione Diodati Riveduta", year = 1924, copyright = "Public Domain")
        val sven = Translation(code = "sven", languageCode = Language.sv.code, englishName = "Svenska 1917", nativeName = "1917 års kyrkobibel", year = 1917, copyright = "Public Domain")

        // search morfologik (embedded in cmp)
        val ubg = Translation(code = "ubg", languageCode = Language.pl.code, englishName = "Updated Gdansk Bible", nativeName = "Uwspółcześniona Biblia gdańska", year = 2017, copyright = "© 2017 Fundacja Wrota Nadziei (Gate of Hope Foundation). Non-commercial use of unaltered text permitted.")
        val ubio = Translation(code = "ubio", languageCode = Language.uk.code, englishName = "Ukrainian Bible, Ivan Ogienko", nativeName = "Біблія в пер. Івана Огієнка", year = 1962, copyright = "CC BY-SA 4.0 © 1962 Українське Біблійне Товариство / Ukrainian Bible Society")

        // search smartcn (embedded in cmp)
        val cunp = Translation(code = "cunp", languageCode = Language.zh.code, englishName = "Chinese Union Version with New Punctuation", nativeName = "新標點和合本", year = 1919, copyright = "Public Domain")

        // search nori (embedded in cmp)
        val krv = Translation(code = "krv", languageCode = Language.ko.code, englishName = "Korean Revised Version", nativeName = "개역한글", year = 1961, copyright = "Public Domain")

        // search kuromoji (embedded in cmp)
        val jc = Translation(code = "jc", languageCode = Language.ja.code, englishName = "Japanese Colloquial Bible", nativeName = "口語訳", year = 1955, copyright = "Public Domain")

        val embeddedTranslations = listOf(webus, kjv, rvr09, tb, delut, lsg, sinod, svrj, rdv24, ubg, ubio, sven, cunp, krv, jc)

        // search common (downloadable in cmp)
        val ayt = Translation("ayt", "id", "The Opened Bible", "Alkitab Yang Terbuka", 2024, "CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT")
        val th1971 = Translation("th1971", "th", "Thai Bible 1925", "พระคริสตธรรมคัมภีร์ ฉบับ1971", 1971, "Public Domain")
        val irvhin = Translation("irvhin", "hi", "Indian Revised Version - Hindi", "इंडियन रिवाइज्ड वर्जन (IRV) हिंदी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val irvben = Translation("irvben", "bn", "Indian Revised Version - Bengali", "ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val irvtam = Translation("irvtam", "ta", "Indian Revised Version - Tamil", "இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ்", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val irvtel = Translation("irvtel", "te", "Indian Revised Version - Telugu", "ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val npiulb = Translation("npiulb", "ne", "Nepali language, Unlocked Literal Bible", "पवित्र बाइबल", 2019,  "CC BY-SA 4.0 © 2019 Door43 World Missions Community")

        // search extra (downloadable in cmp)
        val abtag = Translation("abtag", "tl", "Ang Biblia", "Ang Biblia", 1905, "Public Domain")
        val kttv = Translation("kttv", "vi", "Vietnamese Bible 1925", "Kinh Thánh Tiếng Việt", 1925, "Public Domain")
        val irvguj = Translation("irvguj", "gu", "Indian Revised Version - Gujarati", "ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val irvmar = Translation("irvmar", "mr", "Indian Revised Version - Marathi", "इंडियन रीवाइज्ड वर्जन (IRV) मराठी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
        val irvurd = Translation("irvurd", "ur", "Indian Revised Version - Urdu", "इंडियन रिवाइज्ड वर्जन (IRV) उर्दू", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.")
    }
}

fun List<Translation>.toJson(): String {
    return Json.encodeToString(this)
}