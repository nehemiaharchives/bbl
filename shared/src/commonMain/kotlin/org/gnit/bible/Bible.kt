package org.gnit.bible

class Bible(val assetManager: AssetManager = AssetManagerImpl()) {

    companion object{

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
    }

    fun availableTranslationCodes(): Array<String> {
        return embeddedTranslationCodes.plus(assetManager.downloadedTranslationCodes())
    }

    fun availableTranslations(): List<Translation> {
        val embeddedTranslations = Translation.embeddedTranslations
        val downloadedTranslations = assetManager.downloadedTranslationCodes().map { code ->
            obtainZipBibleTextReader().getTranslationFromManifest(code)
        }
        return embeddedTranslations.plus(downloadedTranslations)
    }

    lateinit var bibleTextReader: BibleTextReader

    var zipBibleTextReader: ZipBibleTextReader? = null

    fun obtainZipBibleTextReader(): ZipBibleTextReader {
        if (zipBibleTextReader == null) {
            zipBibleTextReader = ZipBibleTextReader(assetManager.platform)
        }
        return zipBibleTextReader!!
    }

    fun verses(translation: String = "webus", book: Int = 1, chapter: Int = 1): String {
        return when{
            translation == "webus" && book == 1 && chapter == 1 -> webusGenesisChapterOne
            translation == "jc" && book == 1 && chapter == 1 -> jcGenesisChapterOne
            embeddedTranslationCodes.contains(translation) -> bibleTextReader.getChapterText(translation = "webus", book = book, chapter = chapter)
            assetManager.downloadedTranslationCodes().contains(translation) -> obtainZipBibleTextReader().getChapterText(translation = translation, book = book, chapter = chapter)
            else -> error("Translation '$translation' not found. Available translations: ${availableTranslationCodes().joinToString(", ")}")
        }
    }
}
