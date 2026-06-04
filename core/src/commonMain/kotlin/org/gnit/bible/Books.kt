package org.gnit.bible

/**
 * Created by Joel on 10/22/2014.
 */
object Books {
    fun maxChapter(book: Int): Int = when (book) {
        19 -> 150
        23 -> 66
        24 -> 52
        1 -> 50
        26 -> 48
        18 -> 42
        2 -> 40
        4, 14 -> 36
        5 -> 34
        9, 20 -> 31
        13 -> 29
        40, 44 -> 28
        3 -> 27
        12 -> 25
        6, 10, 42 -> 24
        11, 66 -> 22
        7, 43 -> 21
        41, 45, 46 -> 16
        28, 38 -> 14
        16, 47, 58 -> 13
        21, 27 -> 12
        15, 17 -> 10
        30 -> 9
        22 -> 8
        33 -> 7
        48, 49, 54 -> 6
        25, 52, 59, 60, 62 -> 5
        8, 32, 39, 50, 51, 55 -> 4
        29, 34, 35, 36, 53, 56, 61 -> 3
        37 -> 2
        31, 57, 63, 64, 65 -> 1
        else -> 50
    }

    enum class Category(val key: List<String>, val filter: BibleFilter) {
        OLD_TESTAMENT(listOf("ot", "old testament"), books(1..39)),
        TORAH(listOf("t", "tor", "torah", "pe", "pent", "pentateuch"), books(1..5)),
        ABRAHAM(listOf("abraham"), passage(BookChapterVerse(1, 11, 27), BookChapterVerse(1, 25, 11))),
        ISAAC(listOf("isaac"), passage(BookChapterVerse(1, 21, 1), BookChapterVerse(1, 35, 29))),
        JACOB(listOf("jacob"), passage(BookChapterVerse(1, 25, 19), BookChapterVerse(1, 49, 33))),
        JOSEPH(listOf("joseph"), passage(BookChapterVerse(1, 37, 2), BookChapterVerse(1, 50, 26))),
        HISTORICAL_BOOKS(listOf("h"), books(6..17)),
        SAMUEL(listOf("sam", "samuel"), books(9..10)),
        DAVID(listOf("david"), passage(BookChapterVerse(9, 16, 1), BookChapterVerse(11, 2, 12))),
        KINGS(listOf("ki", "kings"), books(11..12)),
        CHRONICLES(listOf("chr", "chro", "chronicles"), books(13..14)),
        WISDOM_BOOKS(listOf("w", "wis", "wisdom"), books(18..22)),
        PROPHETS(listOf("p", "prophet", "prophets", "profets"), books(23..39)),
        MAJOR_PROPHETS(listOf("map", "major", "major prophet", "major prophets"), books(23..27)),
        MINOR_PROPHETS(listOf("mip", "minor", "minor prophet", "minor prophets"), books(28..39)),
        NEW_TESTAMENT(listOf("nt", "new testament"), books(40..66)),
        GOSPELS(listOf("g", "go", "gospel", "gospels"), books(40..43)),
        SYNOPTIC_GOSPELS(listOf("sg", "synoptic", "synoptic gospel", "synoptic gospels"), books(40..42)),
        PAULINE_EPISTLES(
            listOf(
                "paul", "pauline", "pauline epi", "pauline epistle", "pauline episodes",
                "letter of paul", "letters of paul", "paul's letter", "paul's letters"
            ),
            books(45..57)
        ),
        CORINTHIANS(listOf("cor", "corinthians", "epistle to the corinthians", "letter to the corinthians"), books(46..47)),
        THESSALONIANS(listOf("thes", "thessalonians", "epistle to the thessalonians", "letter to the thessalonians"), books(52..53)),
        TIMOTHY(listOf("tim", "thimothy", "epistle to thimothy", "letter to thimothy"), books(54..55)),
        PETER(listOf("peter", "pet", "epistle of peter", "epistles of peter", "letter of peter", "letters of peter"), books(60, 61)),
        JOHN_LETTERS(listOf("johnl", "johne", "johns letter", "johns letters", "johnsletter", "john letter", "johnletter", "epistle of john", "epistles of john", "letter of john", "letters of john"), books(62, 63, 64)),
        JOHANNINE(listOf("johnw", "john writing", "johns writings", "john writings", "johannine"), books(43, 62, 63, 64, 66)),
        ALL(listOf("all"), BibleFilter.All),
        ;

        companion object {
            fun fromKey(raw: String): Category? {
                val key = raw.trim().lowercase()
                return entries.firstOrNull { key in it.key }
            }

            fun filterOf(key: String): BibleFilter {
                return fromKey(key)!!.filter
            }

            fun filterOrNull(key: String): BibleFilter? {
                return fromKey(key)?.filter
            }

            fun resolveOrThrow(
                rawKey: String,
                errorFactory: (String) -> Throwable
            ): BibleFilter {
                return filterOrNull(rawKey)
                    ?: throw errorFactory(rawKey)
            }

            fun resolveAllOrThrow(
                rawKeys: Iterable<String>,
                errorFactory: (String) -> Throwable
            ): List<BibleFilter> {
                return rawKeys.map { rawKey -> resolveOrThrow(rawKey, errorFactory) }
            }
        }
    }

    fun filterByBookChapter(term: String): BookChapterFilter {

        bookNameNumberArray.forEachIndexed { bookNumber, bookNames ->
            bookNames.forEach { bookName ->
                if (term.endsWith("in $bookName")) {
                    val trimmed = term.replace(" in $bookName", "").trim()
                    return BookChapterFilter(book = bookNumber, term = trimmed)
                }
            }
        }

        bookNameNumberArray.forEachIndexed { bookNumber, bookNames ->
            bookNames.forEach { bookName ->
                if (term.matches(".+ in $bookName ([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex())) {

                    val detectedChapter = term.split("in $bookName ")[1].toInt()
                    val trimmed = term.replace(" in $bookName ([1-9]|[1-9][0-9]|1[0-5][0-9])\$".toRegex(), "").trim()

                    return BookChapterFilter(book = bookNumber, startChapter = detectedChapter, term = trimmed)
                }
            }
        }

        bookNameNumberArray.forEachIndexed { bookNumber, bookNames ->
            bookNames.forEach { bookName ->
                if (term.matches(".+ in $bookName ([1-9]|[1-9][0-9]|1[0-5][0-9])-([1-9]|[1-9][0-9]|1[0-5][0-9])$".toRegex())) {
                    val detectedChapters = term.split("in $bookName ")[1].split("-")

                    val trimmed = term.replace(
                        " in $bookName ([1-9]|[1-9][0-9]|1[0-5][0-9])-([1-9]|[1-9][0-9]|1[0-5][0-9])\$".toRegex(),
                        ""
                    ).trim()

                    return BookChapterFilter(
                        book = bookNumber,
                        startChapter = detectedChapters[0].toInt(),
                        endChapter = detectedChapters[1].toInt(),
                        term = trimmed
                    )
                }
            }
        }

        return BookChapterFilter(term = term)
    }

    fun bookNumber(bookName: String) = when (bookName) {
        "genesis", "gen", "ge", "gn" -> 1
        "exodus", "ex", "exod", "exo" -> 2
        "leviticus", "lev", "le", "lv" -> 3
        "numbers", "num", "nu", "nm", "nb" -> 4
        "deuteronomy", "deut", "de", "dt" -> 5
        "joshua", "josh", "jos", "jsh" -> 6
        "judges", "judg", "jdg", "jg", "jdgs" -> 7
        "ruth", "rth", "ru" -> 8
        "1st samuel", "1 sam", "1sam", "1sm", "1sa", "1s", "1 samuel", "1samuel", "1st sam", "first samuel", "first sam" -> 9
        "2nd samuel", "2 sam", "2sam", "2sm", "2sa", "2s", "2 samuel", "2ndsam", "2nd sam", "second samuel", "second sam" -> 10
        "1st kings", "1kings", "1 kings", "1kgs", "1 kgs", "1ki", "1k", "1stkgs", "first kings", "first kgs" -> 11
        "2nd kings", "2kings", "2 kings", "2kgs", "2 kgs", "2ki", "2k", "2ndkgs", "second kings", "second kgs" -> 12
        "1st chronicles", "1chronicles", "1 chronicles", "1chr", "1 chr", "1ch", "1stchr", "1st chr", "first chronicles", "first chr" -> 13
        "2nd chronicles", "2chronicles", "2 chronicles", "2chr", "2 chr", "2ch", "2ndchr", "2nd chr", "second chronicles", "second chr" -> 14
        "ezra", "ezr", "ez" -> 15
        "nehemiah", "neh", "ne" -> 16
        "esther", "est", "esth", "es" -> 17
        "job", "jb" -> 18
        "psalms", "ps", "psalm", "pslm", "psa", "psm", "pss" -> 19
        "proverbs", "prov", "pro", "prv", "pr" -> 20
        "ecclesiastes", "eccles", "eccle", "ecc", "ec", "qoh" -> 21
        "song of solomon", "song", "song of songs", "sos", "so", "canticle of canticles", "canticles", "cant" -> 22
        "isaiah", "isa", "is" -> 23
        "jeremiah", "jer", "je", "jr" -> 24
        "lamentations", "lam", "la" -> 25
        "ezekiel", "ezek", "eze", "ezk" -> 26
        "daniel", "dan", "da", "dn" -> 27
        "hosea", "hos", "ho" -> 28
        "joel", "jl" -> 29
        "amos", "am" -> 30
        "obadiah", "obad", "ob" -> 31
        "jonah", "jnh", "jon" -> 32
        "micah", "mic", "mc" -> 33
        "nahum", "nah", "na" -> 34
        "habakkuk", "hab", "hb" -> 35
        "zephaniah", "zeph", "zep", "zp" -> 36
        "haggai", "hag", "hg" -> 37
        "zechariah", "zech", "zec", "zc" -> 38
        "malachi", "mal", "ml" -> 39
        "matthew", "matt", "mt" -> 40
        "mark", "mrk", "mar", "mk", "mr" -> 41
        "luke", "luk", "lk" -> 42
        "john", "joh", "jhn", "jn" -> 43
        "acts", "act", "ac" -> 44
        "romans", "rom", "ro", "rm" -> 45
        "1 corinthians", "1corinthians", "1 cor", "1cor", "1 co", "1co", "1st corinthians", "first corinthians" -> 46
        "2 corinthians", "2corinthians", "2 cor", "2cor", "2 co", "2co", "2nd corinthians", "second corinthians" -> 47
        "galatians", "gal", "ga" -> 48
        "ephesians", "eph", "ephes" -> 49
        "philippians", "phil", "php", "pp" -> 50
        "colossians", "col", "co" -> 51
        "1 thessalonians", "1thessalonians", "1 thess", "1thess", "1 thes", "1thes", "1 th", "1th", "1st thessalonians", "1st thess", "first thessalonians", "first thess" -> 52
        "2 thessalonians", "2thessalonians", "2 thess", "2thess", "2 thes", "2thes", "2 th", "2th", "2nd thessalonians", "2nd thess", "second thessalonians", "second thess" -> 53
        "1 timothy", "1timothy", "1 tim", "1tim", "1 ti", "1ti", "1st timothy", "1st tim", "first timothy", "first tim" -> 54
        "2 timothy", "2timothy", "2 tim", "2tim", "2 ti", "2ti", "2nd timothy", "2nd tim", "second timothy", "second tim" -> 55
        "titus", "tit", "ti" -> 56
        "philemon", "philem", "phm", "pm" -> 57
        "hebrews", "heb" -> 58
        "james", "jas", "jm" -> 59
        "1 peter", "1peter", "1 pet", "1pet", "1 pe", "1pe", "1 pt", "1pt", "1p", "1st peter", "first peter" -> 60
        "2 peter", "2peter", "2 pet", "2pet", "2 pe", "2pe", "2 pt", "2pt", "2p", "2nd peter", "second peter" -> 61
        "1 john", "1john", "1 jhn", "1jhn", "1 jn", "1jn", "1j", "1st john", "first john" -> 62
        "2 john", "2john", "2 jhn", "2jhn", "2 jn", "2jn", "2j", "2nd john", "second john" -> 63
        "3 john", "3john", "3 jhn", "3jhn", "3 jn", "3jn", "3j", "3rd john", "third john" -> 64
        "jude", "jud", "jd" -> 65
        "revelation", "rev", "re", "the revelation" -> 66
        else -> throw RuntimeException("the book name $bookName is not supported")
    }

    fun bookNameEnglish(bookNumber: Int) = numberToName[bookNumber]

    fun bookNameEnglishCapital(bookNumber: Int): String {
        val lowerCase = bookNameEnglish(bookNumber)!!
        val split = lowerCase.split(" ")
        return when (split.size) {
            1 -> lowerCase.replaceFirstChar { it.uppercase() }
            2 -> "${split[0]} ${split[1].replaceFirstChar { it.uppercase() }}"
            3 -> "Song of Solomon"
            else -> throw RuntimeException("book name must be less than 3 words")
        }
    }

    fun bookNameFor(bookNumber: Int, translation: Translation): String {
        return arrayOf("chapterZero").plus(translation.language.bookNames())[bookNumber]
    }

    fun formatHeader(pointer: VersePointer): String {
        val bookName = arrayOf("chapterZero").plus(pointer.translation.language.bookNames())[pointer.book]
        val chapter = pointer.chapter
        val startVerse = pointer.startVerse
        val endVerse = pointer.endVerse

        if (startVerse == null) return "$bookName $chapter"
        if (endVerse != null && endVerse != startVerse) return "$bookName $chapter:$startVerse-$endVerse"
        return "$bookName $chapter:$startVerse"
    }

    private val numberToName = mapOf(
        1 to "genesis",
        2 to "exodus",
        3 to "leviticus",
        4 to "numbers",
        5 to "deuteronomy",
        6 to "joshua",
        7 to "judges",
        8 to "ruth",
        9 to "1st samuel",
        10 to "2nd samuel",
        11 to "1st kings",
        12 to "2nd kings",
        13 to "1st chronicles",
        14 to "2nd chronicles",
        15 to "ezra",
        16 to "nehemiah",
        17 to "esther",
        18 to "job",
        19 to "psalms",
        20 to "proverbs",
        21 to "ecclesiastes",
        22 to "song of solomon",
        23 to "isaiah",
        24 to "jeremiah",
        25 to "lamentations",
        26 to "ezekiel",
        27 to "daniel",
        28 to "hosea",
        29 to "joel",
        30 to "amos",
        31 to "obadiah",
        32 to "jonah",
        33 to "micah",
        34 to "nahum",
        35 to "habakkuk",
        36 to "zephaniah",
        37 to "haggai",
        38 to "zechariah",
        39 to "malachi",
        40 to "matthew",
        41 to "mark",
        42 to "luke",
        43 to "john",
        44 to "acts",
        45 to "romans",
        46 to "1 corinthians",
        47 to "2 corinthians",
        48 to "galatians",
        49 to "ephesians",
        50 to "philippians",
        51 to "colossians",
        52 to "1 thessalonians",
        53 to "2 thessalonians",
        54 to "1 timothy",
        55 to "2 timothy",
        56 to "titus",
        57 to "philemon",
        58 to "hebrews",
        59 to "james",
        60 to "1 peter",
        61 to "2 peter",
        62 to "1 john",
        63 to "2 john",
        64 to "3 john",
        65 to "jude",
        66 to "revelation"
    )

    private val bookNameNumberArray = arrayOf(
        emptyArray(), //no book name for book number 0
        arrayOf("genesis", "gen", "ge", "gn"), // index 1 as book 1
        arrayOf("exodus", "ex", "exod", "exo"),
        arrayOf("leviticus", "lev", "le", "lv"),
        arrayOf("numbers", "num", "nu", "nm", "nb"),
        arrayOf("deuteronomy", "deut", "de", "dt"),
        arrayOf("joshua", "josh", "jos", "jsh"),
        arrayOf("judges", "judg", "jdg", "jg", "jdgs"),
        arrayOf("ruth", "rth", "ru"),
        arrayOf("1st samuel", "1 sam", "1sam", "1sm", "1sa", "1s", "1 samuel", "1samuel", "1st sam", "first samuel", "first sam"),
        arrayOf("2nd samuel", "2 sam", "2sam", "2sm", "2sa", "2s", "2 samuel", "2ndsam", "2nd sam", "second samuel", "second sam"),
        arrayOf("1st kings", "1kings", "1 kings", "1kgs", "1 kgs", "1ki", "1k", "1stkgs", "first kings", "first kgs"),
        arrayOf("2nd kings", "2kings", "2 kings", "2kgs", "2 kgs", "2ki", "2k", "2ndkgs", "second kings", "second kgs"),
        arrayOf("1st chronicles", "1chronicles", "1 chronicles", "1chr", "1 chr", "1ch", "1stchr", "1st chr", "first chronicles", "first chr"),
        arrayOf("2nd chronicles", "2chronicles", "2 chronicles", "2chr", "2 chr", "2ch", "2ndchr", "2nd chr", "second chronicles", "second chr"),
        arrayOf("ezra", "ezr", "ez"),
        arrayOf("nehemiah", "neh", "ne"),
        arrayOf("esther", "est", "esth", "es"),
        arrayOf("job", "jb"),
        arrayOf("psalms", "ps", "psalm", "pslm", "psa", "psm", "pss"),
        arrayOf("proverbs", "prov", "pro", "prv", "pr"),
        arrayOf("ecclesiastes", "eccles", "eccle", "ecc", "ec", "qoh"),
        arrayOf("song of solomon", "song", "song of songs", "sos", "so", "canticle of canticles", "canticles", "cant"),
        arrayOf("isaiah", "isa", "is"),
        arrayOf("jeremiah", "jer", "je", "jr"),
        arrayOf("lamentations", "lam", "la"),
        arrayOf("ezekiel", "ezek", "eze", "ezk"),
        arrayOf("daniel", "dan", "da", "dn"),
        arrayOf("hosea", "hos", "ho"),
        arrayOf("joel", "jl"),
        arrayOf("amos", "am"),
        arrayOf("obadiah", "obad", "ob"),
        arrayOf("jonah", "jnh", "jon"),
        arrayOf("micah", "mic", "mc"),
        arrayOf("nahum", "nah", "na"),
        arrayOf("habakkuk", "hab", "hb"),
        arrayOf("zephaniah", "zeph", "zep", "zp"),
        arrayOf("haggai", "hag", "hg"),
        arrayOf("zechariah", "zech", "zec", "zc"),
        arrayOf("malachi", "mal", "ml"),
        arrayOf("matthew", "matt", "mt"),
        arrayOf("mark", "mrk", "mar", "mk", "mr"),
        arrayOf("luke", "luk", "lk"),
        arrayOf("john", "joh", "jhn", "jn"),
        arrayOf("acts", "act", "ac"),
        arrayOf("romans", "rom", "ro", "rm"),
        arrayOf("1 corinthians", "1corinthians", "1 cor", "1cor", "1 co", "1co", "1st corinthians", "first corinthians"),
        arrayOf("2 corinthians", "2corinthians", "2 cor", "2cor", "2 co", "2co", "2nd corinthians", "second corinthians"),
        arrayOf("galatians", "gal", "ga"),
        arrayOf("ephesians", "eph", "ephes"),
        arrayOf("philippians", "phil", "php", "pp"),
        arrayOf("colossians", "col", "co"),
        arrayOf("1 thessalonians", "1thessalonians", "1 thess", "1thess", "1 thes", "1thes", "1 th", "1th", "1st thessalonians", "1st thess", "first thessalonians", "first thess"),
        arrayOf("2 thessalonians", "2thessalonians", "2 thess", "2thess", "2 thes", "2thes", "2 th", "2th", "2nd thessalonians", "2nd thess", "second thessalonians", "second thess"),
        arrayOf("1 timothy", "1timothy", "1 tim", "1tim", "1 ti", "1ti", "1st timothy", "1st tim", "first timothy", "first tim"),
        arrayOf("2 timothy", "2timothy", "2 tim", "2tim", "2 ti", "2ti", "2nd timothy", "2nd tim", "second timothy", "second tim"),
        arrayOf("titus", "tit", "ti"),
        arrayOf("philemon", "philem", "phm", "pm"),
        arrayOf("hebrews", "heb"),
        arrayOf("james", "jas", "jm"),
        arrayOf("1 peter", "1peter", "1 pet", "1pet", "1 pe", "1pe", "1 pt", "1pt", "1p", "1st peter", "first peter"),
        arrayOf("2 peter", "2peter", "2 pet", "2pet", "2 pe", "2pe", "2 pt", "2pt", "2p", "2nd peter", "second peter"),
        arrayOf("1 john", "1john", "1 jhn", "1jhn", "1 jn", "1jn", "1j", "1st john", "first john"),
        arrayOf("2 john", "2john", "2 jhn", "2jhn", "2 jn", "2jn", "2j", "2nd john", "second john"),
        arrayOf("3 john", "3john", "3 jhn", "3jhn", "3 jn", "3jn", "3j", "3rd john", "third john"),
        arrayOf("jude", "jud", "jd"),
        arrayOf("revelation", "rev", "re", "the revelation"),
    )

    val allBookNames: Array<Array<String>>
        get() = bookNameNumberArray.map { it.copyOf() }.toTypedArray()
}

data class BookChapterFilter(
    val book: Int? = null,
    val startChapter: Int? = null,
    val endChapter: Int? = null,
    val term: String
)
