package org.gnit.bible

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.gnit.lucenekmp.analysis.Analyzer
import org.gnit.lucenekmp.analysis.bn.BengaliAnalyzer
import org.gnit.lucenekmp.analysis.cn.smart.SmartChineseAnalyzer
import org.gnit.lucenekmp.analysis.de.GermanAnalyzer
import org.gnit.lucenekmp.analysis.en.EnglishAnalyzer
import org.gnit.lucenekmp.analysis.es.SpanishAnalyzer
import org.gnit.lucenekmp.analysis.fr.FrenchAnalyzer
import org.gnit.lucenekmp.analysis.gu.GujaratiAnalyzer
import org.gnit.lucenekmp.analysis.hi.HindiAnalyzer
import org.gnit.lucenekmp.analysis.id.IndonesianAnalyzer
import org.gnit.lucenekmp.analysis.it.ItalianAnalyzer
import org.gnit.lucenekmp.analysis.ja.JapaneseAnalyzer
import org.gnit.lucenekmp.analysis.ko.KoreanAnalyzer
import org.gnit.lucenekmp.analysis.morfologik.MorfologikAnalyzer
import org.gnit.lucenekmp.analysis.mr.MarathiAnalyzer
import org.gnit.lucenekmp.analysis.ne.NepaliAnalyzer
import org.gnit.lucenekmp.analysis.nl.DutchAnalyzer
import org.gnit.lucenekmp.analysis.pt.PortugueseAnalyzer
import org.gnit.lucenekmp.analysis.ru.RussianAnalyzer
import org.gnit.lucenekmp.analysis.sv.SwedishAnalyzer
import org.gnit.lucenekmp.analysis.ta.TamilAnalyzer
import org.gnit.lucenekmp.analysis.te.TeluguAnalyzer
import org.gnit.lucenekmp.analysis.th.ThaiAnalyzer
import org.gnit.lucenekmp.analysis.tl.TagalogAnalyzer
import org.gnit.lucenekmp.analysis.uk.UkrainianMorfologikAnalyzer
import org.gnit.lucenekmp.analysis.ur.UrduAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseAnalyzer
import org.gnit.lucenekmp.analysis.vi.VietnameseConfig

@Serializable
data class Language(

    /**
     * ISO 639-1:2002, Codes for the representation of names of languages—Part 1: Alpha-2 code eg. "en", "es", "fr", "de", "zh", "ko", "ja"
     * Context of use, this code is used internally in the bbl-kmp code but also used as command line argument to specify language in bbl command line tool.
     * For that cli user experience we include one exception to include none-ISO 639-1 code for Traditional Chinese which is "zht" where t means Traditional script.
     */
    val code: String,

    /**
     * Custom script code for languages with multiple scripts, defaults to null but can be set to specific script code if needed.
     *
     * ISO 15924, Codes for the representation of names of scripts, eg. "Hans" for Simplified Chinese, "Hant" for Traditional Chinese, "Cyrl" for Cyrillic script, "Latn" for Latin script
     * Background: for most of the languages, above `code` is sufficient to identify the language, but some languages are written in multiple scripts and
     * those speakers of the language needs to be able to choose the script they are familiar with. Prominent examples are Chinese (zh-Hans for Simplified Chinese, zh-Hant for Traditional Chinese),
     * Serbian (sr-Cyrl for Serbian in Cyrillic script, sr-Latn for Serbian in Latin script)
     */
    val customScriptCode: String? = null,

    /**
     * English name of the language eg. "English", "Spanish", "French", "German", "Chinese", "Korean", "Japanese"
     */
    val englishName: String,

    /**
     * Native name of the language eg. "English", "Español", "Français", "Deutsch", "中文", "한국어", "日本語"
     */
    val nativeName: String,

    /**
     * Whether the language is Chinese, Japanese or Korean language
      */
    val isCJK: Boolean = false,

    /**
     * Used for making list order by language
     */
    val order: Int,

    /**
     * Pipe-separated list of 66 book names in order from Genesis to Revelation.
     * (comma "," cannot be used as delimiter because book name in some languages contain comma
     * such as Russian "Книга Екклезиаста, или Проповедника" for ecclesiastes)
     */
    private val bookNamesConcat: String,

    @Transient
    val analyzerFactory: (() -> Analyzer)? = null,
){
    fun bookNames(): Array<String> = bookNamesConcat.split("|").toTypedArray()

    companion object {

        // language category used for constructing Language.order
        const val WESTERN = 1000
        const val EAST_ASIA = 2000
        const val SOUTH_EAST_ASIA = 3000
        const val SOUTH_ASIA = 4000

        // reserved constants for the future
        // const val PACIFIC = 5000
        // const val MIDDLE_EAST = 6000
        // const val AFRICA = 7000
        // const val NORTH_AMERICAN_NATIVE = 8000
        // const val SOUTH_AMERICAN_NATIVE = 9000

        val en = Language(
            code = "en",
            englishName = "English",
            nativeName = "English",
            order = WESTERN + 1,
            bookNamesConcat = "Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|1 Samuel|2 Samuel|1 Kings|2 Kings|1 Chronicles|2 Chronicles|Ezra|Nehemiah|Esther|Job|Psalms|Proverbs|Ecclesiastes|Song of Solomon|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|1 Corinthians|2 Corinthians|Galatians|Ephesians|Philippians|Colossians|1 Thessalonians|2 Thessalonians|1 Timothy|2 Timothy|Titus|Philemon|Hebrews|James|1 Peter|2 Peter|1 John|2 John|3 John|Jude|Revelation",
            analyzerFactory = { EnglishAnalyzer() }
        )

        val es = Language(
            code = "es",
            englishName = "Spanish",
            nativeName = "Español",
            order = WESTERN + 2,
            bookNamesConcat = "Génesis|Éxodo|Levítico|Números|Deuteronomio|Josué|Jueces|Rut|1 Samuel|2 Samuel|1 Reyes|2 Reyes|1 Crónicas|2 Crónicas|Esdras|Nehemías|Ester|Job|Salmos|Proverbios|Eclesiastés|Cantar de los Cantares|Isaías|Jeremías|Lamentaciones|Ezequiel|Daniel|Oseas|Joel|Amós|Abdías|Jonás|Miqueas|Nahum|Habacuc|Sofonías|Hageo|Zacarías|Malaquías|Mateo|Marcos|Lucas|Juan|Hechos|Romanos|1 Corintios|2 Corintios|Gálatas|Efesios|Filipenses|Colosenses|1 Tesalonicenses|2 Tesalonicenses|1 Timoteo|2 Timoteo|Tito|Filipeones|Hebreos|Santiago|1 Pedro|2 Pedro|1 Juan|2 Juan|3 Juan|Judas|ApoCalipsis",
            analyzerFactory = { SpanishAnalyzer() }
        )

        val pt = Language(
            code = "pt",
            englishName = "Portuguese",
            nativeName = "Português",
            order = WESTERN + 3,
            bookNamesConcat = "Gênesis|Êxodo|Levítico|Números|Deuteronômio|Josué|Juízes|Rute|1 Samuel|2 Samuel|1 Reis|2 Reis|1 Crônicas|2 Crônicas|Esdras|Neemias|Ester|Jó|Salmos|Provérbios|Eclesiastes|Cântico|Isaías|Jeremias|Lamentações|Ezequiel|Daniel|Oseias|Joel|Amós|Obadias|Jonas|Miqueias|Naum|Habacuque|Sofonias|Ageu|Zacarias|Malaquias|Mateus|Marcos|Lucas|João|Atos|Romanos|1 Coríntios|2 Coríntios|Gálatas|Efésios|Filipenses|Colossenses|1 Tessalonicenses|2 Tessalonicenses|1 Timóteo|2 Timóteo|Tito|Filemom|Hebreus|Tiago|1 Pedro|2 Pedro|1 João|2 João|3 João|Judas|ApoCalipse",
            analyzerFactory = { PortugueseAnalyzer() }
        )

        val de = Language(
            code = "de",
            englishName = "German",
            nativeName = "Deutsch",
            order = WESTERN + 4,
            bookNamesConcat = "1. Mose|2. Mose|3. Mose|4. Mose|5. Mose|Josua|Richter|Rut|1. Samuel|2. Samuel|1. Könige|2. Könige|1. Chronik|2. Chronik|Esra|Nehemia|Ester|Hiob|Psalmen|Sprüche|Prediger|Hohelied|Jesaja|Jeremia|Klagelieder|Hesekiel|Daniel|Hosea|Joel|Amos|Obadja|Jona|Micha|Nahum|Habakuk|Zephanja|Haggai|Sacharja|Maleachi|Matthäus|Markus|Lukas|Johannes|Apostelgeschichte|Römer|1. Korinther|2. Korinther|Galater|Epheser|Philipper|Kolosser|1. Thessalonicher|2. Thessalonicher|1. Timotheus|2. Timotheus|Titus|Philemon|Hebräer|Jakobus|1. Petrus|2. Petrus|1. Johannes|2. Johannes|3. Johannes|Judas|Offenbarung",
            analyzerFactory = { GermanAnalyzer() }
        )

        val fr = Language(
            code = "fr",
            englishName = "French",
            nativeName = "Français",
            order = WESTERN + 5,
            bookNamesConcat = "Genèse|Exode|Lévitique|Nombres|Deutéronome|Josué|Juges|Ruth|1 Samuel|2 Samuel|1 Rois|2 Rois|1 Chroniques|2 Chroniques|Esdras|Néhémie|Esther|Job|Psaumes|Proverbes|Ecclésiaste|Cantique|Ésaïe|Jérémie|Lamentations|Ézékiel|Daniel|Osée|Joël|Amos|Abdias|Jonas|Michée|Nahum|Habacuc|Sophonie|Aggée|Zacharie|Malachie|Matthieu|Marc|Luc|Jean|Actes|Romains|1 Corinthiens|2 Corinthiens|Galates|Éphésiens|Philippiens|Colossiens|1 Thessaloniciens|2 Thessaloniciens|1 Timothée|2 Timothée|Tite|Philémon|Hébreux|Jacques|1 Pierre|2 Pierre|1 Jean|2 Jean|3 Jean|Jude|Apocalypse",
            analyzerFactory = { FrenchAnalyzer() }
        )

        val ru = Language(
            code = "ru",
            englishName = "Russian",
            nativeName = "Русский",
            order = WESTERN + 6,
            bookNamesConcat = "Бытие|Исход|Левит|Числа|Второзаконие|Книга Иисуса Навина|Книга Судей израилевых|Книга Руфи|Первая книга Царств|Вторая книга Царств|Третья книга Царств|Четвертая книга Царств|Первая книга Паралипоменон|Вторая книга Паралипоменон|Первая книга Ездры|Книга Неемии|Есфирь|Книга Иова|Псалтирь|Притчи Соломона|Книга Екклезиаста, или Проповедника|Песнь песней Соломона|Книга пророка Исаии|Книга пророка Иеремии|Плач Иеремии|Книга пророка Иезекииля|Книга пророка Даниила|Книга пророка Осии|Книга пророка Иоиля|Книга пророка Амоса|Книга пророка Авдия|Книга пророка Ионы|Книга пророка Михея|Книга пророка Наума|Книга пророка Аввакума|Книга пророка Софонии|Книга пророка Аггея|Книга пророка Захарии|Книга пророка Малахии|От Матфея святое благовествование|От Марка святое благовествование|От Луки святое благовествование|От Иоанна святое благовествование|Деяния святых апостолов|Послание к Римлянам|Первое послание к Коринфянам|Второе послание к Коринфянам|Послание к Галатам|Послание к Ефесянам|Послание к Филиппийцам|Послание к Колоссянам|Первое послание к Фессалоникийцам|Второе послание к Фессалоникийцам|Первое послание к Тимофею|Второе послание к Тимофею|Послание к Титу|Послание к Филимону|Послание к Евреям|Послание Иакова|Первое послание Петра|Второе послание Петра|Первое послание Иоанна|Второе послание Иоанна|Третье послание Иоанна|Послание Иуды|Откровение ап. Иоанна Богослова",
            analyzerFactory = { RussianAnalyzer() }
        )

        val nl = Language(
            code = "nl",
            englishName = "Dutch",
            nativeName = "Nederlands",
            order = WESTERN + 7,
            bookNamesConcat = "GENESIS|EXODUS|LEVITICUS|NUMERI|DEUTERONOMIUM|JOZUA|RICHTEREN|RUTH|1 SAMUËL|2 SAMUËL|1 KONINGEN|2 KONINGEN|1 KRONIEKEN|2 KRONIEKEN|EZRA|NEHEMIA|ESTHER|JOB|PSALMEN|SPREUKEN|PREDIKER|HOOGLIED|JESAJA|JEREMIA|KLAAGLIEDEREN VAN JEREMIA|EZECHIËL|DANIËL|HOSÉA|JOËL|AMOS|OBADJA|JONA|MICHA|NAHUM|HABAKUK|ZEFANJA|HAGGAÏ|ZACHARIA|MALEACHI|MATTHEÜS|MARKUS|LUKAS|JOHANNES|HANDELINGEN|ROMEINEN|1 KORINTHE|2 KORINTHE|GALATEN|EFEZE|FILIPPENZEN|KOLOSSENZEN|1 THESSALONICENZEN|2 THESSALONICENZEN|1 TIMÓTHEÜS|2 TIMÓTHEÜS|TITUS|FILÉMON|HEBREEËN|JAKOBUS|1 PETRUS|2 PETRUS|1 JOHANNES|2 JOHANNES|3 JOHANNES|JUDAS|OPENBARING",
            analyzerFactory = { DutchAnalyzer() }
        )

        val it = Language(
            code = "it",
            englishName = "Italian",
            nativeName = "Italiano",
            order = WESTERN + 8,
            bookNamesConcat = "GENESI|ESODO|LEVITICO|NUMERI|DEUTERONOMIO|GIOSUÈ|GIUDICI|RUTH|I SAMUELE|II SAMUELE|I RE|II RE|I CRONACHE|II CRONACHE|ESDRA|NEHEMIA|ESTER|GIOBBE|SALMI|PROVERBI|ECCLESIASTE|CANTICO DE'~CANTICI|ISAIA|GEREMIA|LAMENTAZIONI|EZECHIELE|DANIELE|OSEA|GIOELE|AMOS|ABDIA|GIONA|MICHEA|NAHUM|HABACUC|SOFONIA|AGGEO|ZACCARIA|MALACHIA|Matteo|Marco|Luca|Giovanni|ATTI DEGLI APOSTOLI|EPISTOLE DI S. PAOLO AI~ROMANI|I Corinzi|II Corinzi|EPISTOLE DI S. PAOLO AI GALATI|EPISTOLE DI S. PAOLO AGLI EFESINI|EPISTOLE DI S. PAOLO AI FILIPPESI|EPISTOLE DI S. PAOLO AI COLOSSESI|EPISTOLE DI S. PAOLO I AI TESSALONICESI|EPISTOLE DI S. PAOLO II AI TESSALONICESI|EPISTOLE DI S. PAOLO I A TIMOTEO|EPISTOLE DI S. PAOLO II A TIMOTEO|EPISTOLE DI S. PAOLO A TITO|EPISTOLE DI S. PAOLO A FILEMONE|EPISTOLA AGLI EBREI|EPISTOLA DI S. GIACOMO|EPISTOLA I DI S. PIETRO|EPISTOLA II DI S. PIETRO|EPISTOLA I DI S. GIOVANNI|EPISTOLA II DI S. GIOVANNI|EPISTOLA III DI S. GIOVANNI|EPISTOLA DI S. GIUDA|APOCALISSE",
            analyzerFactory = { ItalianAnalyzer() }
        )

        val pl = Language(
            code = "pl",
            englishName = "Polish",
            nativeName = "Polski",
            order = WESTERN + 9,
            bookNamesConcat = "Rodzaju|Wyjścia|Kapłańska|Liczb|Powtórzonego|Jozuego|Księga Sędziów|Rut|I Samuela|II Samuela|I Królewska|II Królewska|I Kronik|II Kronik|Ezdrasza|Nehemiasza|Estery|Hioba|Psalmów|Przysłów|Kaznodziei|Pieśń nad Pieśniami|Izajasza|Jeremiasza|Lamentacje|Ezechiela|Daniela|Ozeasza|Joela|Amosa|Abdiasza|Jonasza|Micheasza|Nahuma|Habakuka|Sofoniasza|Aggeusza|Zachariasza|Malachiasza|Mateusza|Marka|Łukasza|Jana|Dzieje|Rzymian|I Koryntian|II Koryntian|Galacjan|Efezjan|Filipian|Kolosan|I Tesaloniczan|II Tesaloniczan|I Tymoteusza|II Tymoteusza|Tytusa|Filemona|Hebrajczyków|Jakuba|I Piotra|II Piotra|I Jana|II Jana|III Jana|Judy|Objawienie",
            analyzerFactory = { MorfologikAnalyzer() }
        )

        val uk = Language(
            code = "uk",
            englishName = "Ukrainian",
            nativeName = "Українська",
            order = WESTERN + 10,
            bookNamesConcat = "Буття|Вихід|Левит|Числа|Повторення Закону|Iсус Навин|Книга Суддiв|Рут|1-а Самуїлова|2-а Самуїлова|1-а царiв|2-а царiв|1-а хронiки|2-а хронiки|Ездра|Неемія|Естер|Йов|Псалми|Приповiстi|Екклезiяст|Пiсня над пiснями|Iсая|Єремiя|Плач Єремiї|Єзекiїль|Даниїл|Осiя|Йоїл|Амос|Овдiй|Йона|Михей|Наум|Авакум|Софонiя|Огiй|Захарiя|Малахiї|Вiд Матвiя|Вiд Марка|Вiд Луки|Вiд Iвана|Дiї|До римлян|1-е до коринтян|2-е до коринтян|До галатiв|До ефесян|До филип'ян|До колоссян|1-е до солунян|2-е до солунян|1-е Тимофiю|2-е Тимофiю|До Тита|До Филимона|До євреїв|Якова|1-е Петра|2-е Петра|1-е Iвана|2-е Iвана|3-е Iвана|Юда|Об'явлення",
            analyzerFactory = { UkrainianMorfologikAnalyzer() }
        )

        val sv = Language(
            code = "sv",
            englishName = "Swedish",
            nativeName = "Svenska",
            order = WESTERN + 11,
            bookNamesConcat = "1 Mosebok|2 Mosebok|3 Mosebok|4 Mosebok|5 Mosebok|Josua|Domarboken|Rut|1 Samuelsboken|2 Samuelsboken|1 Kungaboken|2 Kungaboken|1 Krönikeboken|2 Krönikeboken|Esra|Nehemja|Ester|Job|Psaltaren|Ordspråksboken|Predikaren|Höga Visan|Jesaja|Jeremia|Klagovisorna|Hesekiel|Daniel|Hosea|Joel|Amos|Obadja|Jona|Mika|Nahum|Habackuk|Sefanja|Haggai|Sakarja|Malaki|Matteus|Markus|Lukas|Johannes|Apostlagärningarna|Romarbrevet|1 Korinthierbrevet|2 Korinthierbrevet|Galaterbrevet|Efesierbrevet|Filipperbrevet|Kolosserbrevet|1 Thessalonikerbreve|2 Thessalonikerbreve|1 Timotheosbrevet|2 Timotheosbrevet|Titusbrevet|Filemonbrevet|Hebreerbrevet|Jakobsbrevet|1 Petrusbrevet|2 Petrusbrevet|1 Johannesbrevet|2 Johannesbrevet|3 Johannesbrevet|Judasbrevet|Uppenbarelseboken",
            analyzerFactory = { SwedishAnalyzer() }
        )

        val zh = Language(
            code = "zh",
            customScriptCode = "Hans",
            englishName = "Chinese",
            nativeName = "中文",
            isCJK = true,
            order = EAST_ASIA + 1,
            bookNamesConcat = "创世记|出埃及|利未记|民数记|申命记|约书亚记|士师记|路得记|撒母耳记上|撒母耳记下|列王纪上|列王纪下|历代志上|历代志下|以斯拉记|尼希米记|以斯帖记|约伯记|诗篇|箴言|传道书|雅歌|以赛亚书|耶利米书|耶利米哀歌|以西结书|但以理书|何西阿书|约珥书|阿摩司书|俄巴底亚书|约拿书|弥迦书|那鸿书|哈巴谷书|西番雅书|哈该书|撒迦利亚书|玛拉基书|马太福音|马可福音|路加福音|约翰福音|使徒行传|罗马书|哥林多前书|哥林多后书|加拉太书|以弗所书|腓立比书|歌罗西书|帖撒罗尼迦前书|帖撒罗尼迦后书|提摩太前书|提摩太后书|提多书|腓利门书|希伯来书|雅各书|彼得前书|彼得后书|约翰一书|约翰二书|约翰三书|犹大书|启示录",
            analyzerFactory = { SmartChineseAnalyzer() }
        )

        val zht = Language(
            code = "zht",
            customScriptCode = "Hant",
            englishName = "Chinese Traditional",
            nativeName = "繁體中文",
            order = EAST_ASIA + 2,
            bookNamesConcat = "創世記|出埃及記|利未記|民數記|申命記|約書亞記|士師記|路得記|撒母耳記上|撒母耳記下|列王紀上|列王紀下|歷代志上|歷代志下|以斯拉記|尼希米記|以斯帖記|約伯記|詩篇|箴言|傳道書|雅歌|以賽亞書|耶利米書|耶利米哀歌|以西結書|但以理書|何西阿書|約珥書|阿摩司書|俄巴底亞書|約拿書|彌迦書|那鴻書|哈巴谷書|西番雅書|哈該書|撒迦利亞書|瑪拉基書|馬太福音|馬可福音|路加福音|約翰福音|使徒行傳|羅馬書|哥林多前書|哥林多後書|加拉太書|以弗所書|腓立比書|歌羅西書|帖撒羅尼迦前書|帖撒羅尼迦後書|提摩太前書|提摩太後書|提多書|腓利門書|希伯來書|雅各書|彼得前書|彼得後書|約翰一書|約翰二書|約翰三書|猶大書|啟示錄"
        )

        val ko = Language(
            code = "ko",
            englishName = "Korean",
            nativeName = "한국어",
            isCJK = true,
            order = EAST_ASIA + 3,
            bookNamesConcat = "창세기|출애굽기|레위기|민수기|신명기|여호수아|사사기|룻기|사무엘상|사무엘하|열왕기상|열왕기하|역대상|역대하|에스라|느헤미야|에스더|욥기|시편|잠언|전도서|아가|이사야|예레미야|예레미야애가|에스겔|다니엘|호세아|요엘|아모스|오바댜|요나|미가|나훔|하박국|스바냐|학개|스가랴|말라기|마태복음|마가복음|누가복음|요한복음|사도행전|로마서|고린도전서|고린도후서|갈라디아서|에베소서|빌립보서|골로새서|데살로니가전서|데살로니가후서|디모데전서|디모데후서|디도서|빌레몬서|히브리서|야고보서|베드로전서|베드로후서|요한1서|요한2서|요한3서|유다서|요한계시록",
            analyzerFactory = { KoreanAnalyzer() }
        )

        /**
         * Japanese book names differs for each Bible translations, but only public domain modern Japanese version is jc, so we use book names of jc here as default.
         */
        val ja = Language(
            code = "ja",
            englishName = "Japanese",
            nativeName = "日本語",
            isCJK = true,
            order = EAST_ASIA + 4,
            bookNamesConcat = "創世記|出エジプト記|レビ記|民数記|申命記|ヨシュア記|士師記|ルツ記|サムエル記上|サムエル記下|列王記上|列王記下|歴代誌上|歴代誌下|エズラ記|ネヘミヤ記|エステル記|ヨブ記|詩篇|箴言|伝道の書|雅歌|イザヤ書|エレミヤ書|哀歌|エゼキエル書|ダニエル書|ホセア書|ヨエル書|アモス書|オバデヤ書|ヨナ書|ミカ書|ナホム書|ハバクク書|ゼパニヤ書|ハガイ書|ゼカリヤ書|マラキ書|マタイによる福音書|マルコによる福音書|ルカによる福音書|ヨハネによる福音書|使徒行伝|ローマ人への手紙|コリント人への第一の手紙|コリント人への第二の手紙|ガラテヤ人への手紙|エペソ人への手紙|ピリピ人への手紙|コロサイ人への手紙|テサロニケ人への第一の手紙|テサロニケ人への第二の手紙|テモテへの第一の手紙|テモテへの第二の手紙|テトスへの手紙|ピレモンへの手紙|ヘブル人への手紙|ヤコブの手紙|ペテロの第一の手紙|ペテロの第二の手紙|ヨハネの第一の手紙|ヨハネの第二の手紙|ヨハネの第三の手紙|ユダの手紙|ヨハネの黙示録",
            analyzerFactory = { JapaneseAnalyzer() }
        )

        val embeddedLanguages = arrayOf(en, es, pt, de, fr, ru, nl, it, pl, uk, sv, zh, /* zht (is not included for now) ,*/ ko, ja)

        val tl = Language(
            code = "tl",
            englishName = "Tagalog",
            nativeName = "Tagalog",
            order = SOUTH_EAST_ASIA + 1,
            bookNamesConcat = "GENESIS|EXODO|LEVITICO|MGA BILANG|DEUTERONOMIO|JOSUE|MGA HUKOM|RUTH|I SAMUEL|II SAMUEL|I MGA HARI|II MGA HARI|I MGA CRONICA|II MGA CRONICA|EZRA|NEHEMIAS|ESTHER|JOB|MGA AWIT|MGA KAWIKAAN|ECLESIASTES|ANG AWIT NG MGA AWIT|ISAIAS|JEREMIAS|MGA PANAGHOY|EZEKIEL|DANIEL|OSEAS|JOEL|AMOS|OBADIAS|JONAS|MIKAS|NAHUM|HABACUC|ZEFANIAS|HAGAI|ZACARIAS|MALAKIAS|MATEO|MARCOS|LUCAS|JUAN|ANG MGA GAWA|MGA TAGA ROMA|I MGA TAGA CORINTO|II MGA TAGA CORINTO|GALACIA|EFESO|FILIPOS|COLOSAS|I MGA TAGA TESALONICA|II MGA TAGA TESALONICA|I TIMOTEO|II TIMOTEO|TITO|FILEMON|MGA HEBREO|SANTIAGO|I PEDRO|II PEDRO|I JUAN|II JUAN|III JUAN|JUDAS|APOCALIPSIS",
            analyzerFactory = { TagalogAnalyzer() }
        )

        val id = Language(
            code = "id",
            englishName = "Indonesian",
            nativeName = "Bahasa Indonesia",
            order = SOUTH_EAST_ASIA + 2,
            bookNamesConcat = "Kejadian|Keluaran|Imamat|Bilangan|Ulangan|Yosua|Hakim-hakim|Rut|1 Samuel|2 Samuel|1 Raja-raja|2 Raja-raja|1 Tawarikh|2 Tawarikh|Ezra|Nehemia|Ester|Ayub|Mazmur|Amsal|Pengkhotbah|Kidung Agung|Yesaya|Yeremia|Ratapan|Yehezkiel|Daniel|Hosea|Y\\\"oel|Amos|Obaja|Yunus|Mikha|Nahum|Habakuk|Zefanya|Hagai|Zakharia|Maleakhi|Matius|Markus|Lukas|Yohanes|Kisah Para Rasul|Roma|1 Korintus|2 Korintus|Galatia|Efesus|Filipi|Kolose|1 Tesalonika|2 Tesalonika|1 Timotius|2 Timotius|Titus|Filemon|Ibrani|Yakobus|1 Petrus|2 Petrus|1 Yohanes|2 Yohanes|3 Yohanes|Yudas|Wahyu",
            analyzerFactory = { IndonesianAnalyzer() }
        )

        val vi = Language(
            code = "vi",
            englishName = "Vietnamese",
            nativeName = "Tiếng Việt",
            order = SOUTH_EAST_ASIA + 3,
            bookNamesConcat = "Sáng-thế Ký|Xuất Ê-díp-tô Ký|Lê-vi Ký|Dân-số Ký|Phục-truyền Luật-lệ Ký|Giô-suê|Các Quan Xét|Ru-tơ|I Sa-mu-ên|II Sa-mu-ên|I Các Vua|II Các Vua|I Sử-ký|II Sử-ký|E-xơ-ra|Nê-hê-mi|Ê-xơ-tê|Gióp|Thi-thiên|Châm-ngôn|Truyền-đạo|Nhã-ca|Ê-sai|Giê-rê-mi|Ca thương|Ê-xê-chi-ên|Đa-ni-ên|Ô-sê|Giô-ên|A-mốt|Áp-đia|Giô-na|Mi-chê|Na-hum|Ha-ba-cúc|Sô-phô-ni|A-ghê|Xa-cha-ri|Ma-la-chi|Ma-thi-ơ|Mác|Lu-ca|Giăng|Công-vụ|Rô-ma|I Cô-rinh-tô|II Cô-rinh-tô|Ga-la-ti|Ê-phê-sô|Phi-líp|Cô-lô-se|I Tê-sa-lô-ni-ca|II Tê-sa-lô-ni-ca|I Ti-mô-thê|II Ti-mô-thê|Tít|Phi-lê-môn|Hê-bơ-rơ|Gia-cơ|I Phi-e-rơ|II Phi-e-rơ|I Giăng|II Giăng|III Giăng|Giu-đe|Khải-huyền",
            analyzerFactory = { VietnameseAnalyzer(VietnameseConfig()) }
        )

        val th = Language(
            code = "th",
            englishName = "Thai",
            nativeName = "ไทย",
            order = SOUTH_EAST_ASIA + 4,
            bookNamesConcat = "ปฐมกาล|อพยพ|เลวีนิติ|กันดารวิถี|เฉลยธรรมบัญญัติ|โยชูวา|ผู้วินิจฉัย|นางรูธ|1 ซามูเอล|2 ซามูเอล|1 พงศ์กษัตริย์|2 พงศ์กษัตริย์|1 พงศาวดาร|2 พงศาวดาร|เอสรา|เนหะมีย์|เอสเธอร์|โยบ|สดุดี|สุภาษิต|ปัญญาจารย์|เพลงซาโลมอน|อิสยาห์|เยเรมีย์|เพลงคร่ำครวญ|เอเสเคียล|ดาเนียล|โฮเชยา|โยเอล|อาโมส|โอบาดีห์|โยนาห์|มีคาห์|นาฮูม|ฮาบากุก|เศฟันยาห์|ฮักกัย|เศคาริยาห์|มาลาคี|มัทธิว|มาระโก|ลูกา|ยอห์น|กิจการ|โรม|1 โครินธ์|2 โครินธ์|กาลาเทีย|เอเฟซัส|ฟีลิปปี|โคโลสี|1 เธสะโลนิกา|2 เธสะโลนิกา|1 ทิโมธี|2 ทิโมธี|ทิตัส|ฟีเลโมน|ฮีบรู|ยากอบ|1 เปโตร|2 เปโตร|1 ยอห์น|2 ยอห์น|3 ยอห์น|ยูดา|วิวรณ์",
            analyzerFactory = { ThaiAnalyzer() }
        )

        val hi =  Language(
            code = "hi",
            englishName = "Hindi",
            nativeName = "हिन्दी",
            order = SOUTH_ASIA + 1,
            bookNamesConcat = "उत्पत्ति|निर्गमन|लैव्यव्यवस्था|गिनती|व्यवस्थाविवरण|यहोशू|न्यायियों|रूत|1 शमूएल|2 शमूएल|1 राजाओं|2 राजाओं|1 इतिहास|2 इतिहास|एज्रा|नहेम्याह|एस्तेर|अय्यूब|भजन संहिता|नीतिवचन|सभोपदेशक|श्रेष्ठगीत|यशायाह|यिर्मयाह|विलापगीत|यहेजकेल|दानिय्येल|होशे|योएल|आमोस|ओबद्याह|योना|मीका|नहूम|हबक्कूक|सपन्याह|हाग्गै|जकर्याह|मलाकी|मत्ती|मरकुस|लूका|यूहन्ना|प्रेरितों के काम|रोमियों|1 कुरिन्थियों|2 कुरिन्थियों|गलातियों|इफिसियों|फिलिप्पियों|कुलुस्सियों|1 थिस्सलुनीकियों|2 थिस्सलुनीकियों|1 तीमुथियुस|2 तीमुथियुस|तीतुस|फिलेमोन|इब्रानियों|याकूब|1 पतरस|2 पतरस|1 यूहन्ना|2 यूहन्ना|3 यूहन्ना|यहूदा|प्रकाशितवाक्य",
            analyzerFactory = { HindiAnalyzer() }
        )

        val bn = Language(
            code = "bn",
            englishName = "Bengali",
            nativeName = "বাংলা",
            order = SOUTH_ASIA + 2,
            bookNamesConcat = "আদিপুস্তক|যাত্রাপুস্তক|লেবীয়|গণনা|দ্বিতীয় বিবরণ|যিহোশূয়|বিচারকর্ত্তৃগণ|রূত|1 শমূয়েল|2 শমূয়েল|1 রাজাবলি|2 রাজাবলি|1 বংশাবলি|2 বংশাবলি|ইষ্রা|নহিমিয়|ইষ্টের|ইয়োব|গীতসংহিতা|হিতোপদেশ|উপদেশক|পরমগীত|যিশাইয়|যিরমিয়|বিলাপ|যিহিস্কেল|দানিয়েল|হোশেয়|যোয়েল|আমোষ|ওবদিয়|যোনা|মীখা|নহূম|হবককূক|সফনিয়|হগয়|সখরিয়|মালাখি|মথি|মার্ক|লুক|যোহন|প্রেরিতদের কার্য্য|রোমীয়|1 করিন্থীয়|2 করিন্থীয়|গালাতীয়|ইফিষীয়|ফিলিপীয়|কলসীয়|1 থিষলনীকীয়|2 থিষলনীকীয়|1 তীমথিয়|2 তীমথিয়|তীত|ফিলীমন|ইব্রীয়|যাকোব|1 পিতর|2 পিতর|1 যোহন|2 যোহন|3 যোহন|যিহূদা|প্রকাশিত বাক্য",
            analyzerFactory = { BengaliAnalyzer() }
        )

        val mr = Language(
            code = "mr",
            englishName = "Marathi",
            nativeName = "मराठी",
            order = SOUTH_ASIA + 3,
            bookNamesConcat = "उत्प.|निर्ग.|लेवी.|गण.|अनु.|यहो.|शास्ते|रूथ|1 शमु.|2 शमु.|1 राजे|2 राजे|1 इति.|2 इति.|एज्रा|नहे.|एस्ते.|ईयो.|स्तोत्र.|नीति.|उप.|गीत.|यश.|यिर्म.|विला.|यहे.|दानि.|होशे.|योए.|आमो.|ओब.|योना|मीखा|नहू.|हब.|सफ.|हाग्ग.|जख.|मला.|मत्त.|मार्क|लूक|योहा.|प्रेषि.|रोम.|1 करिं.|2 करिं.|गल.|इफि.|फिलि.|कल.|1 थेस्स.|2 थेस्स.|1 तीम.|2 तीम.|तीत.|फिले.|इब्री.|याको.|1 पेत्र.|2 पेत्र.|1 योहा.|2 योहा.|3 योहा.|यहू.|प्रक.",
            analyzerFactory = { MarathiAnalyzer() }
        )

        val te = Language(
            code = "te",
            englishName = "Telugu",
            nativeName = "తెలుగు",
            order = SOUTH_ASIA + 4,
            bookNamesConcat = "ఆది|నిర్గమ|లేవీ|సంఖ్యా|ద్వితీ|యెహో|న్యాయాధి|రూతు|1 సమూ|2 సమూ|1 రాజులు|2 రాజులు|1 దిన|2 దిన|ఎజ్రా|నెహెమ్యా|ఎస్తేరు|యోబు|కీర్తన|సామెత|ప్రసంగి|పరమ|యెషయా|యిర్మీయా|విలాప|యెహె|దాని|హోషే|యోవే|ఆమోసు|ఓబద్యా|యోనా|మీకా|నహూ|హబ|జెఫన్యా|హగ్గయి|జెకర్యా|మలాకీ|మత్తయి|మార్కు|లూకా|యోహాను|అపొస్తలుల కార్యములు|రోమా పత్రిక|1 కొరింతీ పత్రిక|2 కొరింతీ పత్రిక|గలతీ పత్రిక|ఎఫెసీ పత్రిక|ఫిలిప్పీ పత్రిక|కొలస్సీ పత్రిక|1 తెస్సలోనిక పత్రిక|2 తెస్సలోనిక పత్రిక|1 తిమోతి పత్రిక|2 తిమోతి పత్రిక|తీతు పత్రిక|ఫిలేమోను పత్రిక|హెబ్రీ పత్రిక|యాకోబు పత్రిక|1 పేతురు పత్రిక|2 పేతురు పత్రిక|1 యోహాను పత్రిక|2 యోహాను పత్రిక|3 యోహాను పత్రిక|యూదా పత్రిక|ప్రకటన గ్రంథం",
            analyzerFactory = { TeluguAnalyzer() }
        )

        val ta = Language(
            code = "ta",
            englishName = "Tamil",
            nativeName = "தமிழ்",
            order = SOUTH_ASIA + 5,
            bookNamesConcat = "ஆதி|யாத்|லேவி|எண்|உபா|யோசு|நியா|ரூத்|1 சாமு|2 சாமு|1 இராஜா|2 இராஜா|1 நாளா|2 நாளா|எஸ்றா|நெகே|எஸ்த|யோபு|சங்|நீதி|பிரச|உன்|ஏசா|எரே|புலம்|எசேக்|தானி|ஓசியா|யோவேல்|ஆமோ|ஒபதி|யோனா|மீகா|நாகூ|ஆப|செப்ப|ஆகாய்|சகரி|மல்கி|மத்|மாற்|லூக்|யோவா|அப்|ரோமர்|1 கொரி|2 கொரி|கலா|எபே|பிலி|கொலோ|1 தெச|2 தெச|1 தீமோ|2 தீமோ|தீத்|பிலே|எபி|யாக்|1 பேது|2 பேது|1 யோவா|2 யோவா|3 யோவா|யூதா|வெளி",
            analyzerFactory = { TamilAnalyzer() }
        )

        val gu = Language(
            code = "gu",
            englishName = "Gujarati",
            nativeName = "ગુજરાતી",
            order = SOUTH_ASIA + 6,
            bookNamesConcat = "ઉત્પ|નિર્ગ.|લેવી|ગણ.|પુન.|યહો.|ન્યાય.|રૂથ|1 શમુ.|2 શમુ.|1 રાજા.|2 રાજા.|1 કાળ.|2 કાળ.|એઝ.|નહે.|એસ્ત.|અયૂ.|ગી.શા.|નીતિ.|સભા.|ગીત.|યશા.|યર્મિ.|વિલા.|હઝ.|દાનિ.|હોશિ.|યોએ.|આમ.|ઓબ.|યૂન.|મીખા.|નાહૂ.|હબ.|સફા.|હાગ.|ઝખા.|માલા.|માથ.|માર્ક|લૂક|યોહ.|પ્રે.કૃ.|રોમ.|1 કરિં.|2 કરિં.|ગલ.|એફે.|ફિલિ.|કલો.|1 થેસ.|2 થેસ.|1 તિમ.|2 તિમ.|તિત.|ફિલે.|હિબ.|યાકૂ.|1 પિત.|2 પિત.|1 યોહ.|2 યોહ.|3 યોહ.|યહૂ.|પ્રક.",
            analyzerFactory = { GujaratiAnalyzer() }
        )

        val ur = Language(
            code = "ur",
            englishName = "Urdu",
            nativeName = "اردو",
            order = SOUTH_ASIA + 7,
            bookNamesConcat = "पैदाइश|ख़ुरु|अह|गिन|इस्त|यशो|क़ुजा|रुत|1 समु|2 समु|1 सला|2 सला|1 तवा|2 तवा|एज्रा|नहे|आस्त|अय्यू|ज़बूर|अम्सा|वाइज़|गज़लुल|यसा|यर्म|नोहा|हिज़ि|दानि|होसी|यूए|आमू|अब्द|यूना|मीका|नाहूम|हबक़्|सफ़न|हज्जी|ज़कर|मला|मत्त|मर|लूका|यूहन्ना|रसूलों|रोमि|1 कुरि|2 कुरि|गला|इफ़ि|फ़िलि|कुलु|1 थिस्स|2 थिस्स|1 तीमु|2 तीमु|तितु|फ़िले|इब्रा|या'क़ूब|1 पत|2 पत|1 यूह|2 यूह|3 यूह|यहू|मुका",
            analyzerFactory = { UrduAnalyzer() }
        )

        val ne = Language(
            code = "ne",
            englishName = "Nepali",
            nativeName = "नेपाली",
            order = SOUTH_ASIA + 8,
            bookNamesConcat = "उत्पत्ति|प्रस्थान|लेवी|गन्ती|व्यवस्था|यहोशू|न्यायकर्ता|रूथ|१ शमूएल|२ शमूएल|१ राजा|२ राजा|१ इतिहास|२ इतिहास|एज्रा|नहेम्याह|एस्तर|अय्यूब|भजनसंग्रह|हितोपदेश|उपदेशक|श्रेष्ठगीत|यशैया|यर्मिया|विलाप|इजकिएल|दानिएल|होशे|योएल|आमोस|ओबदिया|योना|मीका|नहूम|हबकूक|सपन्याह|हाग्गै|जकरिया|मलाकी|मत्ती|मर्कूस|लूका|यूहन्ना|प्रेरित|रोमी|१ कोरिन्थी|२ कोरिन्थी|गलाती|एफिसी|फिलिप्पी|कलस्सी|१ थेसलोनिकी|२ थेसलोनिकी|१ तिमोथी|२ तिमोथी|तीतस|फिलेमोन|हिब्रू|याकूब|१ पत्रुस|२ पत्रुस|१ यूहन्ना|२ यूहन्ना|३ यूहन्ना|यहूदा|प्रकाश",
            analyzerFactory = { NepaliAnalyzer() }
        )

        val downloadableLanguages = arrayOf(hi, bn, mr, te, ta, gu, ur, vi, tl, ne, id, th, zht)
    }
}

fun String.toLanguage(): Language {
    for (language in Language.embeddedLanguages + Language.downloadableLanguages) {
        if (this == language.code) {
            return language
        }
    }
    throw IllegalArgumentException("Language code not found: $this")
}
