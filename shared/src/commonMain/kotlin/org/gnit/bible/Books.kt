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

    // TODO range/filter capability can be expanded as following
    // 1. book..book IntRange
    // 2. List<IntRange>
    // 3. BookChapterVerse..BookChapterVerse : BookChapterVerseRange
    // 4. List<BookChapterVerseRange>

    // TODO key can be expanded to have multiple per entry being List<String>

    enum class Category(val key: String, range: IntRange){
        OLD_TESTAMENT("ot", 1..39),
        TORAH("t", 1..5),
        HISTORICAL_BOOKS("h", 6..17),
        SAMUEL("samuel", 9..10),
        KINGS("kings", 11..12),
        CHRONICLES("chr", 13..14),
        WISDOM_BOOKS("w", 18..22),
        PROPHETS("p", 23..39),
        MAJOR_PROPHETS("map", 23..27),
        MINOR_PROPHETS("mip", 28..39),
        NEW_TESTAMENT("nt", 40..66),
        GOSPELS("g", 40..43),
        SYNOPTIC_GOSPELS("sg", 40..42),
        PAULINE_EPISTLES("paul", 45..57),
        CORINTHIANS("cor", 46..47),
        THESSALONIANS("thes", 52..53),
        TIMOTHY("tim", 54..55),
        PETER("peter", 60..61),
        JOHN("john", 62..64),
        ALL("all", 1..66)
    }
}
