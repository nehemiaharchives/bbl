package org.gnit.bible

data class Language(

    /**
     * ISO 639-1:2002, Codes for the representation of names of languages—Part 1: Alpha-2 code eg. "en", "es", "fr", "de", "zh", "ko", "ja"
     */
    val code: String,

    /**
     * English name of the language eg. "English", "Spanish", "French", "German", "Chinese", "Korean", "Japanese"
     */
    val englishName: String,

    /**
     * Native name of the language eg. "English", "Español", "Français", "Deutsch", "中文", "한국어", "日本語"
     */
    val nativeName: String,

    /**
     * Pipe-separated list of 66 book names in order from Genesis to Revelation.
     * (comma "," cannot be used as delimiter because book name in some languages contain comma
     * such as Russian "Книга Екклезиаста, или Проповедника" for ecclesiastes)
     */
    private val bookNamesConcat: String,
){
    fun bookNames(): Array<String> = bookNamesConcat.split("|").toTypedArray()

    companion object {
        val en = Language(
            code = "en",
            englishName = "English",
            nativeName = "English",
            bookNamesConcat = "Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|1 Samuel|2 Samuel|1 Kings|2 Kings|1 Chronicles|2 Chronicles|Ezra|Nehemiah|Esther|Job|Psalms|Proverbs|Ecclesiastes|Song of Solomon|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|1 Corinthians|2 Corinthians|Galatians|Ephesians|Philippians|Colossians|1 Thessalonians|2 Thessalonians|1 Timothy|2 Timothy|Titus|Philemon|Hebrews|James|1 Peter|2 Peter|1 John|2 John|3 John|Jude|Revelation"
        )

        val es = Language(
            code = "es",
            englishName = "Spanish",
            nativeName = "Español",
            bookNamesConcat = "Génesis|Éxodo|Levítico|Números|Deuteronomio|Josué|Jueces|Rut|1 Samuel|2 Samuel|1 Reyes|2 Reyes|1 Crónicas|2 Crónicas|Esdras|Nehemías|Ester|Job|Salmos|Proverbios|Eclesiastés|Cantar de los Cantares|Isaías|Jeremías|Lamentaciones|Ezequiel|Daniel|Oseas|Joel|Amós|Abdías|Jonás|Miqueas|Nahum|Habacuc|Sofonías|Hageo|Zacarías|Malaquías|Mateo|Marcos|Lucas|Juan|Hechos|Romanos|1 Corintios|2 Corintios|Gálatas|Efesios|Filipenses|Colosenses|1 Tesalonicenses|2 Tesalonicenses|1 Timoteo|2 Timoteo|Tito|Filipeones|Hebreos|Santiago|1 Pedro|2 Pedro|1 Juan|2 Juan|3 Juan|Judas|ApoCalipsis"
        )

        val pt = Language(
            code = "pt",
            englishName = "Portuguese",
            nativeName = "Português",
            bookNamesConcat = "Gênesis|Êxodo|Levítico|Números|Deuteronômio|Josué|Juízes|Rute|1 Samuel|2 Samuel|1 Reis|2 Reis|1 Crônicas|2 Crônicas|Esdras|Neemias|Ester|Jó|Salmos|Provérbios|Eclesiastes|Cântico|Isaías|Jeremias|Lamentações|Ezequiel|Daniel|Oseias|Joel|Amós|Obadias|Jonas|Miqueias|Naum|Habacuque|Sofonias|Ageu|Zacarias|Malaquias|Mateus|Marcos|Lucas|João|Atos|Romanos|1 Coríntios|2 Coríntios|Gálatas|Efésios|Filipenses|Colossenses|1 Tessalonicenses|2 Tessalonicenses|1 Timóteo|2 Timóteo|Tito|Filemom|Hebreus|Tiago|1 Pedro|2 Pedro|1 João|2 João|3 João|Judas|ApoCalipse"
        )

        val de = Language(
            code = "de",
            englishName = "German",
            nativeName = "Deutsch",
            bookNamesConcat = "1. Mose|2. Mose|3. Mose|4. Mose|5. Mose|Josua|Richter|Rut|1. Samuel|2. Samuel|1. Könige|2. Könige|1. Chronik|2. Chronik|Esra|Nehemia|Ester|Hiob|Psalmen|Sprüche|Prediger|Hohelied|Jesaja|Jeremia|Klagelieder|Hesekiel|Daniel|Hosea|Joel|Amos|Obadja|Jona|Micha|Nahum|Habakuk|Zephanja|Haggai|Sacharja|Maleachi|Matthäus|Markus|Lukas|Johannes|Apostelgeschichte|Römer|1. Korinther|2. Korinther|Galater|Epheser|Philipper|Kolosser|1. Thessalonicher|2. Thessalonicher|1. Timotheus|2. Timotheus|Titus|Philemon|Hebräer|Jakobus|1. Petrus|2. Petrus|1. Johannes|2. Johannes|3. Johannes|Judas|Offenbarung"
        )

        val fr = Language(
            code = "fr",
            englishName = "French",
            nativeName = "Français",
            bookNamesConcat = "Genèse|Exode|Lévitique|Nombres|Deutéronome|Josué|Juges|Ruth|1 Samuel|2 Samuel|1 Rois|2 Rois|1 Chroniques|2 Chroniques|Esdras|Néhémie|Esther|Job|Psaumes|Proverbes|Ecclésiaste|Cantique|Ésaïe|Jérémie|Lamentations|Ézékiel|Daniel|Osée|Joël|Amos|Abdias|Jonas|Michée|Nahum|Habacuc|Sophonie|Aggée|Zacharie|Malachie|Matthieu|Marc|Luc|Jean|Actes|Romains|1 Corinthiens|2 Corinthiens|Galates|Éphésiens|Philippiens|Colossiens|1 Thessaloniciens|2 Thessaloniciens|1 Timothée|2 Timothée|Tite|Philémon|Hébreux|Jacques|1 Pierre|2 Pierre|1 Jean|2 Jean|3 Jean|Jude|Apocalypse"
        )

        val ru = Language(
            code = "ru",
            englishName = "Russian",
            nativeName = "Русский",
            bookNamesConcat = "Бытие|Исход|Левит|Числа|Второзаконие|Книга Иисуса Навина|Книга Судей израилевых|Книга Руфи|Первая книга Царств|Вторая книга Царств|Третья книга Царств|Четвертая книга Царств|Первая книга Паралипоменон|Вторая книга Паралипоменон|Первая книга Ездры|Книга Неемии|Есфирь|Книга Иова|Псалтирь|Притчи Соломона|Книга Екклезиаста, или Проповедника|Песнь песней Соломона|Книга пророка Исаии|Книга пророка Иеремии|Плач Иеремии|Книга пророка Иезекииля|Книга пророка Даниила|Книга пророка Осии|Книга пророка Иоиля|Книга пророка Амоса|Книга пророка Авдия|Книга пророка Ионы|Книга пророка Михея|Книга пророка Наума|Книга пророка Аввакума|Книга пророка Софонии|Книга пророка Аггея|Книга пророка Захарии|Книга пророка Малахии|От Матфея святое благовествование|От Марка святое благовествование|От Луки святое благовествование|От Иоанна святое благовествование|Деяния святых апостолов|Послание к Римлянам|Первое послание к Коринфянам|Второе послание к Коринфянам|Послание к Галатам|Послание к Ефесянам|Послание к Филиппийцам|Послание к Колоссянам|Первое послание к Фессалоникийцам|Второе послание к Фессалоникийцам|Первое послание к Тимофею|Второе послание к Тимофею|Послание к Титу|Послание к Филимону|Послание к Евреям|Послание Иакова|Первое послание Петра|Второе послание Петра|Первое послание Иоанна|Второе послание Иоанна|Третье послание Иоанна|Послание Иуды|Откровение ап. Иоанна Богослова"
        )

        val nl = Language(
            code = "nl",
            englishName = "Dutch",
            nativeName = "Nederlands",
            bookNamesConcat = "GENESIS|EXODUS|LEVITICUS|NUMERI|DEUTERONOMIUM|JOZUA|RICHTEREN|RUTH|1 SAMUËL|2 SAMUËL|1 KONINGEN|2 KONINGEN|1 KRONIEKEN|2 KRONIEKEN|EZRA|NEHEMIA|ESTHER|JOB|PSALMEN|SPREUKEN|PREDIKER|HOOGLIED|JESAJA|JEREMIA|KLAAGLIEDEREN VAN JEREMIA|EZECHIËL|DANIËL|HOSÉA|JOËL|AMOS|OBADJA|JONA|MICHA|NAHUM|HABAKUK|ZEFANJA|HAGGAÏ|ZACHARIA|MALEACHI|MATTHEÜS|MARKUS|LUKAS|JOHANNES|HANDELINGEN|ROMEINEN|1 KORINTHE|2 KORINTHE|GALATEN|EFEZE|FILIPPENZEN|KOLOSSENZEN|1 THESSALONICENZEN|2 THESSALONICENZEN|1 TIMÓTHEÜS|2 TIMÓTHEÜS|TITUS|FILÉMON|HEBREEËN|JAKOBUS|1 PETRUS|2 PETRUS|1 JOHANNES|2 JOHANNES|3 JOHANNES|JUDAS|OPENBARING"
        )

        val it = Language(
            code = "it",
            englishName = "Italian",
            nativeName = "Italiano",
            bookNamesConcat = "GENESI|ESODO|LEVITICO|NUMERI|DEUTERONOMIO|GIOSUÈ|GIUDICI|RUTH|I SAMUELE|II SAMUELE|I RE|II RE|I CRONACHE|II CRONACHE|ESDRA|NEHEMIA|ESTER|GIOBBE|SALMI|PROVERBI|ECCLESIASTE|CANTICO DE'~CANTICI|ISAIA|GEREMIA|LAMENTAZIONI|EZECHIELE|DANIELE|OSEA|GIOELE|AMOS|ABDIA|GIONA|MICHEA|NAHUM|HABACUC|SOFONIA|AGGEO|ZACCARIA|MALACHIA|Matteo|Marco|Luca|Giovanni|ATTI DEGLI APOSTOLI|EPISTOLE DI S. PAOLO AI~ROMANI|I Corinzi|II Corinzi|EPISTOLE DI S. PAOLO AI GALATI|EPISTOLE DI S. PAOLO AGLI EFESINI|EPISTOLE DI S. PAOLO AI FILIPPESI|EPISTOLE DI S. PAOLO AI COLOSSESI|EPISTOLE DI S. PAOLO I AI TESSALONICESI|EPISTOLE DI S. PAOLO II AI TESSALONICESI|EPISTOLE DI S. PAOLO I A TIMOTEO|EPISTOLE DI S. PAOLO II A TIMOTEO|EPISTOLE DI S. PAOLO A TITO|EPISTOLE DI S. PAOLO A FILEMONE|EPISTOLA AGLI EBREI|EPISTOLA DI S. GIACOMO|EPISTOLA I DI S. PIETRO|EPISTOLA II DI S. PIETRO|EPISTOLA I DI S. GIOVANNI|EPISTOLA II DI S. GIOVANNI|EPISTOLA III DI S. GIOVANNI|EPISTOLA DI S. GIUDA|APOCALISSE"
        )

        val pl = Language(
            code = "pl",
            englishName = "Polish",
            nativeName = "Polski",
            bookNamesConcat = "Rodzaju|Wyjścia|Kapłańska|Liczb|Powtórzonego|Jozuego|Księga Sędziów|Rut|I Samuela|II Samuela|I Królewska|II Królewska|I Kronik|II Kronik|Ezdrasza|Nehemiasza|Estery|Hioba|Psalmów|Przysłów|Kaznodziei|Pieśń nad Pieśniami|Izajasza|Jeremiasza|Lamentacje|Ezechiela|Daniela|Ozeasza|Joela|Amosa|Abdiasza|Jonasza|Micheasza|Nahuma|Habakuka|Sofoniasza|Aggeusza|Zachariasza|Malachiasza|Mateusza|Marka|Łukasza|Jana|Dzieje|Rzymian|I Koryntian|II Koryntian|Galacjan|Efezjan|Filipian|Kolosan|I Tesaloniczan|II Tesaloniczan|I Tymoteusza|II Tymoteusza|Tytusa|Filemona|Hebrajczyków|Jakuba|I Piotra|II Piotra|I Jana|II Jana|III Jana|Judy|Objawienie"
        )

        val uk = Language(
            code = "uk",
            englishName = "Ukrainian",
            nativeName = "Українська",
            bookNamesConcat = "Буття|Вихід|Левит|Числа|Повторення Закону|Iсус Навин|Книга Суддiв|Рут|1-а Самуїлова|2-а Самуїлова|1-а царiв|2-а царiв|1-а хронiки|2-а хронiки|Ездра|Неемія|Естер|Йов|Псалми|Приповiстi|Екклезiяст|Пiсня над пiснями|Iсая|Єремiя|Плач Єремiї|Єзекiїль|Даниїл|Осiя|Йоїл|Амос|Овдiй|Йона|Михей|Наум|Авакум|Софонiя|Огiй|Захарiя|Малахiї|Вiд Матвiя|Вiд Марка|Вiд Луки|Вiд Iвана|Дiї|До римлян|1-е до коринтян|2-е до коринтян|До галатiв|До ефесян|До филип'ян|До колоссян|1-е до солунян|2-е до солунян|1-е Тимофiю|2-е Тимофiю|До Тита|До Филимона|До євреїв|Якова|1-е Петра|2-е Петра|1-е Iвана|2-е Iвана|3-е Iвана|Юда|Об'явлення"
        )

        val sv = Language(
            code = "sv",
            englishName = "Swedish",
            nativeName = "Svenska",
            bookNamesConcat = "1 Mosebok|2 Mosebok|3 Mosebok|4 Mosebok|5 Mosebok|Josua|Domarboken|Rut|1 Samuelsboken|2 Samuelsboken|1 Kungaboken|2 Kungaboken|1 Krönikeboken|2 Krönikeboken|Esra|Nehemja|Ester|Job|Psaltaren|Ordspråksboken|Predikaren|Höga Visan|Jesaja|Jeremia|Klagovisorna|Hesekiel|Daniel|Hosea|Joel|Amos|Obadja|Jona|Mika|Nahum|Habackuk|Sefanja|Haggai|Sakarja|Malaki|Matteus|Markus|Lukas|Johannes|Apostlagärningarna|Romarbrevet|1 Korinthierbrevet|2 Korinthierbrevet|Galaterbrevet|Efesierbrevet|Filipperbrevet|Kolosserbrevet|1 Thessalonikerbreve|2 Thessalonikerbreve|1 Timotheosbrevet|2 Timotheosbrevet|Titusbrevet|Filemonbrevet|Hebreerbrevet|Jakobsbrevet|1 Petrusbrevet|2 Petrusbrevet|1 Johannesbrevet|2 Johannesbrevet|3 Johannesbrevet|Judasbrevet|Uppenbarelseboken"
        )

        val zh = Language(
            code = "zh",
            englishName = "Chinese",
            nativeName = "中文",
            bookNamesConcat = "创世记|出埃及|利未记|民数记|申命记|约书亚记|士师记|路得记|撒母耳记上|撒母耳记下|列王纪上|列王纪下|历代志上|历代志下|以斯拉记|尼希米记|以斯帖记|约伯记|诗篇|箴言|传道书|雅歌|以赛亚书|耶利米书|耶利米哀歌|以西结书|但以理书|何西阿书|约珥书|阿摩司书|俄巴底亚书|约拿书|弥迦书|那鸿书|哈巴谷书|西番雅书|哈该书|撒迦利亚书|玛拉基书|马太福音|马可福音|路加福音|约翰福音|使徒行传|罗马书|哥林多前书|哥林多后书|加拉太书|以弗所书|腓立比书|歌罗西书|帖撒罗尼迦前书|帖撒罗尼迦后书|提摩太前书|提摩太后书|提多书|腓利门书|希伯来书|雅各书|彼得前书|彼得后书|约翰一书|约翰二书|约翰三书|犹大书|启示录"
        )

        val ko = Language(
            code = "ko",
            englishName = "Korean",
            nativeName = "한국어",
            bookNamesConcat = "창세기|출애굽기|레위기|민수기|신명기|여호수아|사사기|룻기|사무엘상|사무엘하|열왕기상|열왕기하|역대상|역대하|에스라|느헤미야|에스더|욥기|시편|잠언|전도서|아가|이사야|예레미야|예레미야애가|에스겔|다니엘|호세아|요엘|아모스|오바댜|요나|미가|나훔|하박국|스바냐|학개|스가랴|말라기|마태복음|마가복음|누가복음|요한복음|사도행전|로마서|고린도전서|고린도후서|갈라디아서|에베소서|빌립보서|골로새서|데살로니가전서|데살로니가후서|디모데전서|디모데후서|디도서|빌레몬서|히브리서|야고보서|베드로전서|베드로후서|요한1서|요한2서|요한3서|유다서|요한계시록"
        )

        /**
         * Japanese book names differs for each Bible translations, but only public domain modern Japanese version is jc, so we use book names of jc here as default.
         */
        val ja = Language(
            code = "ja",
            englishName = "Japanese",
            nativeName = "日本語",
            bookNamesConcat = "創世記|出エジプト記|レビ記|民数記|申命記|ヨシュア記|士師記|ルツ記|サムエル記上|サムエル記下|列王記上|列王記下|歴代誌上|歴代誌下|エズラ記|ネヘミヤ記|エステル記|ヨブ記|詩篇|箴言|伝道の書|雅歌|イザヤ書|エレミヤ書|哀歌|エゼキエル書|ダニエル書|ホセア書|ヨエル書|アモス書|オバデヤ書|ヨナ書|ミカ書|ナホム書|ハバクク書|ゼパニヤ書|ハガイ書|ゼカリヤ書|マラキ書|マタイによる福音書|マルコによる福音書|ルカによる福音書|ヨハネによる福音書|使徒行伝|ローマ人への手紙|コリント人への第一の手紙|コリント人への第二の手紙|ガラテヤ人への手紙|エペソ人への手紙|ピリピ人への手紙|コロサイ人への手紙|テサロニケ人への第一の手紙|テサロニケ人への第二の手紙|テモテへの第一の手紙|テモテへの第二の手紙|テトスへの手紙|ピレモンへの手紙|ヘブル人への手紙|ヤコブの手紙|ペテロの第一の手紙|ペテロの第二の手紙|ヨハネの第一の手紙|ヨハネの第二の手紙|ヨハネの第三の手紙|ユダの手紙|ヨハネの黙示録"
        )

        val embeddedLanguages = arrayOf(en, es, pt, de, fr, ru, nl, it, pl, uk, sv, zh, ko, ja)

        val hi =  Language(
            code = "hi",
            englishName = "Hindi",
            nativeName = "हिन्दी",
            bookNamesConcat = "उत्पत्ति|निर्गमन|लैव्यव्यवस्था|गिनती|व्यवस्थाविवरण|यहोशू|न्यायियों|रूत|1 शमूएल|2 शमूएल|1 राजाओं|2 राजाओं|1 इतिहास|2 इतिहास|एज्रा|नहेम्याह|एस्तेर|अय्यूब|भजन संहिता|नीतिवचन|सभोपदेशक|श्रेष्ठगीत|यशायाह|यिर्मयाह|विलापगीत|यहेजकेल|दानिय्येल|होशे|योएल|आमोस|ओबद्याह|योना|मीका|नहूम|हबक्कूक|सपन्याह|हाग्गै|जकर्याह|मलाकी|मत्ती|मरकुस|लूका|यूहन्ना|प्रेरितों के काम|रोमियों|1 कुरिन्थियों|2 कुरिन्थियों|गलातियों|इफिसियों|फिलिप्पियों|कुलुस्सियों|1 थिस्सलुनीकियों|2 थिस्सलुनीकियों|1 तीमुथियुस|2 तीमुथियुस|तीतुस|फिलेमोन|इब्रानियों|याकूब|1 पतरस|2 पतरस|1 यूहन्ना|2 यूहन्ना|3 यूहन्ना|यहूदा|प्रकाशितवाक्य"
        )

        val bn = Language(
            code = "bn",
            englishName = "Bengali",
            nativeName = "বাংলা",
            bookNamesConcat = "আদিপুস্তক|যাত্রাপুস্তক|লেবীয়|গণনা|দ্বিতীয় বিবরণ|যিহোশূয়|বিচারকর্ত্তৃগণ|রূত|1 শমূয়েল|2 শমূয়েল|1 রাজাবলি|2 রাজাবলি|1 বংশাবলি|2 বংশাবলি|ইষ্রা|নহিমিয়|ইষ্টের|ইয়োব|গীতসংহিতা|হিতোপদেশ|উপদেশক|পরমগীত|যিশাইয়|যিরমিয়|বিলাপ|যিহিস্কেল|দানিয়েল|হোশেয়|যোয়েল|আমোষ|ওবদিয়|যোনা|মীখা|নহূম|হবককূক|সফনিয়|হগয়|সখরিয়|মালাখি|মথি|মার্ক|লুক|যোহন|প্রেরিতদের কার্য্য|রোমীয়|1 করিন্থীয়|2 করিন্থীয়|গালাতীয়|ইফিষীয়|ফিলিপীয়|কলসীয়|1 থিষলনীকীয়|2 থিষলনীকীয়|1 তীমথিয়|2 তীমথিয়|তীত|ফিলীমন|ইব্রীয়|যাকোব|1 পিতর|2 পিতর|1 যোহন|2 যোহন|3 যোহন|যিহূদা|প্রকাশিত বাক্য"
        )

        val mr = Language(
            code = "mr",
            englishName = "Marathi",
            nativeName = "मराठी",
            bookNamesConcat = "उत्प.|निर्ग.|लेवी.|गण.|अनु.|यहो.|शास्ते|रूथ|1 शमु.|2 शमु.|1 राजे|2 राजे|1 इति.|2 इति.|एज्रा|नहे.|एस्ते.|ईयो.|स्तोत्र.|नीति.|उप.|गीत.|यश.|यिर्म.|विला.|यहे.|दानि.|होशे.|योए.|आमो.|ओब.|योना|मीखा|नहू.|हब.|सफ.|हाग्ग.|जख.|मला.|मत्त.|मार्क|लूक|योहा.|प्रेषि.|रोम.|1 करिं.|2 करिं.|गल.|इफि.|फिलि.|कल.|1 थेस्स.|2 थेस्स.|1 तीम.|2 तीम.|तीत.|फिले.|इब्री.|याको.|1 पेत्र.|2 पेत्र.|1 योहा.|2 योहा.|3 योहा.|यहू.|प्रक."
        )

        val downloadableLanguages = arrayOf(hi, bn, mr)
    }
}
