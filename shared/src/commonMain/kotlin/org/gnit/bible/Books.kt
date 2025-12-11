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

    enum class Category(val key: List<String>, range: IntRange){
        OLD_TESTAMENT(listOf("ot", "old testament"), 1..39),
        TORAH(listOf("t", "tor", "torah", "pe", "pent", "pentateuch"), 1..5),
        HISTORICAL_BOOKS(listOf("h"), 6..17),
        SAMUEL(listOf("sam", "samuel"), 9..10),
        KINGS(listOf("ki", "kings"), 11..12),
        CHRONICLES(listOf("chr", "chro", "chronicles"), 13..14),
        WISDOM_BOOKS(listOf("w", "wis", "wisdom"), 18..22),
        PROPHETS(listOf("p", "pro", "prophet", "prophets"), 23..39),
        MAJOR_PROPHETS(listOf("map", "major", "major prophet", "major prophets"), 23..27),
        MINOR_PROPHETS(listOf("mip", "minor", "minor prophet", "minor prophets"), 28..39),
        NEW_TESTAMENT(listOf("nt", "new testament"), 40..66),
        GOSPELS(listOf("g", "go", "gospel", "gospels"), 40..43),
        SYNOPTIC_GOSPELS(listOf("sg", "synoptic", "synoptic gospel", "synoptic gospels"), 40..42),
        PAULINE_EPISTLES(listOf("paul", "pauline", "pauline epi", "pauline epistle", "pauline episodes", "letter of paul", "letters of paul", "paul's letter", "paul's letters"), 45..57),
        CORINTHIANS(listOf("cor", "corinthians", "epistle to the corinthians", "letter to the corinthians"), 46..47),
        THESSALONIANS(listOf("thes", "thessalonians", "epistle to the thessalonians", "letter to the thessalonians"), 52..53),
        TIMOTHY(listOf("tim", "thimothy", "epistle to thimothy", "letter to thimothy"), 54..55),
        PETER(listOf("peter", "pet", "epistle of peter", "epistles of peter", "letter of peter", "letters of peter"), 60..61),
        JOHN(listOf("john", "jhn", "epistle of john", "epistles of john", "letter of john", "letters of john"), 62..64),
        ALL(listOf("all"), 1..66),
    }
}
