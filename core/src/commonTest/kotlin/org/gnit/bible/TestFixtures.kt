package org.gnit.bible

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import org.gnit.bible.test.ZipUtil

object TestFixtures {
    const val KTTV_GENESIS_1_1 = "1 Ban đầu Đức Chúa Trời dựng nên trời đất."

    private val kttvManifestJson = Translation(
        code = "kttv",
        languageCode = "vi",
        englishName = "Vietnamese Bible 1925",
        nativeName = "Kinh Thánh Tiếng Việt",
        year = 1925,
        copyright = "Public Domain"
    ).toJson()

    val kttvDownloadingMockEngine = MockEngine { _ ->
        val bytes = ZipUtil.buildMinimalZip(
            listOf(
                "kttv.1.1.txt" to genesis1Kttv,
                "kttv${MANIFEST_JSON_POSTFIX}" to kttvManifestJson,
                "index/kttv.index.manifest" to "_0.cfs\n",
                "index/_0.cfs" to "CODEC"
            )
        )
        respond(
            content = bytes,
            headers = headersOf(
                "Content-Type" to listOf("application/zip"),
                "Content-Length" to listOf(bytes.size.toString())
            )
        )
    }

    val downloadableTranslationsListMockEngine = MockEngine { _ ->
        val json = """[
            {
                "code": "abtag",
                "languageCode": "tl",
                "englishName": "Ang Biblia",
                "nativeName": "Ang Biblia",
                "year": 1905,
                "copyright": "Public Domain"
            }
        ]""".trimIndent()
        respond(
            content = json,
            headers = headersOf(
                "Content-Type" to listOf("application/json"),
                "Content-Length" to listOf(json.encodeToByteArray().size.toString())
            )
        )
    }

    private val genesis1Kttv = """
    1 Ban đầu Đức Chúa Trời dựng nên trời đất.
    2 Vả, đất là vô-hình và trống không, sự mờ-tối ở trên mặt vực; Thần Đức Chúa Trời vận-hành trên mặt nước.
    3 Đức Chúa Trời phán rằng: Phải có sự sáng; thì có sự sáng.
    4 Đức Chúa Trời thấy sáng là tốt-lành, bèn phân sáng ra cùng tối.
    5 Đức Chúa Trời đặt tên sự sáng là ngày; sự tối là đêm. Vậy, có buổi chiều và buổi mai; ấy là ngày thứ nhứt.
    """.trimIndent()
}
