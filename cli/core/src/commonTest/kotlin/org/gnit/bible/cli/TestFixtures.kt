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
    const val KJV_JOHN_3_16 = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life."

    val webusJohnThreeFrom16 = listOf(
        "16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.",
        "17 For God didn’t send his Son into the world to judge the world, but that the world should be saved through him.",
        "18 He who believes in him is not judged. He who doesn’t believe has been judged already, because he has not believed in the name of the only born Son of God.",
        "19 This is the judgment, that the light has come into the world, and men loved the darkness rather than the light, for their works were evil.",
        "20 For everyone who does evil hates the light and doesn’t come to the light, lest his works would be exposed.",
        "21 But he who does the truth comes to the light, that his works may be revealed, that they have been done in God.”",
        "22 After these things, Jesus came with his disciples into the land of Judea. He stayed there with them and baptized.",
        "23 John also was baptizing in Enon near Salim, because there was much water there. They came and were baptized;",
        "24 for John was not yet thrown into prison.",
        "25 Therefore a dispute arose on the part of John’s disciples with some Jews about purification.",
        "26 They came to John and said to him, “Rabbi, he who was with you beyond the Jordan, to whom you have testified, behold, he baptizes, and everyone is coming to him.”",
        "27 John answered, “A man can receive nothing unless it has been given him from heaven.",
        "28 You yourselves testify that I said, ‘I am not the Christ,’ but, ‘I have been sent before him.’",
        "29 He who has the bride is the bridegroom; but the friend of the bridegroom, who stands and hears him, rejoices greatly because of the bridegroom’s voice. Therefore my joy is made full.",
        "30 He must increase, but I must decrease.",
        "31 “He who comes from above is above all. He who is from the earth belongs to the earth and speaks of the earth. He who comes from heaven is above all.",
        "32 What he has seen and heard, of that he testifies; and no one receives his witness.",
        "33 He who has received his witness has set his seal to this, that God is true.",
        "34 For he whom God has sent speaks the words of God; for God gives the Spirit without measure.",
        "35 The Father loves the Son, and has given all things into his hand.",
        "36 One who believes in the Son has eternal life, but one who disobeys the Son won’t see life, but the wrath of God remains on him.”",
    ).joinToString("\n", postfix = "\n")

    val jcJohnThreeFrom16 = listOf(
        "16 神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。",
        "17 神が御子を世につかわされたのは、世をさばくためではなく、御子によって、この世が救われるためである。",
        "18 彼を信じる者は、さばかれない。信じない者は、すでにさばかれている。神のひとり子の名を信じることをしないからである。",
        "19 そのさばきというのは、光がこの世にきたのに、人々はそのおこないが悪いために、光よりもやみの方を愛したことである。",
        "20 悪を行っている者はみな光を憎む。そして、そのおこないが明るみに出されるのを恐れて、光にこようとはしない。",
        "21 しかし、真理を行っている者は光に来る。その人のおこないの、神にあってなされたということが、明らかにされるためである。",
        "22 こののち、イエスは弟子たちとユダヤの地に行き、彼らと一緒にそこに滞在して、バプテスマを授けておられた。",
        "23 ヨハネもサリムに近いアイノンで、バプテスマを授けていた。そこには水がたくさんあったからである。人々がぞくぞくとやってきてバプテスマを受けていた。",
        "24 そのとき、ヨハネはまだ獄に入れられてはいなかった。",
        "25 ところが、ヨハネの弟子たちとひとりのユダヤ人との間に、きよめのことで争論が起った。",
        "26 そこで彼らはヨハネのところにきて言った、「先生、ごらん下さい。ヨルダンの向こうであなたと一緒にいたことがあり、そして、あなたがあかしをしておられたあのかたが、バプテスマを授けており、皆の者が、そのかたのところへ出かけています」。",
        "27 ヨハネは答えて言った、「人は天から与えられなければ、何ものも受けることはできない。",
        "28 『わたしはキリストではなく、そのかたよりも先につかわされた者である』と言ったことをあかししてくれるのは、あなたがた自身である。",
        "29 花嫁をもつ者は花婿である。花婿の友人は立って彼の声を聞き、その声を聞いて大いに喜ぶ。こうして、この喜びはわたしに満ち足りている。",
        "30 彼は必ず栄え、わたしは衰える。",
        "31 上から来る者は、すべてのものの上にある。地から出る者は、地に属する者であって、地のことを語る。天から来る者は、すべてのものの上にある。",
        "32 彼はその見たところ、聞いたところをあかししているが、だれもそのあかしを受けいれない。",
        "33 しかし、そのあかしを受けいれる者は、神がまことであることを、たしかに認めたのである。",
        "34 神がおつかわしになったかたは、神の言葉を語る。神は聖霊を限りなく賜うからである。",
        "35 父は御子を愛して、万物をその手にお与えになった。",
        "36 御子を信じる者は永遠の命をもつ。御子に従わない者は、命にあずかることがないばかりか、神の怒りがその上にとどまるのである」。",
    ).joinToString("\n", postfix = "\n")

    val kjvJohnThreeFrom16 = listOf(
        "16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
        "17 For God sent not his Son into the world to condemn the world; but that the world through him might be saved.",
        "18 He that believeth on him is not condemned: but he that believeth not is condemned already, because he hath not believed in the name of the only begotten Son of God.",
        "19 And this is the condemnation, that light is come into the world, and men loved darkness rather than light, because their deeds were evil.",
        "20 For every one that doeth evil hateth the light, neither cometh to the light, lest his deeds should be reproved.",
        "21 But he that doeth truth cometh to the light, that his deeds may be made manifest, that they are wrought in God.",
        "22 After these things came Jesus and his disciples into the land of Judaea; and there he tarried with them, and baptized.",
        "23 And John also was baptizing in Aenon near to Salim, because there was much water there: and they came, and were baptized.",
        "24 For John was not yet cast into prison.",
        "25 Then there arose a question between [some] of John's disciples and the Jews about purifying.",
        "26 And they came unto John, and said unto him, Rabbi, he that was with thee beyond Jordan, to whom thou barest witness, behold, the same baptizeth, and all [men] come to him.",
        "27 John answered and said, A man can receive nothing, except it be given him from heaven.",
        "28 Ye yourselves bear me witness, that I said, I am not the Christ, but that I am sent before him.",
        "29 He that hath the bride is the bridegroom: but the friend of the bridegroom, which standeth and heareth him, rejoiceth greatly because of the bridegroom's voice: this my joy therefore is fulfilled.",
        "30 He must increase, but I [must] decrease.",
        "31 He that cometh from above is above all: he that is of the earth is earthly, and speaketh of the earth: he that cometh from heaven is above all.",
        "32 And what he hath seen and heard, that he testifieth; and no man receiveth his testimony.",
        "33 He that hath received his testimony hath set to his seal that God is true.",
        "34 For he whom God hath sent speaketh the words of God: for God giveth not the Spirit by measure [unto him].",
        "35 The Father loveth the Son, and hath given all things into his hand.",
        "36 He that believeth on the Son hath everlasting life: and he that believeth not the Son shall not see life; but the wrath of God abideth on him.",
    ).joinToString("\n", postfix = "\n")

    val genesisOneWebus = WEBUS_GENESIS_1_1

    val genesisTowWebus = "1 The heavens, the earth, and all their vast array were finished."

    val johnThreeWebus = buildString {
        (1..15).forEach { appendLine("$it verse text") }
        append(webusJohnThreeFrom16)
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
        append(jcJohnThreeFrom16)
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
    private val kjvManifestJson = SupportedTranslation.KJV.translation.toJson()
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

    val kjvMinimalZipBytes: ByteArray = ZipUtil.buildMinimalZip(
        listOf(
            "kjv.43.3.txt" to buildString {
                (1..15).forEach { appendLine("$it verse text") }
                append(kjvJohnThreeFrom16)
            },
            "kjv$MANIFEST_JSON_POSTFIX" to kjvManifestJson,
            "index/kjv.index.manifest" to "_0.cfs",
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
        "kjv" -> kjvMinimalZipBytes
        "ubg" -> minimalZip(code, ubgManifestJson)
        "ubio" -> minimalZip(code, ubioManifestJson)
        else -> error("Unexpected translation code: $code")
    }

    fun bblInstallMockEngine() = MockEngine { request ->
        val releaseVersion = BblVersion.VERSION
        val path = request.url.encodedPath
        val primaryReleasePath = "/${BblVersion.BBL_REPOSITORY}/releases/download/$releaseVersion/"
        val legacyReleasePath = "/${BblVersion.BBL_REPOSITORY_LEGACY}/releases/download/$releaseVersion/"
        val helperName = path.substringAfterLast('/')
        val helperBytes = "$helperName helper".encodeToByteArray()
        when {
            (path.startsWith(primaryReleasePath) || path.startsWith(legacyReleasePath)) &&
                path.endsWith(".zip") -> {
                val code = path.substringAfterLast('/').removeSuffix(".zip")
                respond(
                    content = packBytes(code),
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/zip")
                )
            }
            path.startsWith(primaryReleasePath) || path.startsWith(legacyReleasePath) -> respond(
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
