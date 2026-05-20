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

    enum class Category(val key: List<String>, val filter: BibleFilter){
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
        PAULINE_EPISTLES(listOf("paul", "pauline", "pauline epi", "pauline epistle", "pauline episodes", "letter of paul", "letters of paul", "paul's letter", "paul's letters"), books(45..57)),
        CORINTHIANS(listOf("cor", "corinthians", "epistle to the corinthians", "letter to the corinthians"), books(46..47)),
        THESSALONIANS(listOf("thes", "thessalonians", "epistle to the thessalonians", "letter to the thessalonians"), books(52..53)),
        TIMOTHY(listOf("tim", "thimothy", "epistle to thimothy", "letter to thimothy"), books(54..55)),
        PETER(listOf("peter", "pet", "epistle of peter", "epistles of peter", "letter of peter", "letters of peter"), books(60, 61)),
        //JOHN_GOSPEL(listOf("john", "jhn", "johng", "johns gospel", "john gospel", "gospel john", "gospel of john"), books(43)),
        JOHN_LETTERS(listOf("johnl", "johne", "johns letter", "johns letter", "johns letters", "johnsletter", "john letter", "johnletter", "epistle of john", "epistles of john", "letter of john", "letters of john"), books(62, 63, 64)),
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
        }
    }
}

fun categoryFilterOrNull(key: String): BibleFilter? {
    return Books.Category.fromKey(key)?.filter
}
