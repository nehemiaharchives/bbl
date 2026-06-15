describe 'bbl binary' do
  subject { file($bbl_bin) }
  it { should exist }
  it { should be_file }
  it 'is executable' do
    skip 'N/A on Windows' if $bbl_windows
    expect(subject).to be_executable
  end
end

describe 'bbl version file' do
  subject { file($bbl_version_file) }
  it { should exist }
  it { should be_file }
  its('content') { should match(/\Av\d+\.\d+\s*\z/) }
end

describe 'bbl Linux install home' do
  it 'uses the ubuntu home directory' do
    skip 'N/A outside Linux' if $bbl_windows || $bbl_macos

    expect($bbl_home_dir).to eq('/home/ubuntu')
    expect($bbl_pack_dir).to eq('/home/ubuntu/.bbl/packs')
    expect(file('/home/ubuntu/.bbl')).to be_directory
  end
end

describe 'bbl -v' do
  subject(:cmd) { command($bbl_run.call('-v')) }
  its('stdout') { should include("bbl version #{$bbl_expected_version}") }
end

$bbl_normalized_stdout = ->(cmd) { cmd.stdout.gsub("\r\n", "\n").force_encoding('UTF-8') }

describe 'bbl' do
  subject(:cmd) { command($bbl_run.call('')) }

  it 'prints Genesis 1:1 in WEBUS by default' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      [
        "1 In the beginning, God created the heavens and the earth.",
        "2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.",
        "3 God said, “Let there be light,” and there was light.",
        "4 God saw the light, and saw that it was good. God divided the light from the darkness.",
        "5 God called the light “day”, and the darkness he called “night”. There was evening and there was morning, the first day.",
        "6 God said, “Let there be an expanse in the middle of the waters, and let it divide the waters from the waters.”",
        "7 God made the expanse, and divided the waters which were under the expanse from the waters which were above the expanse; and it was so.",
        "8 God called the expanse “sky”. There was evening and there was morning, a second day.",
        "9 God said, “Let the waters under the sky be gathered together to one place, and let the dry land appear;” and it was so.",
        "10 God called the dry land “earth”, and the gathering together of the waters he called “seas”. God saw that it was good.",
        "11 God said, “Let the earth yield grass, herbs yielding seeds, and fruit trees bearing fruit after their kind, with their seeds in it, on the earth;” and it was so.",
        "12 The earth yielded grass, herbs yielding seed after their kind, and trees bearing fruit, with their seeds in it, after their kind; and God saw that it was good.",
        "13 There was evening and there was morning, a third day.",
        "14 God said, “Let there be lights in the expanse of the sky to divide the day from the night; and let them be for signs to mark seasons, days, and years;",
        "15 and let them be for lights in the expanse of the sky to give light on the earth;” and it was so.",
        "16 God made the two great lights: the greater light to rule the day, and the lesser light to rule the night. He also made the stars.",
        "17 God set them in the expanse of the sky to give light to the earth,",
        "18 and to rule over the day and over the night, and to divide the light from the darkness. God saw that it was good.",
        "19 There was evening and there was morning, a fourth day.",
        "20 God said, “Let the waters abound with living creatures, and let birds fly above the earth in the open expanse of the sky.”",
        "21 God created the large sea creatures and every living creature that moves, with which the waters swarmed, after their kind, and every winged bird after its kind. God saw that it was good.",
        "22 God blessed them, saying, “Be fruitful, and multiply, and fill the waters in the seas, and let birds multiply on the earth.”",
        "23 There was evening and there was morning, a fifth day.",
        "24 God said, “Let the earth produce living creatures after their kind, livestock, creeping things, and animals of the earth after their kind;” and it was so.",
        "25 God made the animals of the earth after their kind, and the livestock after their kind, and everything that creeps on the ground after its kind. God saw that it was good.",
        "26 God said, “Let’s make man in our image, after our likeness. Let them have dominion over the fish of the sea, and over the birds of the sky, and over the livestock, and over all the earth, and over every creeping thing that creeps on the earth.”",
        "27 God created man in his own image. In God’s image he created him; male and female he created them.",
        "28 God blessed them. God said to them, “Be fruitful, multiply, fill the earth, and subdue it. Have dominion over the fish of the sea, over the birds of the sky, and over every living thing that moves on the earth.”",
        "29 God said, “Behold, I have given you every herb yielding seed, which is on the surface of all the earth, and every tree, which bears fruit yielding seed. It will be your food.",
        "30 To every animal of the earth, and to every bird of the sky, and to everything that creeps on the earth, in which there is life, I have given every green herb for food;” and it was so.",
        "31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.",
      ].join("\n") + "\n"
    )
  end
end

describe 'bbl gen 1' do
  subject(:cmd) { command($bbl_run.call('gen 1')) }

  it 'prints the exact WEBUS Genesis 1 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      [
        "1 In the beginning, God created the heavens and the earth.",
        "2 The earth was formless and empty. Darkness was on the surface of the deep and God’s Spirit was hovering over the surface of the waters.",
        "3 God said, “Let there be light,” and there was light.",
        "4 God saw the light, and saw that it was good. God divided the light from the darkness.",
        "5 God called the light “day”, and the darkness he called “night”. There was evening and there was morning, the first day.",
        "6 God said, “Let there be an expanse in the middle of the waters, and let it divide the waters from the waters.”",
        "7 God made the expanse, and divided the waters which were under the expanse from the waters which were above the expanse; and it was so.",
        "8 God called the expanse “sky”. There was evening and there was morning, a second day.",
        "9 God said, “Let the waters under the sky be gathered together to one place, and let the dry land appear;” and it was so.",
        "10 God called the dry land “earth”, and the gathering together of the waters he called “seas”. God saw that it was good.",
        "11 God said, “Let the earth yield grass, herbs yielding seeds, and fruit trees bearing fruit after their kind, with their seeds in it, on the earth;” and it was so.",
        "12 The earth yielded grass, herbs yielding seed after their kind, and trees bearing fruit, with their seeds in it, after their kind; and God saw that it was good.",
        "13 There was evening and there was morning, a third day.",
        "14 God said, “Let there be lights in the expanse of the sky to divide the day from the night; and let them be for signs to mark seasons, days, and years;",
        "15 and let them be for lights in the expanse of the sky to give light on the earth;” and it was so.",
        "16 God made the two great lights: the greater light to rule the day, and the lesser light to rule the night. He also made the stars.",
        "17 God set them in the expanse of the sky to give light to the earth,",
        "18 and to rule over the day and over the night, and to divide the light from the darkness. God saw that it was good.",
        "19 There was evening and there was morning, a fourth day.",
        "20 God said, “Let the waters abound with living creatures, and let birds fly above the earth in the open expanse of the sky.”",
        "21 God created the large sea creatures and every living creature that moves, with which the waters swarmed, after their kind, and every winged bird after its kind. God saw that it was good.",
        "22 God blessed them, saying, “Be fruitful, and multiply, and fill the waters in the seas, and let birds multiply on the earth.”",
        "23 There was evening and there was morning, a fifth day.",
        "24 God said, “Let the earth produce living creatures after their kind, livestock, creeping things, and animals of the earth after their kind;” and it was so.",
        "25 God made the animals of the earth after their kind, and the livestock after their kind, and everything that creeps on the ground after its kind. God saw that it was good.",
        "26 God said, “Let’s make man in our image, after our likeness. Let them have dominion over the fish of the sea, and over the birds of the sky, and over the livestock, and over all the earth, and over every creeping thing that creeps on the earth.”",
        "27 God created man in his own image. In God’s image he created him; male and female he created them.",
        "28 God blessed them. God said to them, “Be fruitful, multiply, fill the earth, and subdue it. Have dominion over the fish of the sea, over the birds of the sky, and over every living thing that moves on the earth.”",
        "29 God said, “Behold, I have given you every herb yielding seed, which is on the surface of all the earth, and every tree, which bears fruit yielding seed. It will be your food.",
        "30 To every animal of the earth, and to every bird of the sky, and to everything that creeps on the earth, in which there is life, I have given every green herb for food;” and it was so.",
        "31 God saw everything that he had made, and, behold, it was very good. There was evening and there was morning, a sixth day.",
      ].join("\n") + "\n"
    )
  end
end

describe 'bbl john 3:16' do
  subject(:cmd) { command($bbl_run.call('john 3:16')) }

  it 'prints the exact WEBUS John 3:16 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq("16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life.\n")
  end
end

describe 'bbl john 3:16 in kjv' do
  subject(:cmd) { command($bbl_run.call('john 3:16 in kjv')) }

  it 'prints the exact KJV John 3:16 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq("16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.\n")
  end
end

describe 'bbl matthew 28:18-20' do
  subject(:cmd) { command($bbl_run.call('matthew 28:18-20')) }

  it 'prints the exact WEBUS Matthew 28:18-20 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth.\n" \
      "19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,\n" \
      "20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.\n\n"
    )
  end
end

describe 'bbl matthew 28:18-20 in kjv' do
  subject(:cmd) { command($bbl_run.call('matthew 28:18-20 in kjv')) }

  it 'prints the exact KJV Matthew 28:18-20 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "18 And Jesus came and spake unto them, saying, All power is given unto me in heaven and in earth.\n" \
      "19 Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:\n" \
      "20 Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, [even] unto the end of the world. Amen.\n"
    )
  end
end

describe 'bbl genesis 1 in kjv jc krv' do
  subject(:cmd) { command($bbl_run.call('genesis 1 in kjv jc krv')) }

  it 'prints the exact stacked Genesis 1 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "1 In the beginning God created the heaven and the earth.\n" \
      "2 And the earth was without form, and void; and darkness [was] upon the face of the deep. And the Spirit of God moved upon the face of the waters.\n" \
      "3 And God said, Let there be light: and there was light.\n" \
      "4 And God saw the light, that [it was] good: and God divided the light from the darkness.\n" \
      "5 And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day.\n" \
      "6 And God said, Let there be a firmament in the midst of the waters, and let it divide the waters from the waters.\n" \
      "7 And God made the firmament, and divided the waters which [were] under the firmament from the waters which [were] above the firmament: and it was so.\n" \
      "8 And God called the firmament Heaven. And the evening and the morning were the second day.\n" \
      "9 And God said, Let the waters under the heaven be gathered together unto one place, and let the dry [land] appear: and it was so.\n" \
      "10 And God called the dry [land] Earth; and the gathering together of the waters called he Seas: and God saw that [it was] good.\n" \
      "11 And God said, Let the earth bring forth grass, the herb yielding seed, [and] the fruit tree yielding fruit after his kind, whose seed [is] in itself, upon the earth: and it was so.\n" \
      "12 And the earth brought forth grass, [and] herb yielding seed after his kind, and the tree yielding fruit, whose seed [was] in itself, after his kind: and God saw that [it was] good.\n" \
      "13 And the evening and the morning were the third day.\n" \
      "14 And God said, Let there be lights in the firmament of the heaven to divide the day from the night; and let them be for signs, and for seasons, and for days, and years:\n" \
      "15 And let them be for lights in the firmament of the heaven to give light upon the earth: and it was so.\n" \
      "16 And God made two great lights; the greater light to rule the day, and the lesser light to rule the night: [he made] the stars also.\n" \
      "17 And God set them in the firmament of the heaven to give light upon the earth,\n" \
      "18 And to rule over the day and over the night, and to divide the light from the darkness: and God saw that [it was] good.\n" \
      "19 And the evening and the morning were the fourth day.\n" \
      "20 And God said, Let the waters bring forth abundantly the moving creature that hath life, and fowl [that] may fly above the earth in the open firmament of heaven.\n" \
      "21 And God created great whales, and every living creature that moveth, which the waters brought forth abundantly, after their kind, and every winged fowl after his kind: and God saw that [it was] good.\n" \
      "22 And God blessed them, saying, Be fruitful, and multiply, and fill the waters in the seas, and let fowl multiply in the earth.\n" \
      "23 And the evening and the morning were the fifth day.\n" \
      "24 And God said, Let the earth bring forth the living creature after his kind, cattle, and creeping thing, and beast of the earth after his kind: and it was so.\n" \
      "25 And God made the beast of the earth after his kind, and cattle after their kind, and every thing that creepeth upon the earth after his kind: and God saw that [it was] good.\n" \
      "26 And God said, Let us make man in our image, after our likeness: and let them have dominion over the fish of the sea, and over the fowl of the air, and over the cattle, and over all the earth, and over every creeping thing that creepeth upon the earth.\n" \
      "27 So God created man in his [own] image, in the image of God created he him; male and female created he them.\n" \
      "28 And God blessed them, and God said unto them, Be fruitful, and multiply, and replenish the earth, and subdue it: and have dominion over the fish of the sea, and over the fowl of the air, and over every living thing that moveth upon the earth.\n" \
      "29 And God said, Behold, I have given you every herb bearing seed, which [is] upon the face of all the earth, and every tree, in the which [is] the fruit of a tree yielding seed; to you it shall be for meat.\n" \
      "30 And to every beast of the earth, and to every fowl of the air, and to every thing that creepeth upon the earth, wherein [there is] life, [I have given] every green herb for meat: and it was so.\n" \
      "31 And God saw every thing that he had made, and, behold, [it was] very good. And the evening and the morning were the sixth day.\n" \
      "1 はじめに神は天と地とを創造された。\n" \
      "2 地は形なく、むなしく、やみが淵のおもてにあり、神の霊が水のおもてをおおっていた。\n" \
      "3 神は「光あれ」と言われた。すると光があった。\n" \
      "4 神はその光を見て、良しとされた。神はその光とやみとを分けられた。\n" \
      "5 神は光を昼と名づけ、やみを夜と名づけられた。夕となり、また朝となった。第一日である。\n" \
      "6 神はまた言われた、「水の間におおぞらがあって、水と水とを分けよ」。\n" \
      "7 そのようになった。神はおおぞらを造って、おおぞらの下の水とおおぞらの上の水とを分けられた。\n" \
      "8 神はそのおおぞらを天と名づけられた。夕となり、また朝となった。第二日である。\n" \
      "9 神はまた言われた、「天の下の水は一つ所に集まり、かわいた地が現れよ」。そのようになった。\n" \
      "10 神はそのかわいた地を陸と名づけ、水の集まった所を海と名づけられた。神は見て、良しとされた。\n" \
      "11 神はまた言われた、「地は青草と、種をもつ草と、種類にしたがって種のある実を結ぶ果樹とを地の上にはえさせよ」。そのようになった。\n" \
      "12 地は青草と、種類にしたがって種をもつ草と、種類にしたがって種のある実を結ぶ木とをはえさせた。神は見て、良しとされた。\n" \
      "13 夕となり、また朝となった。第三日である。\n" \
      "14 神はまた言われた、「天のおおぞらに光があって昼と夜とを分け、しるしのため、季節のため、日のため、年のためになり、\n" \
      "15 天のおおぞらにあって地を照らす光となれ」。そのようになった。\n" \
      "16 神は二つの大きな光を造り、大きい光に昼をつかさどらせ、小さい光に夜をつかさどらせ、また星を造られた。\n" \
      "17 神はこれらを天のおおぞらに置いて地を照らさせ、\n" \
      "18 昼と夜とをつかさどらせ、光とやみとを分けさせられた。神は見て、良しとされた。\n" \
      "19 夕となり、また朝となった。第四日である。\n" \
      "20 神はまた言われた、「水は生き物の群れで満ち、鳥は地の上、天のおおぞらを飛べ」。\n" \
      "21 神は海の大いなる獣と、水に群がるすべての動く生き物とを、種類にしたがって創造し、また翼のあるすべての鳥を、種類にしたがって創造された。神は見て、良しとされた。\n" \
      "22 神はこれらを祝福して言われた、「生めよ、ふえよ、海の水に満ちよ、また鳥は地にふえよ」。\n" \
      "23 夕となり、また朝となった。第五日である。\n" \
      "24 神はまた言われた、「地は生き物を種類にしたがっていだせ。家畜と、這うものと、地の獣とを種類にしたがっていだせ」。そのようになった。\n" \
      "25 神は地の獣を種類にしたがい、家畜を種類にしたがい、また地に這うすべての物を種類にしたがって造られた。神は見て、良しとされた。\n" \
      "26 神はまた言われた、「われわれのかたちに、われわれにかたどって人を造り、これに海の魚と、空の鳥と、家畜と、地のすべての獣と、地のすべての這うものとを治めさせよう」。\n" \
      "27 神は自分のかたちに人を創造された。すなわち、神のかたちに創造し、男と女とに創造された。\n" \
      "28 神は彼らを祝福して言われた、「生めよ、ふえよ、地に満ちよ、地を従わせよ。また海の魚と、空の鳥と、地に動くすべての生き物とを治めよ」。\n" \
      "29 神はまた言われた、「わたしは全地のおもてにある種をもつすべての草と、種のある実を結ぶすべての木とをあなたがたに与える。これはあなたがたの食物となるであろう。\n" \
      "30 また地のすべての獣、空のすべての鳥、地を這うすべてのもの、すなわち命あるものには、食物としてすべての青草を与える」。そのようになった。\n" \
      "31 神が造ったすべての物を見られたところ、それは、はなはだ良かった。夕となり、また朝となった。第六日である。\n" \
      "1 태초에 하나님이 천지를 창조하시니라\n" \
      "2 땅이 혼돈하고 공허하며 흑암이 깊음 위에 있고 하나님의 신은 수면에 운행하시니라\n" \
      "3 하나님이 가라사대 빛이 있으라 하시매 빛이 있었고\n" \
      "4 그 빛이 하나님의 보시기에 좋았더라 하나님이 빛과 어두움을 나누사\n" \
      "5 빛을 낮이라 칭하시고 어두움을 밤이라 칭하시니라 저녁이 되며 아침이 되니 이는 첫째 날이니라\n" \
      "6 하나님이 가라사대 물 가운데 궁창이 있어 물과 물로 나뉘게 하리라 하시고\n" \
      "7 하나님이 궁창을 만드사 궁창 아래의 물과 궁창 위의 물로 나뉘게 하시매 그대로 되니라\n" \
      "8 하나님이 궁창을 하늘이라 칭하시니라 저녁이 되며 아침이 되니 이는 둘째 날이니라\n" \
      "9 하나님이 가라사대 천하의 물이 한곳으로 모이고 뭍이 드러나라 하시매 그대로 되니라\n" \
      "10 하나님이 뭍을 땅이라 칭하시고 모인 물을 바다라 칭하시니라 하나님의 보시기에 좋았더라\n" \
      "11 하나님이 가라사대 땅은 풀과 씨 맺는 채소와 각기 종류대로 씨 가진 열매 맺는 과목을 내라 하시매 그대로 되어\n" \
      "12 땅이 풀과 각기 종류대로 씨 맺는 채소와 각기 종류대로 씨 가진 열매 맺는 나무를 내니 하나님의 보시기에 좋았더라\n" \
      "13 저녁이 되며 아침이 되니 이는 세째 날이니라\n" \
      "14 하나님이 가라사대 하늘의 궁창에 광명이 있어 주야를 나뉘게 하라 또 그 광명으로 하여 징조와 사시와 일자와 연한이 이루라\n" \
      "15 또 그 광명이 하늘의 궁창에 있어 땅에 비취라 하시고 (그대로 되니라)\n" \
      "16 하나님이 두 큰 광명을 만드사 큰 광명으로 낮을 주관하게 하시고 작은 광명으로 밤을 주관하게 하시며 또 별들을 만드시고\n" \
      "17 하나님이 그것들을 하늘의 궁창에 두어 땅에 비취게 하시며\n" \
      "18 주야를 주관하게 하시며 빛과 어두움을 나뉘게 하시니라 하나님의 보시기에 좋았더라\n" \
      "19 저녁이 되며 아침이 되니 이는 네째 날이니라\n" \
      "20 하나님이 가라사대 물들은 생물로 번성케 하라 땅위 하늘의 궁창에는 새가 날으라 하시고\n" \
      "21 하나님이 큰 물고기와 물에서 번성하여 움직이는 모든 생물을 그 종류대로, 날개 있는 모든 새를 그 종류대로 창조하시니 하나님의 보시기에 좋았더라\n" \
      "22 하나님이 그들에게 복을 주어 가라사대 생육하고 번성하여 여러 바다 물에 충만하라 새들도 땅에 번성하라 하시니라\n" \
      "23 저녁이 되며 아침이 되니 이는 다섯째 날이니라\n" \
      "24 하나님이 가라사대 땅은 생물을 그 종류대로 내되 육축과 기는 것과 땅의 짐승을 종류대로 내라 하시고 (그대로 되니라)\n" \
      "25 하나님이 땅의 짐승을 그 종류대로, 육축을 그 종류대로, 땅에 기는 모든 것을 그 종류대로 만드시니 하나님의 보시기에 좋았더라\n" \
      "26 하나님이 가라사대 우리의 형상을 따라 우리의 모양대로 우리가 사람을 만들고 그로 바다의 고기와 공중의 새와 육축과 온 땅과 땅에 기는 모든 것을 다스리게 하자 하시고\n" \
      "27 하나님이 자기 형상 곧 하나님의 형상대로 사람을 창조하시되 남자와 여자를 창조하시고\n" \
      "28 하나님이 그들에게 복을 주시며 그들에게 이르시되 생육하고 번성하여 땅에 충만하라, 땅을 정복하라, 바다의 고기와 공중의 새와 땅에 움직이는 모든 생물을 다스리라 하시니라\n" \
      "29 하나님이 가라사대 내가 온 지면의 씨 맺는 모든 채소와 씨 가진 열매 맺는 모든 나무를 너희에게 주노니 너희 식물이 되리라\n" \
      "30 또 땅의 모든 짐승과 공중의 모든 새와 생명이 있어 땅에 기는 모든 것에게는 내가 모든 푸른 풀을 식물로 주노라 하시니 그대로 되니라\n" \
      "31 하나님이 그 지으신 모든 것을 보시니 보시기에 심히 좋았더라 저녁이 되며 아침이 되니 이는 여섯째 날이니라\n"
    )
  end
end

describe 'bbl john 3:16 in kjv jc krv' do
  subject(:cmd) { command($bbl_run.call('john 3:16 in kjv jc krv')) }

  it 'prints the exact stacked John 3:16 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.\n" \
      "16 神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。\n" \
      "16 하나님이 세상을 이처럼 사랑하사 독생자를 주셨으니 이는 저를 믿는 자마다 멸망치 않고 영생을 얻게 하려 하심이니라\n"
    )
  end
end

describe 'bbl matthew 28:18-20 in kjv jc krv' do
  subject(:cmd) { command($bbl_run.call('matthew 28:18-20 in kjv jc krv')) }

  it 'prints the exact stacked Matthew 28:18-20 output' do
    expect($bbl_normalized_stdout.call(cmd)).to eq(
      "18 And Jesus came and spake unto them, saying, All power is given unto me in heaven and in earth.\n" \
      "19 Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost:\n" \
      "20 Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, [even] unto the end of the world. Amen.\n" \
      "18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。\n" \
      "19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、\n" \
      "20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。\n" \
      "18 예수께서 나아와 일러 가라사대 하늘과 땅의 모든 권세를 내게 주셨으니\n" \
      "19 그러므로 너희는 가서 모든 족속으로 제자를 삼아 아버지와 아들과 성령의 이름으로 세례를 주고\n" \
      "20 내가 너희에게 분부한 모든 것을 가르쳐 지키게 하라 볼찌어다 내가 세상 끝날까지 너희와 항상 함께 있으리라 하시니라\n"
    )
  end
end

describe 'bbl pack dir' do
  subject { file($bbl_pack_dir) }
  it { should exist }
  it { should be_directory }

  it 'is owned by the Linux install user' do
    skip 'N/A outside Linux' if $bbl_windows || $bbl_macos

    expect(subject.owner).to eq($bbl_install_user)
  end
end

describe 'bbl pack files' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are non-empty' do
    pack_codes.each do |code|
      pack_file = file("#{pack_dir}#{sep}#{code}.zip")
      expect(pack_file).to exist
      expect(pack_file).to be_file
      expect(pack_file.size).to be > 0
      expect(pack_file.owner).to eq($bbl_install_user) unless $bbl_windows || $bbl_macos
    end
  end
end

describe 'bbl helper binaries' do
  let(:helper_names) { $bbl_installed_search_helpers }
  let(:helper_bin_dir) { $bbl_helper_bin_dir }
  let(:sep) { $bbl_sep }

  it 'exist and are executable' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      helper_file = file(path)
      expect(helper_file).to exist
      expect(helper_file).to be_file
      unless $bbl_windows
        expect(helper_file).to be_executable
      end
    end
  end

  it 'report correct version via --version' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '--version'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end

  it 'report correct version via -v' do
    helper_names.each do |name|
      path = "#{helper_bin_dir}#{sep}#{name}"
      expected_name = $bbl_windows ? name.delete_suffix('.exe') : name
      expected_stdout = "#{expected_name} #{$bbl_expected_version}#{$bbl_eol}"
      cmd = command($bbl_helper_run.call(path, '-v'))
      expect(cmd.exit_status).to eq 0
      expect(cmd.stdout).to eq(expected_stdout)
    end
  end
end

describe 'bbl pack manifest versions' do
  let(:pack_codes) { $bbl_installed_pack_codes }
  let(:pack_dir) { $bbl_pack_dir }
  let(:sep) { $bbl_sep }
  let(:expected_version) { $bbl_expected_version }

  it 'match expected version' do
    pack_codes.each do |code|
      pack_file = "#{pack_dir}#{sep}#{code}.zip"
      manifest = "#{code}.0.manifest.json"

      version = if $bbl_windows
        $bbl_zip_manifest_version.call(pack_file, manifest)
      else
        $bbl_zip_manifest_version.call(file(pack_file).content, manifest)
      end

      expect(version).to eq(expected_version)
    end
  end
end
