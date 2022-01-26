import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MissingArgument

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.int

fun parseBook(book: List<String>) = parseBook(book.map { it.lowercase() }.joinToString(separator = " "))

fun parseBook(book: String) = when (book) {
    "genesis", "gen", "ge", "gn" -> 1
    "exodus", "ex", "exod", "exo" -> 2
    "leviticus", "lev", "le", "lv" -> 3
    "numbers", "num", "nu", "nm", "nb" -> 4
    "deuteronomy", "deut", "de", "dt" -> 5
    "joshua", "josh", "jos", "jsh" -> 6
    "judges", "judg", "jdg", "jg", "jdgs" -> 7
    "ruth", "rth", "ru" -> 8
    "1st samuel", "1sam", "1sm", "1sa", "1s", "1 samuel", "1samuel", "1st sam", "first samuel", "first sam" -> 9
    "2nd samuel", "2sam", "2sm", "2sa", "2s", "2 samuel", "2ndsam", "2nd sam", "second samuel", "second sam" -> 10
    "1st kings", "1kings", "1 kings", "1kgs", "1ki", "1k", "1stkgs", "first kings", "first kgs" -> 11
    "2nd kings", "2kings", "2 kings", "2kgs", "2ki", "2k", "2ndkgs", "second kings", "second kgs" -> 12
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


    else -> throw Exception()
}

class Bbl : CliktCommand() {
    val book: List<String> by argument().multiple(required = true, default = listOf("gen"))
    val chapter: Int by argument().int().default(1)

    override fun run() {

        val bookNumber = try {
            parseBook(book)
        } catch (e: Exception) {
            throw MissingArgument(argument = argument("book"))
        }
        echo("book: $bookNumber, chapter: $chapter")
    }
}

fun main(args: Array<String>) = Bbl().main(args)