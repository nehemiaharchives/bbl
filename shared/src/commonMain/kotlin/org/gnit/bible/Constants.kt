package org.gnit.bible

const val SERVER_PORT = 8081
const val DOWNLOADABLE_BIBLE_LIST_URL = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bbllist.json"
const val DOWNLOADABLE_BIBLE_BASE_URL = "https://raw.githubusercontent.com/nehemiaharchives/bbl-kmp/refs/heads/master/server/src/main/resources/files/bblpacks"
const val MANIFEST_JSON_POSTFIX = ".0.manifest.json"

const val SETTINGS_FILE_NAME = "config.properties"

fun hasEmbeddedTranslation(translationCode: String, readerInitialized: Boolean): Boolean {
    return readerInitialized && embeddedTranslationCodes.contains(translationCode)
}

val embeddedTranslationCodes = arrayOf(
    "cunp",
    "delut",
    "jc",
    "kjv",
    "krv",
    "lsg",
    "rdv24",
    "rvr09",
    "sinod",
    "sven",
    "svrj",
    "tb",
    "ubg",
    "ubio",
    "webus",
)

val downloadableTranslationsCli = listOf(
    // search common
    Translation.ayt,
    Translation.th1971,
    Translation.irvhin,
    Translation.irvben,
    Translation.irvtam,
    Translation.npiulb,

    // search extra
    Translation.abtag,
    Translation.kttv,
    Translation.irvguj,
    Translation.irvmar,
    Translation.irvtel,
    Translation.irvurd
)

val downloadableTranslationsCmp = Translation.embeddedTranslations.plus(downloadableTranslationsCli)

val downloadableTranslationCodeListCli = downloadableTranslationsCli.map { it.code }

val webusGenesisChapterOne = """
            1 In the beginning, God created the heavens and the earth.
            2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.
            3 God said, “Let there be light,” and there was light.
            4 God saw the light, and saw that it was good. God divided the light from the darkness.
            5 God called the light “day”, and the darkness he called “night”. There was evening and there was morning, the first day.
            6 God said, “Let there be an expanse in the middle of the waters, and let it divide the waters from the waters.”
            7 God made the expanse, and divided the waters which were under the expanse from the waters which were above the expanse; and it was so.
            8 God called the expanse “sky”. There was evening and there was morning, a second day.
            9 God said, “Let the waters under the sky be gathered together to one place, and let the dry land appear;” and it was so.
            10 God called the dry land “earth”, and the gathering together of the waters he called “seas”. God saw that it was good.
            11 God said, “Let the earth yield grass, herbs yielding seeds, and fruit trees bearing fruit after their kind, with their seeds in it, on the earth;” and it was so.
            12 The earth yielded grass, herbs yielding seed after their kind, and trees bearing fruit, with their seeds in it, after their kind; and God saw that it was good.
            13 There was evening and there was morning, a third day.
            14 God said, “Let there be lights in the expanse of the sky to divide the day from the night; and let them be for signs to mark seasons, days, and years;
            15 and let them be for lights in the expanse of the sky to give light on the earth;” and it was so.
            16 God made the two great lights: the greater light to rule the day, and the lesser light to rule the night. He also made the stars.
            17 God set them in the expanse of the sky to give light to the earth,
            18 and to rule over the day and over the night, and to divide the light from the darkness. God saw that it was good.
            19 There was evening and there was morning, a fourth day.
            20 God said, “Let the waters abound with living creatures, and let birds fly above the earth in the open expanse of the sky.”
            21 God created the large sea creatures and every living creature that moves, with which the waters swarmed, after their kind, and every winged bird after its kind. God saw that it was good.
            22 God blessed them, saying, “Be fruitful, and multiply, and fill the waters in the seas, and let birds multiply on the earth.”
            23 There was evening and there was morning, a fifth day.
            24 God said, “Let the earth produce living creatures after their kind, livestock, creeping things, and animals of the earth after their kind;” and it was so.
            25 God made the animals of the earth after their kind, and the livestock after their kind, and everything that creeps on the ground after its kind. God saw that it was good.
            26 God said, “Let’s make man in our image, after our likeness. Let them have dominion over the fish of the sea, and over the birds of the sky, and over the livestock, and over all the earth, and over every creeping thing that creeps on the earth.”
            27 God created man in his own image. In God’s image he created him; male and female he created them.
            28 God blessed them. God said to them, “Be fruitful, multiply, fill the earth, and subdue it. Have dominion over the fish of the sea, over the birds of the sky, and over every living thing that moves on the earth.”
            29 God said, “Behold, I have given you every herb yielding seed, which is on the surface of all the earth, and every tree, which bears fruit yielding seed. It will be your food.
            30 To every animal of the earth, and to every bird of the sky, and to everything that creeps on the earth, in which there is life, I have given every green herb for food;” and it was so.
            31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.
        """.trimIndent()

val webusJohnChapter3 = """
            1 Now there was a man of the Pharisees named Nicodemus, a ruler of the Jews.
            2 He came to Jesus by night and said to him, “Rabbi, we know that you are a teacher come from God, for no one can do these signs that you do, unless God is with him.”
            3 Jesus answered him, “Most certainly I tell you, unless one is born anew,  he can’t see God’s Kingdom.”
            4 Nicodemus said to him, “How can a man be born when he is old? Can he enter a second time into his mother’s womb and be born?”
            5 Jesus answered, “Most certainly I tell you, unless one is born of water and Spirit, he can’t enter into God’s Kingdom.
            6 That which is born of the flesh is flesh. That which is born of the Spirit is spirit.
            7 Don’t marvel that I said to you, ‘You must be born anew.’
            8 The wind  blows where it wants to, and you hear its sound, but don’t know where it comes from and where it is going. So is everyone who is born of the Spirit.”
            9 Nicodemus answered him, “How can these things be?”
            10 Jesus answered him, “Are you the teacher of Israel, and don’t understand these things?
            11 Most certainly I tell you, we speak that which we know and testify of that which we have seen, and you don’t receive our witness.
            12 If I told you earthly things and you don’t believe, how will you believe if I tell you heavenly things?
            13 No one has ascended into heaven but he who descended out of heaven, the Son of Man, who is in heaven.
            14 As Moses lifted up the serpent in the wilderness, even so must the Son of Man be lifted up,
            15 that whoever believes in him should not perish, but have eternal life.
            16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.
            17 For God didn’t send his Son into the world to judge the world, but that the world should be saved through him.
            18 He who believes in him is not judged. He who doesn’t believe has been judged already, because he has not believed in the name of the only born Son of God.
            19 This is the judgment, that the light has come into the world, and men loved the darkness rather than the light, for their works were evil.
            20 For everyone who does evil hates the light and doesn’t come to the light, lest his works would be exposed.
            21 But he who does the truth comes to the light, that his works may be revealed, that they have been done in God.”
            22 After these things, Jesus came with his disciples into the land of Judea. He stayed there with them and baptized.
            23 John also was baptizing in Enon near Salim, because there was much water there. They came and were baptized;
            24 for John was not yet thrown into prison.
            25 Therefore a dispute arose on the part of John’s disciples with some Jews about purification.
            26 They came to John and said to him, “Rabbi, he who was with you beyond the Jordan, to whom you have testified, behold, he baptizes, and everyone is coming to him.”
            27 John answered, “A man can receive nothing unless it has been given him from heaven.
            28 You yourselves testify that I said, ‘I am not the Christ,’ but, ‘I have been sent before him.’
            29 He who has the bride is the bridegroom; but the friend of the bridegroom, who stands and hears him, rejoices greatly because of the bridegroom’s voice. Therefore my joy is made full.
            30 He must increase, but I must decrease.
            31 “He who comes from above is above all. He who is from the earth belongs to the earth and speaks of the earth. He who comes from heaven is above all.
            32 What he has seen and heard, of that he testifies; and no one receives his witness.
            33 He who has received his witness has set his seal to this, that God is true.
            34 For he whom God has sent speaks the words of God; for God gives the Spirit without measure.
            35 The Father loves the Son, and has given all things into his hand.
            36 One who believes in the Son has eternal life, but one who disobeys the Son won’t see life, but the wrath of God remains on him.”
""".trimIndent()

val webusMatthewChapter28 = """
            1 Now after the Sabbath, as it began to dawn on the first day of the week, Mary Magdalene and the other Mary came to see the tomb.
            2 Behold, there was a great earthquake, for an angel of the Lord descended from the sky and came and rolled away the stone from the door and sat on it.
            3 His appearance was like lightning, and his clothing white as snow.
            4 For fear of him, the guards shook, and became like dead men.
            5 The angel answered the women, “Don’t be afraid, for I know that you seek Jesus, who has been crucified.
            6 He is not here, for he has risen, just like he said. Come, see the place where the Lord was lying.
            7 Go quickly and tell his disciples, ‘He has risen from the dead, and behold, he goes before you into Galilee; there you will see him.’ Behold, I have told you.”
            8 They departed quickly from the tomb with fear and great joy, and ran to bring his disciples word.
            9 As they went to tell his disciples, behold, Jesus met them, saying, “Rejoice!”
             They came and took hold of his feet, and worshiped him.
            10 Then Jesus said to them, “Don’t be afraid. Go tell my brothers  that they should go into Galilee, and there they will see me.”
            11 Now while they were going, behold, some of the guards came into the city and told the chief priests all the things that had happened.
            12 When they were assembled with the elders and had taken counsel, they gave a large amount of silver to the soldiers,
            13 saying, “Say that his disciples came by night and stole him away while we slept.
            14 If this comes to the governor’s ears, we will persuade him and make you free of worry.”
            15 So they took the money and did as they were told. This saying was spread abroad among the Jews, and continues until today.
            16 But the eleven disciples went into Galilee, to the mountain where Jesus had sent them.
            17 When they saw him, they bowed down to him; but some doubted.
            18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.
            19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,
            20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
""".trimIndent()

val jcGenesisChapterOne = """
            1 はじめに神は天と地とを創造された。
            2 地は形なく、むなしく、やみが淵のおもてにあり、神の霊が水のおもてをおおっていた。
            3 神は「光あれ」と言われた。すると光があった。
            4 神はその光を見て、良しとされた。神はその光とやみとを分けられた。
            5 神は光を昼と名づけ、やみを夜と名づけられた。夕となり、また朝となった。第一日である。
            6 神はまた言われた、「水の間におおぞらがあって、水と水とを分けよ」。
            7 そのようになった。神はおおぞらを造って、おおぞらの下の水とおおぞらの上の水とを分けられた。
            8 神はそのおおぞらを天と名づけられた。夕となり、また朝となった。第二日である。
            9 神はまた言われた、「天の下の水は一つ所に集まり、かわいた地が現れよ」。そのようになった。
            10 神はそのかわいた地を陸と名づけ、水の集まった所を海と名づけられた。神は見て、良しとされた。
            11 神はまた言われた、「地は青草と、種をもつ草と、種類にしたがって種のある実を結ぶ果樹とを地の上にはえさせよ」。そのようになった。
            12 地は青草と、種類にしたがって種をもつ草と、種類にしたがって種のある実を結ぶ木とをはえさせた。神は見て、良しとされた。
            13 夕となり、また朝となった。第三日である。
            14 神はまた言われた、「天のおおぞらに光があって昼と夜とを分け、しるしのため、季節のため、日のため、年のためになり、
            15 天のおおぞらにあって地を照らす光となれ」。そのようになった。
            16 神は二つの大きな光を造り、大きい光に昼をつかさどらせ、小さい光に夜をつかさどらせ、また星を造られた。
            17 神はこれらを天のおおぞらに置いて地を照らさせ、
            18 昼と夜とをつかさどらせ、光とやみとを分けさせられた。神は見て、良しとされた。
            19 夕となり、また朝となった。第四日である。
            20 神はまた言われた、「水は生き物の群れで満ち、鳥は地の上、天のおおぞらを飛べ」。
            21 神は海の大いなる獣と、水に群がるすべての動く生き物とを、種類にしたがって創造し、また翼のあるすべての鳥を、種類にしたがって創造された。神は見て、良しとされた。
            22 神はこれらを祝福して言われた、「生めよ、ふえよ、海の水に満ちよ、また鳥は地にふえよ」。
            23 夕となり、また朝となった。第五日である。
            24 神はまた言われた、「地は生き物を種類にしたがっていだせ。家畜と、這うものと、地の獣とを種類にしたがっていだせ」。そのようになった。
            25 神は地の獣を種類にしたがい、家畜を種類にしたがい、また地に這うすべての物を種類にしたがって造られた。神は見て、良しとされた。
            26 神はまた言われた、「われわれのかたちに、われわれにかたどって人を造り、これに海の魚と、空の鳥と、家畜と、地のすべての獣と、地のすべての這うものとを治めさせよう」。
            27 神は自分のかたちに人を創造された。すなわち、神のかたちに創造し、男と女とに創造された。
            28 神は彼らを祝福して言われた、「生めよ、ふえよ、地に満ちよ、地を従わせよ。また海の魚と、空の鳥と、地に動くすべての生き物とを治めよ」。
            29 神はまた言われた、「わたしは全地のおもてにある種をもつすべての草と、種のある実を結ぶすべての木とをあなたがたに与える。これはあなたがたの食物となるであろう。
            30 また地のすべての獣、空のすべての鳥、地を這うすべてのもの、すなわち命あるものには、食物としてすべての青草を与える」。そのようになった。
            31 神が造ったすべての物を見られたところ、それは、はなはだ良かった。夕となり、また朝となった。第六日である。
        """.trimIndent()

