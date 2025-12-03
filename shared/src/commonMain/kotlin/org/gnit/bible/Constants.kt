package org.gnit.bible

const val SERVER_PORT = 8081
const val DOWNLOADABLE_BIBLE_LIST_URL = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bbllist.json"
const val DOWNLOADABLE_BIBLE_BASE_URL = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks"
const val MANIFEST_JSON_POSTFIX = ".0.manifest.json"

val downloadableTranslations = listOf(
    Translation("abtag", "tl", "Ang Biblia", "Ang Biblia", 1905, "Public Domain"),
    Translation("ayt", "id", "The Opened Bible", "Alkitab Yang Terbuka", 2024, "CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT"),
    Translation("irvben", "bn", "Indian Revised Version - Bengali", "ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvguj", "gu", "Indian Revised Version - Gujarati", "ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvhin", "hi", "Indian Revised Version - Hindi", "इंडियन रिवाइज्ड वर्जन (IRV) हिंदी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvmar", "mr", "Indian Revised Version - Marathi", "इंडियन रीवाइज्ड वर्जन (IRV) मराठी", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvtam", "ta", "Indian Revised Version - Tamil", "இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ்", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvtel", "te", "Indian Revised Version - Telugu", "ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("irvurd", "ur", "Indian Revised Version - Urdu", "इंडियन रिवाइज्ड वर्जन (IRV) उर्दू", 2019, "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."),
    Translation("kttv", "vi", "Vietnamese Bible 1925", "Kinh Thánh Tiếng Việt", 1925, "Public Domain"),
    Translation("th1971", "th", "Thai Bible 1925", "พระคริสตธรรมคัมภีร์ ฉบับ1971", 1971, "Public Domain")
)

val downloadableTranslationCodeList = downloadableTranslations.map { it.code }
