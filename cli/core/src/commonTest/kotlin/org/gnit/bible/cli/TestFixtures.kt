package org.gnit.bible.cli

import org.gnit.bible.SupportedTranslation

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.BblVersion
import org.gnit.bible.test.ZipUtil

object TestFixtures {
    const val KTTV_GENESIS_1_1 = "1 Ban đầu Đức Chúa Trời dựng nên trời đất."
    const val WEBUS_GENESIS_1_1 = "1 In the beginning, God created the heavens and the earth."
    const val JC_GENESIS_1_1 = "1 はじめに神は天と地とを創造された。"
    const val WEBUS_JOHN_3_16 = "For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life."
    const val WEBUS_MATT_28_18 = "Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth."
    const val WEBUS_MATT_28_19 = "Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,"
    const val WEBUS_MATT_28_20 = "teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen."
    const val JC_JOHN_3_16 = "神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。"
    const val JC_MATT_28_18 = "イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。"
    const val JC_MATT_28_19 = "それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、"
    const val JC_MATT_28_20 = "あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。"

    val genesisOneWebus = WEBUS_GENESIS_1_1

    val genesisTowWebus = "1 The heavens, the earth, and all their vast array were finished."

    val johnThreeWebus = buildString {
        (1..15).forEach { appendLine("$it verse text") }
        appendLine("16 $WEBUS_JOHN_3_16")
    }

    val mattTwentyEightWebus = buildString {
        (1..17).forEach { appendLine("$it verse text") }
        appendLine("18 $WEBUS_MATT_28_18")
        appendLine("19 $WEBUS_MATT_28_19")
        appendLine("20 $WEBUS_MATT_28_20")
    }

    val genesisOneJc = JC_GENESIS_1_1

    val john3Jc = buildString {
        (1..15).forEach { appendLine("$it verse text") }
        appendLine("16 $JC_JOHN_3_16")
    }

    val matthew28Jc = buildString {
        (1..17).forEach { appendLine("$it verse text") }
        appendLine("18 $JC_MATT_28_18")
        appendLine("19 $JC_MATT_28_19")
        appendLine("20 $JC_MATT_28_20")
    }

    private val kttvManifestJson = SupportedTranslation.KTTV.translation.toJson()
    private val th1971ManifestJson = SupportedTranslation.TH1971.translation.toJson()
    private val webusManifestJson = SupportedTranslation.WEBUS.translation.toJson()
    private val jcManifestJson = SupportedTranslation.JC.translation.toJson()
    private val ubgManifestJson = SupportedTranslation.UBG.translation.toJson()
    private val ubioManifestJson = SupportedTranslation.UBIO.translation.toJson()

    val webusMinimalZipBytes: ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "webus.1.1.txt" to genesisOneWebus,
            "webus.1.2.txt" to genesisTowWebus,
            "webus.43.3.txt" to johnThreeWebus,
            "webus.40.28.txt" to mattTwentyEightWebus,
            "webus$MANIFEST_JSON_POSTFIX" to webusManifestJson,
            "index/webus.index.manifest" to "_0.cfs",
            "index/_0.cfs" to "CODEC"
        )
    )

    val jcMinimalZipBytes: ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "jc.1.1.txt" to genesisOneJc,
            "jc.43.3.txt" to john3Jc,
            "jc.40.28.txt" to matthew28Jc,
            "jc$MANIFEST_JSON_POSTFIX" to jcManifestJson,
            "index/jc.index.manifest" to "_0.cfs",
            "index/_0.cfs" to "CODEC"
        )
    )

    private fun minimalZip(code: String, manifestJson: String): ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "$code.1.1.txt" to "1 test",
            "$code$MANIFEST_JSON_POSTFIX" to manifestJson,
            "index/$code.index.manifest" to "_0.cfs",
            "index/_0.cfs" to "CODEC"
        )
    )

    private fun packBytes(code: String): ByteArray = when (code) {
        "kttv" -> minimalZip(code, kttvManifestJson)
        "th1971" -> minimalZip(code, th1971ManifestJson)
        "webus" -> webusMinimalZipBytes
        "jc" -> jcMinimalZipBytes
        "ubg" -> minimalZip(code, ubgManifestJson)
        "ubio" -> minimalZip(code, ubioManifestJson)
        else -> error("Unexpected translation code: $code")
    }

    fun bblInstallMockEngine() = MockEngine { request ->
        val releaseVersion = BblVersion.VERSION
        val path = request.url.encodedPath
        val helperName = path.substringAfterLast('/')
        val helperBytes = "$helperName helper".encodeToByteArray()
        when {
            path.startsWith("${BblVersion.SERVER_RESOURCE_PATH}/bblpacks/") &&
                path.endsWith(".zip") -> {
                val code = path.substringAfterLast('/').removeSuffix(".zip")
                respond(
                    content = packBytes(code),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/zip")
                )
            }
            path.startsWith("${BblVersion.SERVER_RESOURCE_PATH_LEGACY}/bblpacks/") &&
                path.endsWith(".zip") -> {
                val code = path.substringAfterLast('/').removeSuffix(".zip")
                respond(
                    content = packBytes(code),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/zip")
                )
            }
            path.contains("/releases/download/$releaseVersion/") -> respond(
                content = helperBytes,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/octet-stream")
            )
            else -> respond(
                content = "",
                status = HttpStatusCode.NotFound
            )
        }
    }

}
