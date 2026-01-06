package org.gnit.bible.test

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation

object TestFixtures {

    val tmpWorkingDirForBblPack = "/tmp/bblpack-cli-create"

    const val KTTV_GENESIS_1_1 = "1 Ban đầu Đức Chúa Trời dựng nên trời đất."
    const val WEBUS_GENESIS_1_1 = "1 In the beginning, God created the heavens and the earth."
    const val WEBUS_GENESIS_1_2 = "2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters."
    const val WEBUS_GENESIS_1_3 = "3 God said, “Let there be light,” and there was light."
    const val JC_GENESIS_1_1 = "1 はじめに神は天と地とを創造された。"
    const val WEBUS_JOHN_3_16 = "For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life."
    const val WEBUS_MATT_28_18 = "Jesus came to them and spoke to them, saying, \\“All authority has been given to me in heaven and on earth."
    const val WEBUS_MATT_28_19 = "Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,"
    const val WEBUS_MATT_28_20 = "teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen."
    const val JC_JOHN_3_16 = "神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。"
    const val JC_MATT_28_18 = "イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。"
    const val JC_MATT_28_19 = "それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、"
    const val JC_MATT_28_20 = "あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。"

    private fun buildChapter(totalVerses: Int, content: Map<Int, String>): String = buildString {
        for (i in 1..totalVerses) {
            append(i).append(' ').append(content[i] ?: "placeholder verse $i")
            if (i != totalVerses) append('\n')
        }
    }

    private val webusJohn3 = buildChapter(
        totalVerses = 16,
        content = mapOf(16 to WEBUS_JOHN_3_16)
    )

    private val webusMatt28 = buildChapter(
        totalVerses = 20,
        content = mapOf(
            18 to WEBUS_MATT_28_18,
            19 to WEBUS_MATT_28_19,
            20 to WEBUS_MATT_28_20
        )
    )

    private val jcJohn3 = buildChapter(
        totalVerses = 16,
        content = mapOf(16 to JC_JOHN_3_16)
    )

    private val jcMatt28 = buildChapter(
        totalVerses = 20,
        content = mapOf(
            18 to JC_MATT_28_18,
            19 to JC_MATT_28_19,
            20 to JC_MATT_28_20
        )
    )

    private val genesisOneKttv = """
    1 Ban đầu Đức Chúa Trời dựng nên trời đất.
    2 Vả, đất là vô-hình và trống không, sự mờ-tối ở trên mặt vực; Thần Đức Chúa Trời vận-hành trên mặt nước.
    3 Đức Chúa Trời phán rằng: Phải có sự sáng; thì có sự sáng.
    4 Đức Chúa Trời thấy sáng là tốt-lành, bèn phân sáng ra cùng tối.
    5 Đức Chúa Trời đặt tên sự sáng là ngày; sự tối là đêm. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ nhứt.
    6 Đức Chúa Trời lại phán rằng: Phải có một khoảng không ở giữa nước đặng phân-rẽ nước cách với nước.
    7 Ngài làm nên khoảng không, phân-rẽ nước ở dưới khoảng không cách với nước ở trên khoảng không; thì có như vậy.
    8 Đức Chúa Trời đặt tên khoảng không là trời. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ nhì.
    9 Đức Chúa Trời lại phán rằng: Những nước ở dưới trời phải tụ lại một nơi, và phải có chỗ khô-cạn bày ra; thì có như vậy.
    10 Đức Chúa Trời đặt tên chỗ khô-cạn là đất, còn nơi nước tụ lại là biển. Đức Chúa Trời thấy điều đó là tốt-lành.
    11 Đức Chúa Trời lại phán rằng: Đất phải sanh cây-cỏ; cỏ kết hột giống, cây-trái kết quả, tùy theo loại mà có hột giống trong mình trên đất; thì có như vậy.
    12 Đất sanh cây-cỏ: Cỏ kết hột tùy theo loại, cây kết quả có hột trong mình, tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    13 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ ba.
    14 Đức Chúa Trời lại phán rằng: Phải có các vì sáng trong khoảng-không trên trời, đặng phân ra ngày với đêm, và dùng làm dấu để định thì-tiết, ngày và năm;
    15 lại dùng làm vì sáng trong khoảng không trên trời để soi xuống đất; thì có như vậy.
    16 Đức Chúa Trời làm nên hai vì sáng lớn; vì lớn hơn để cai-trị ban ngày, vì nhỏ hơn để cai-trị ban đêm; Ngài cũng làm các ngôi sao.
    17 Đức Chúa Trời đặt các vì đó trong khoảng không trên trời, đặng soi sáng đất,
    18 đặng cai-trị ban ngày và ban đêm, đặng phân ra sự sáng với sự tối. Đức Chúa Trời thấy điều đó là tốt-lành.
    19 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ tư.
    20 Đức Chúa Trời lại phán rằng: Nước phải sanh các vật sống cho nhiều, và các loài chim phải bay trên mặt đất trong khoảng không trên trời.
    21 Đức Chúa Trời dựng nên các loài cá lớn, các vật sống hay động nhờ nước mà sanh nhiều ra, tùy theo loại, và các loài chim hay bay, tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    22 Đức Chúa Trời ban phước cho các loài đó mà phán rằng: Hãy sanh-sản, thêm nhiều, làm cho đầy-dẫy dưới biển; còn các loài chim hãy sanh-sản trên đất cho nhiều.
    23 Vậy, có buổi chiều và buổi mai; ấy là ngày thứ năm.
    24 Đức Chúa Trời lại phán rằng: Đất phải sanh các vật sống tùy theo loại, tức súc-vật, côn-trùng, và thú rừng, đều tùy theo loại; thì có như vậy.
    25 Đức Chúa Trời làm nên các loài thú rừng tùy theo loại, súc-vật tùy theo loại, và các côn-trùng trên đất tùy theo loại. Đức Chúa Trời thấy điều đó là tốt-lành.
    26 Đức Chúa Trời phán rằng: Chúng ta hãy làm nên loài người như hình ta và theo tượng ta, đặng quản-trị loài cá biển, loài chim trời, loài súc-vật, loài côn-trùng bò trên mặt đất, và khắp cả đất.
    27 Đức Chúa Trời dựng nên loài người như hình Ngài; Ngài dựng nên loài người giống như hình Đức Chúa Trời; Ngài dựng nên người nam cùng người nữ.
    28 Đức Chúa Trời ban phước cho loài người và phán rằng: Hãy sanh-sản, thêm nhiều, làm cho đầy-dẫy đất; hãy làm cho đất phục-tùng, hãy quản-trị loài cá dưới biển, loài chim trên trời cùng các vật sống hành-động trên mặt đất.
    29 Đức Chúa Trời lại phán rằng: Nầy, ta sẽ ban cho các ngươi mọi thứ cỏ kết hột mọc khắp mặt đất, và các loài cây sanh quả có hột giống; ấy sẽ là đồ-ăn cho các ngươi.
    30 Còn các loài thú ngoài đồng, các loài chim trên trời, và các động-vật khác trên mặt đất, phàm giống nào có sự sống thì ta ban cho mọi thứ cỏ xanh đặng dùng làm đồ-ăn; thì có như vậy.
    31 Đức Chúa Trời thấy các việc Ngài đã làm thật rất tốt-lành. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ sáu.
""".trimIndent()

    private val kttvManifestJson = Translation(
        code = "kttv",
        languageCode = "vi",
        englishName = "Vietnamese Bible 1925",
        nativeName = "Kinh Thánh Tiếng Việt",
        year = 1925,
        copyright = "Public Domain"
    ).toJson()

    private val genesisOneTh1971 = "1 (test) Thai Bible 1971 Genesis 1:1"

    private val th1971ManifestJson = Translation(
        code = "th1971",
        languageCode = "th",
        englishName = "Thai Bible 1925",
        nativeName = "พระคริสตธรรมคัมภีร์ ฉบับ1971",
        year = 1971,
        copyright = "Public Domain"
    ).toJson()

    private val webusManifestJson = Translation.webus.toJson()
    private val jcManifestJson = Translation.jc.toJson()

    val kttvDownloadingMockEngine = MockEngine { _ ->
        val bytes = ZipUtil.buildMinimalZip(
            listOf(
                "kttv.1.1.txt" to genesisOneKttv,
                "kttv${MANIFEST_JSON_POSTFIX}" to kttvManifestJson,
                // Minimal searchable index fixture (manifest + one file)
                "index/kttv.index.manifest" to "_0.cfs\n",
                "index/_0.cfs" to "CODEC"
            )
        )
        respond(
            content = bytes, headers = headersOf(
                "Content-Type" to listOf("application/zip"),
                "Content-Length" to listOf(bytes.size.toString())
            )
        )
    }

    val webusMinimalZipBytes: ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "webus.1.1.txt" to WEBUS_GENESIS_1_1,
            "webus.43.3.txt" to webusJohn3,
            "webus.40.28.txt" to webusMatt28,
            "webus$MANIFEST_JSON_POSTFIX" to webusManifestJson,
            "index/webus.index.manifest" to "_0.cfs",
            "index/_0.cfs" to "CODEC"
        )
    )

    val jcMinimalZipBytes: ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "jc.1.1.txt" to JC_GENESIS_1_1,
            "jc.43.3.txt" to jcJohn3,
            "jc.40.28.txt" to jcMatt28,
            "jc$MANIFEST_JSON_POSTFIX" to jcManifestJson,
            "index/jc.index.manifest" to "_0.cfs",
            "index/_0.cfs" to "CODEC"
        )
    )

    private val downloadableTranslationsListJson = """
        [
    {
        "code": "webus",
        "languageCode": "en",
        "englishName": "World English Bible",
        "nativeName": "World English Bible",
        "year": 2000,
        "copyright": "Public Domain"
    },
    {
        "code": "kjv",
        "languageCode": "en",
        "englishName": "King James Version",
        "nativeName": "King James Version",
        "year": 1611,
        "copyright": "Public Domain"
    },
    {
        "code": "rvr09",
        "languageCode": "es",
        "englishName": "Reina-Valera",
        "nativeName": "Reina-Valera",
        "year": 1909,
        "copyright": "Public Domain"
    },
    {
        "code": "tb",
        "languageCode": "pt",
        "englishName": "Brazilian Translation",
        "nativeName": "Tradução Brasileira",
        "year": 1917,
        "copyright": "Public Domain"
    },
    {
        "code": "delut",
        "languageCode": "de",
        "englishName": "Luther Bible",
        "nativeName": "Lutherbibel",
        "year": 1912,
        "copyright": "Public Domain"
    },
    {
        "code": "lsg",
        "languageCode": "fr",
        "englishName": "Louis Segond",
        "nativeName": "Bible Segond",
        "year": 1910,
        "copyright": "Public Domain"
    },
    {
        "code": "sinod",
        "languageCode": "ru",
        "englishName": "Russian Synodal Bible",
        "nativeName": "Синодальный перевод",
        "year": 1876,
        "copyright": "Public Domain"
    },
    {
        "code": "svrj",
        "languageCode": "nl",
        "englishName": "Statenvertaling Jongbloed edition",
        "nativeName": "Statenvertaling Jongbloed-editie",
        "year": 1888,
        "copyright": "Public Domain"
    },
    {
        "code": "rdv24",
        "languageCode": "it",
        "englishName": "Revised Diodati Version",
        "nativeName": "Versione Diodati Riveduta",
        "year": 1924,
        "copyright": "Public Domain"
    },
    {
        "code": "ubg",
        "languageCode": "pl",
        "englishName": "Updated Gdansk Bible",
        "nativeName": "Uwspółcześniona Biblia gdańska",
        "year": 2017,
        "copyright": "© 2017 Fundacja Wrota Nadziei (Gate of Hope Foundation). Non-commercial use of unaltered text permitted."
    },
    {
        "code": "ubio",
        "languageCode": "uk",
        "englishName": "Ukrainian Bible, Ivan Ogienko",
        "nativeName": "Біблія в пер. Івана Огієнка",
        "year": 1962,
        "copyright": "CC BY-SA 4.0 © 1962 Українське Біблійне Товариство / Ukrainian Bible Society"
    },
    {
        "code": "sven",
        "languageCode": "sv",
        "englishName": "Svenska 1917",
        "nativeName": "1917 års kyrkobibel",
        "year": 1917,
        "copyright": "Public Domain"
    },
    {
        "code": "cunp",
        "languageCode": "zh",
        "englishName": "Chinese Union Version with New Punctuation",
        "nativeName": "新標點和合本",
        "year": 1919,
        "copyright": "Public Domain"
    },
    {
        "code": "krv",
        "languageCode": "ko",
        "englishName": "Korean Revised Version",
        "nativeName": "개역한글",
        "year": 1961,
        "copyright": "Public Domain"
    },
    {
        "code": "jc",
        "languageCode": "ja",
        "englishName": "Japanese Colloquial Bible",
        "nativeName": "口語訳",
        "year": 1955,
        "copyright": "Public Domain"
    },
    {
        "code": "abtag",
        "languageCode": "tl",
        "englishName": "Ang Biblia",
        "nativeName": "Ang Biblia",
        "year": 1905,
        "copyright": "Public Domain"
    },
    {
        "code": "ayt",
        "languageCode": "id",
        "englishName": "The Opened Bible",
        "nativeName": "Alkitab Yang Terbuka",
        "year": 2024,
        "copyright": "CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT"
    },
    {
        "code": "irvben",
        "languageCode": "bn",
        "englishName": "Indian Revised Version - Bengali",
        "nativeName": "ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvguj",
        "languageCode": "gu",
        "englishName": "Indian Revised Version - Gujarati",
        "nativeName": "ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvhin",
        "languageCode": "hi",
        "englishName": "Indian Revised Version - Hindi",
        "nativeName": "इंडियन रिवाइज्ड वर्जन (IRV) हिंदी",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvmar",
        "languageCode": "mr",
        "englishName": "Indian Revised Version - Marathi",
        "nativeName": "इंडियन रीवाइज्ड वर्जन (IRV) मराठी",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvtam",
        "languageCode": "ta",
        "englishName": "Indian Revised Version - Tamil",
        "nativeName": "இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ்",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvtel",
        "languageCode": "te",
        "englishName": "Indian Revised Version - Telugu",
        "nativeName": "ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "irvurd",
        "languageCode": "ur",
        "englishName": "Indian Revised Version - Urdu",
        "nativeName": "इंडियन रिवाइज्ड वर्जन (IRV) उर्दू",
        "year": 2019,
        "copyright": "CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd."
    },
    {
        "code": "kttv",
        "languageCode": "vi",
        "englishName": "Vietnamese Bible 1925",
        "nativeName": "Kinh Thánh Tiếng Việt",
        "year": 1925,
        "copyright": "Public Domain"
    },
    {
        "code": "th1971",
        "languageCode": "th",
        "englishName": "Thai Bible 1925",
        "nativeName": "พระคริสตธรรมคัมภีร์ ฉบับ1971",
        "year": 1971,
        "copyright": "Public Domain"
    }
]

    """.trimIndent()

    val downloadableTranslationsListMockEngine = MockEngine { _ ->
        respond(
            content = downloadableTranslationsListJson, headers = headersOf(
                "Content-Type" to listOf("application/json"),
                "Content-Length" to listOf(downloadableTranslationsListJson.encodeToByteArray().size.toString())
            )
        )
    }

    val bblInstallMockEngine = MockEngine {
            request ->
        fun packBytes(translationCode: String): ByteArray =
            when (translationCode) {
                "kttv" -> ZipUtil.buildMinimalZip(
                    listOf(
                        "kttv.1.1.txt" to genesisOneKttv,
                        "kttv${MANIFEST_JSON_POSTFIX}" to kttvManifestJson
                    )
                )

                "th1971" -> ZipUtil.buildMinimalZip(
                    listOf(
                        "th1971.1.1.txt" to genesisOneTh1971,
                        "th1971${MANIFEST_JSON_POSTFIX}" to th1971ManifestJson
                    )
                )

                "webus" -> webusMinimalZipBytes

                "jc" -> jcMinimalZipBytes

                else -> error("Unexpected translation code: $translationCode")
            }

        when(request.url.encodedPath){
            "/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bbllist.json" -> respond(
                content = downloadableTranslationsListJson, headers = headersOf(
                    "Content-Type" to listOf("application/json"),
                    "Content-Length" to listOf(downloadableTranslationsListJson.encodeToByteArray().size.toString())
                )
            )

            "/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks/kttv.zip" -> {
                val bytes = packBytes("kttv")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks/th1971.zip" -> {
                val bytes = packBytes("th1971")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks/webus.zip" -> {
                val bytes = packBytes("webus")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks/jc.zip" -> {
                val bytes = packBytes("jc")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            else -> error("Unexpected request for path: ${request.url.encodedPath}")
        }
    }
}
