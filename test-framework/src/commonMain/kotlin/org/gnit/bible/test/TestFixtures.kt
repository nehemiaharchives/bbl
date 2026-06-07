package org.gnit.bible.test

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.BblVersion

object TestFixtures {

    val tmpWorkingDirForBblPack = "/tmp/bblpack-cli-create"

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

    // webus chapters
    val genesisOneWebus = """
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

    val genesisTowWebus = """
    1 The heavens, the earth, and all their vast array were finished.
    2 On the seventh day God finished his work which he had done; and he rested on the seventh day from all his work which he had done.
    3 God blessed the seventh day, and made it holy, because he rested in it from all his work of creation which he had done.
    4 This is the history of the generations of the heavens and of the earth when they were created, in the day that Yahweh God made the earth and the heavens.
    5 No plant of the field was yet in the earth, and no herb of the field had yet sprung up; for Yahweh God had not caused it to rain on the earth. There was not a man to till the ground,
    6 but a mist went up from the earth, and watered the whole surface of the ground.
    7 Yahweh God formed man from the dust of the ground, and breathed into his nostrils the breath of life; and man became a living soul.
    8 Yahweh God planted a garden eastward, in Eden, and there he put the man whom he had formed.
    9 Out of the ground Yahweh God made every tree to grow that is pleasant to the sight, and good for food, including the tree of life in the middle of the garden and the tree of the knowledge of good and evil.
    10 A river went out of Eden to water the garden; and from there it was parted, and became the source of four rivers.
    11 The name of the first is Pishon: it flows through the whole land of Havilah, where there is gold;
    12 and the gold of that land is good. Bdellium and onyx stone are also there.
    13 The name of the second river is Gihon. It is the same river that flows through the whole land of Cush.
    14 The name of the third river is Hiddekel. This is the one which flows in front of Assyria. The fourth river is the Euphrates.
    15 Yahweh God took the man, and put him into the garden of Eden to cultivate and keep it.
    16 Yahweh God commanded the man, saying, “You may freely eat of every tree of the garden;
    17 but you shall not eat of the tree of the knowledge of good and evil; for in the day that you eat of it, you will surely die.”
    18 Yahweh God said, “It is not good for the man to be alone. I will make him a helper comparable to him.”
    19 Out of the ground Yahweh God formed every animal of the field, and every bird of the sky, and brought them to the man to see what he would call them. Whatever the man called every living creature became its name.
    20 The man gave names to all livestock, and to the birds of the sky, and to every animal of the field; but for man there was not found a helper comparable to him.
    21 Yahweh God caused the man to fall into a deep sleep. As the man slept, he took one of his ribs, and closed up the flesh in its place.
    22 Yahweh God made a woman from the rib which he had taken from the man, and brought her to the man.
    23 The man said, “This is now bone of my bones, and flesh of my flesh. She will be called ‘woman,’ because she was taken out of Man.”
    24 Therefore a man will leave his father and his mother, and will join with his wife, and they will be one flesh.
    25 The man and his wife were both naked, and they were not ashamed.
    """.trimIndent()

    val matthew28Webus = """
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

    val john3Webus = """
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

    // jc chapters
    val genesisOneJc = """
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

    val matthew28Jc = """
    1 さて、安息日が終って、週の初めの日の明け方に、マグダラのマリヤとほかのマリヤとが、墓を見にきた。
    2 すると、大きな地震が起った。それは主の使が天から下って、そこにきて石をわきへころがし、その上にすわったからである。
    3 その姿はいなずまのように輝き、その衣は雪のように真白であった。
    4 見張りをしていた人たちは、恐ろしさの余り震えあがって、死人のようになった。
    5 この御使は女たちにむかって言った、「恐れることはない。あなたがたが十字架におかかりになったイエスを捜していることは、わたしにわかっているが、
    6 もうここにはおられない。かねて言われたとおりに、よみがえられたのである。さあ、イエスが納められていた場所をごらんなさい。
    7 そして、急いで行って、弟子たちにこう伝えなさい、『イエスは死人の中からよみがえられた。見よ、あなたがたより先にガリラヤへ行かれる。そこでお会いできるであろう』。あなたがたに、これだけ言っておく」。
    8 そこで女たちは恐れながらも大喜びで、急いで墓を立ち去り、弟子たちに知らせるために走って行った。
    9 すると、イエスは彼らに出会って、「平安あれ」と言われたので、彼らは近寄りイエスのみ足をいだいて拝した。
    10 そのとき、イエスは彼らに言われた、「恐れることはない。行って兄弟たちに、ガリラヤに行け、そこでわたしに会えるであろう、と告げなさい」。
    11 女たちが行っている間に、番人のうちのある人々が都に帰って、いっさいの出来事を祭司長たちに話した。
    12 祭司長たちは長老たちと集まって協議をこらし、兵卒たちにたくさんの金を与えて言った、
    13 「『弟子たちが夜中にきて、われわれの寝ている間に彼を盗んだ』と言え。
    14 万一このことが総督の耳にはいっても、われわれが総督に説いて、あなたがたに迷惑が掛からないようにしよう」。
    15 そこで、彼らは金を受け取って、教えられたとおりにした。そしてこの話は、今日に至るまでユダヤ人の間にひろまっている。
    16 さて、十一人の弟子たちはガリラヤに行って、イエスが彼らに行くように命じられた山に登った。
    17 そして、イエスに会って拝した。しかし、疑う者もいた。
    18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。
    19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、
    20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。
    """.trimIndent()

    val john3Jc = """
    1 パリサイ人のひとりで、その名をニコデモというユダヤ人の指導者があった。
    2 この人が夜イエスのもとにきて言った、「先生、わたしたちはあなたが神からこられた教師であることを知っています。神がご一緒でないなら、あなたがなさっておられるようなしるしは、だれにもできはしません」。
    3 イエスは答えて言われた、「よくよくあなたに言っておく。だれでも新しく生れなければ、神の国を見ることはできない」。
    4 ニコデモは言った、「人は年をとってから生れることが、どうしてできますか。もう一度、母の胎にはいって生れることができましょうか」。
    5 イエスは答えられた、「よくよくあなたに言っておく。だれでも、水と霊とから生れなければ、神の国にはいることはできない。
    6 肉から生れる者は肉であり、霊から生れる者は霊である。
    7 あなたがたは新しく生れなければならないと、わたしが言ったからとて、不思議に思うには及ばない。
    8 風は思いのままに吹く。あなたはその音を聞くが、それがどこからきて、どこへ行くかは知らない。霊から生れる者もみな、それと同じである」。
    9 ニコデモはイエスに答えて言った、「どうして、そんなことがあり得ましょうか」。
    10 イエスは彼に答えて言われた、「あなたはイスラエルの教師でありながら、これぐらいのことがわからないのか。
    11 よくよく言っておく。わたしたちは自分の知っていることを語り、また自分の見たことをあかししているのに、あなたがたはわたしたちのあかしを受けいれない。
    12 わたしが地上のことを語っているのに、あなたがたが信じないならば、天上のことを語った場合、どうしてそれを信じるだろうか。
    13 天から下ってきた者、すなわち人の子のほかには、だれも天に上った者はない。
    14 そして、ちょうどモーセが荒野でへびを上げたように、人の子もまた上げられなければならない。
    15 それは彼を信じる者が、すべて永遠の命を得るためである」。
    16 神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。
    17 神が御子を世につかわされたのは、世をさばくためではなく、御子によって、この世が救われるためである。
    18 彼を信じる者は、さばかれない。信じない者は、すでにさばかれている。神のひとり子の名を信じることをしないからである。
    19 そのさばきというのは、光がこの世にきたのに、人々はそのおこないが悪いために、光よりもやみの方を愛したことである。
    20 悪を行っている者はみな光を憎む。そして、そのおこないが明るみに出されるのを恐れて、光にこようとはしない。
    21 しかし、真理を行っている者は光に来る。その人のおこないの、神にあってなされたということが、明らかにされるためである。
    22 こののち、イエスは弟子たちとユダヤの地に行き、彼らと一緒にそこに滞在して、バプテスマを授けておられた。
    23 ヨハネもサリムに近いアイノンで、バプテスマを授けていた。そこには水がたくさんあったからである。人々がぞくぞくとやってきてバプテスマを受けていた。
    24 そのとき、ヨハネはまだ獄に入れられてはいなかった。
    25 ところが、ヨハネの弟子たちとひとりのユダヤ人との間に、きよめのことで争論が起った。
    26 そこで彼らはヨハネのところにきて言った、「先生、ごらん下さい。ヨルダンの向こうであなたと一緒にいたことがあり、そして、あなたがあかしをしておられたあのかたが、バプテスマを授けており、皆の者が、そのかたのところへ出かけています」。
    27 ヨハネは答えて言った、「人は天から与えられなければ、何ものも受けることはできない。
    28 『わたしはキリストではなく、そのかたよりも先につかわされた者である』と言ったことをあかししてくれるのは、あなたがた自身である。
    29 花嫁をもつ者は花婿である。花婿の友人は立って彼の声を聞き、その声を聞いて大いに喜ぶ。こうして、この喜びはわたしに満ち足りている。
    30 彼は必ず栄え、わたしは衰える。
    31 上から来る者は、すべてのものの上にある。地から出る者は、地に属する者であって、地のことを語る。天から来る者は、すべてのものの上にある。
    32 彼はその見たところ、聞いたところをあかししているが、だれもそのあかしを受けいれない。
    33 しかし、そのあかしを受けいれる者は、神がまことであることを、たしかに認めたのである。
    34 神がおつかわしになったかたは、神の言葉を語る。神は聖霊を限りなく賜うからである。
    35 父は御子を愛して、万物をその手にお与えになった。
    36 御子を信じる者は永遠の命をもつ。御子に従わない者は、命にあずかることがないばかりか、神の怒りがその上にとどまるのである」。
    """.trimIndent()

    // kttv chapters
    private val genesis1Kttv = """
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
    private val ubgManifestJson = Translation.ubg.toJson()
    private val ubioManifestJson = Translation.ubio.toJson()

    val kttvDownloadingMockEngine = MockEngine { _ ->
        val bytes = ZipUtil.buildMinimalZip(
            listOf(
                "kttv.1.1.txt" to genesis1Kttv,
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
            "webus.1.1.txt" to genesisOneWebus,
            "webus.1.2.txt" to genesisTowWebus,
            "webus.43.3.txt" to john3Webus,
            "webus.40.28.txt" to matthew28Webus,
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

    val bblInstallMockEngine = MockEngine {
            request ->
        fun packBytes(translationCode: String): ByteArray =
            when (translationCode) {
                "kttv" -> ZipUtil.buildMinimalZip(
                    listOf(
                        "kttv.1.1.txt" to genesis1Kttv,
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

                "ubg" -> ZipUtil.buildMinimalZip(
                    listOf(
                        "ubg.1.1.txt" to "1 test",
                        "ubg${MANIFEST_JSON_POSTFIX}" to ubgManifestJson
                    )
                )

                "ubio" -> ZipUtil.buildMinimalZip(
                    listOf(
                        "ubio.1.1.txt" to "1 test",
                        "ubio${MANIFEST_JSON_POSTFIX}" to ubioManifestJson
                    )
                )

                else -> error("Unexpected translation code: $translationCode")
            }

        when(request.url.encodedPath){
            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/kttv.zip" -> {
                val bytes = packBytes("kttv")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/th1971.zip" -> {
                val bytes = packBytes("th1971")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/webus.zip" -> {
                val bytes = packBytes("webus")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/jc.zip" -> {
                val bytes = packBytes("jc")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/ubg.zip" -> {
                val bytes = packBytes("ubg")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/${BblVersion.version}/bbl/resources/bblpacks/ubio.zip" -> {
                val bytes = packBytes("ubio")
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-kuromoji",
            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-kuromoji.exe" -> {
                val bytes = "kuromoji helper".encodeToByteArray()
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/octet-stream"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-extra",
            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-extra.exe" -> {
                val bytes = "extra helper".encodeToByteArray()
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/octet-stream"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-morfologik",
            "/nehemiaharchives/bbl/releases/download/${BblVersion.version}/bbl-search-morfologik.exe" -> {
                val bytes = "morfologik helper".encodeToByteArray()
                respond(
                    content = bytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/octet-stream"),
                        "Content-Length" to listOf(bytes.size.toString())
                    )
                )
            }

            else -> error("Unexpected request for path: ${request.url.encodedPath}")
        }
    }
}
