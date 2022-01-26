import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MissingArgument

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.int

fun parseBook(book: String) = when (book) {
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
    "3 john", "3john", "3 jhn", "3jhn", "3 jn", "3jn", "3j", "3rd  john", "third john" -> 64
    "jude", "jud", "jd" -> 65
    "revelation", "rev", "re", "the revelation" -> 66
    else -> throw Exception()
}

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

class Bbl : CliktCommand() {

    val book: List<String> by argument().multiple(default = listOf("1", "sam"))
    val chapter: String by argument().default("3")

    override fun run() {

        var bookString = book.map { it.lowercase() }.joinToString(separator = " ")

        val bookNumber = try {
            parseBook(bookString)
        } catch (e: Exception) {
            throw MissingArgument(argument = argument("book '$bookString' not found in the list of book names"))
        }

        val chapterNumber = try {
            chapter.toInt()
        } catch (e: NumberFormatException) {
            throw MissingArgument(argument = argument("chapter"))
        }

        val maxChapter = maxChapter(bookNumber)
        if (chapterNumber > maxChapter){
            throw BadParameterValue("you requested chapter $chapterNumber but number of chapters of the book $bookString is only $maxChapter.")
        }

        echo("book: $bookNumber, chapter: $chapterNumber")
    }
}

fun main(args: Array<String>) = Bbl().main(args)