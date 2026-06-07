package org.gnit.bible

enum class SupportedTranslation(
    val translation: Translation,
    val embedded: Boolean,
) {
    WEBUS(Translation("webus", Language.en.code, "World English Bible", "World English Bible", 2000, "Public Domain"), embedded = true),
    KJV(Translation("kjv", Language.en.code, "King James Version", "King James Version", 1611, "Public Domain"), embedded = true),
    RVR09(Translation("rvr09", Language.es.code, "Reina-Valera", "Reina-Valera", 1909, "Public Domain"), embedded = true),
    TB(Translation("tb", Language.pt.code, "Brazilian Translation", "Tradução Brasileira", 1917, "Public Domain"), embedded = true),
    DELUT(Translation("delut", Language.de.code, "Luther Bible", "Lutherbibel", 1912, "Public Domain"), embedded = true),
    LSG(Translation("lsg", Language.fr.code, "Louis Segond", "Bible Segond", 1910, "Public Domain"), embedded = true),
    SINOD(Translation("sinod", Language.ru.code, "Russian Synodal Bible", "Синодальный перевод", 1876, "Public Domain"), embedded = true),
    SVRJ(Translation("svrj", Language.nl.code, "Statenvertaling Jongbloed edition", "Statenvertaling Jongbloed-editie", 1888, "Public Domain"), embedded = true),
    RDV24(Translation("rdv24", Language.it.code, "Revised Diodati Version", "Versione Diodati Riveduta", 1924, "Public Domain"), embedded = true),
    UBG(Translation("ubg", Language.pl.code, "Updated Gdansk Bible", "Uwspółcześniona Biblia gdańska", 2017, "© 2017 Fundacja Wrota Nadziei (Non-commercial & unaltered text)"), embedded = true),
    UBIO(Translation("ubio", Language.uk.code, "Ukrainian Bible, Ivan Ogienko", "Біблія в пер. Івана Огієнка", 1962, "CC BY-SA 4.0 © 1962 Українське Біблійне Товариство"), embedded = true),
    SVEN(Translation("sven", Language.sv.code, "Svenska 1917", "1917 års kyrkobibel", 1917, "Public Domain"), embedded = true),
    CUNP(Translation("cunp", Language.zh.code, "Chinese Union Version with New Punctuation", "新標點和合本", 1919, "Public Domain"), embedded = true),
    KRV(Translation("krv", Language.ko.code, "Korean Revised Version", "개역한글", 1961, "Public Domain"), embedded = true),
    JC(Translation("jc", Language.ja.code, "Japanese Colloquial Bible", "口語訳", 1955, "Public Domain"), embedded = true),
    AYT(Translation("ayt", "id", "The Opened Bible", "Alkitab Yang Terbuka", 2024, "CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT"), embedded = false),
    TH1971(Translation("th1971", "th", "Thai Bible 1925", "พระคริสตธรรมคัมภีร์ ฉบับ1971", 1971, "Public Domain"), embedded = false),
    IRVHIN(Translation("irvhin", "hi", "Indian Revised Version - Hindi", "इंडियन रिवाइज्ड वर्जन (IRV) हिंदी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    IRVBEN(Translation("irvben", "bn", "Indian Revised Version - Bengali", "ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    IRVTAM(Translation("irvtam", "ta", "Indian Revised Version - Tamil", "இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ்", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    NPIULB(Translation("npiulb", "ne", "Nepali language, Unlocked Literal Bible", "पवित्र बाइबल", 2019, "CC BY-SA 4.0 © 2019 Door43 World Missions Community"), embedded = false),
    ABTAG(Translation("abtag", "tl", "Ang Biblia", "Ang Biblia", 1905, "Public Domain"), embedded = false),
    KTTV(Translation("kttv", "vi", "Vietnamese Bible 1925", "Kinh Thánh Tiếng Việt", 1925, "Public Domain"), embedded = false),
    IRVGUJ(Translation("irvguj", "gu", "Indian Revised Version - Gujarati", "ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    IRVMAR(Translation("irvmar", "mr", "Indian Revised Version - Marathi", "इंडियन रीवाइज्ड वर्जन (IRV) मराठी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    IRVTEL(Translation("irvtel", "te", "Indian Revised Version - Telugu", "ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false),
    IRVURD(Translation("irvurd", "ur", "Indian Revised Version - Urdu", "इंडियन रिवाइज्ड वर्जन (IRV) उर्दू", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."), embedded = false);

    val code: String get() = translation.code
    val searchModuleId: SearchModuleId get() = translation.language.searchModuleId

    companion object {
        val all: List<Translation> get() = entries.map { it.translation }
        val embeddedTranslations: List<Translation> get() = entries.filter { it.embedded }.map { it.translation }
        val downloadableTranslations: List<Translation> get() = entries.filterNot { it.embedded }.map { it.translation }
        val embeddedCodes: Array<String> get() = embeddedTranslations.map { it.code }.toTypedArray()
        val downloadableCodes: List<String> get() = downloadableTranslations.map { it.code }
        val byCode: Map<String, SupportedTranslation> get() = entries.associateBy { it.code }
    }
}
