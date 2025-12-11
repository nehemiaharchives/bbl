package org.gnit.bible

/**
 * Position of a verse in the canonical 66-book order.
 * book: 1-based index (1 = Genesis, 66 = Revelation)
 */
data class BookChapterVerse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
) : Comparable<BookChapterVerse> {

    override fun compareTo(other: BookChapterVerse): Int =
        compareValuesBy(this, other, BookChapterVerse::book, BookChapterVerse::chapter, BookChapterVerse::verse)
}

/**
 * Allow idiomatic `start..end` ranges on BookChapterVerse.
 */
operator fun BookChapterVerse.rangeTo(other: BookChapterVerse): ClosedRange<BookChapterVerse> =
    object : ClosedRange<BookChapterVerse> {
        override val start: BookChapterVerse = this@rangeTo
        override val endInclusive: BookChapterVerse = other
    }

/**
 * Predicate over a verse reference.
 */
sealed interface BibleFilter {
    fun contains(ref: BookChapterVerse): Boolean

    data object All : BibleFilter {
        override fun contains(ref: BookChapterVerse) = true
    }

    data class BookRange(val range: IntRange) : BibleFilter {
        override fun contains(ref: BookChapterVerse): Boolean = ref.book in range
    }

    data class BookSet(val books: Set<Int>) : BibleFilter {
        override fun contains(ref: BookChapterVerse): Boolean = ref.book in books
    }

    data class Passage(val start: BookChapterVerse, val endInclusive: BookChapterVerse) : BibleFilter {
        init {
            require(start <= endInclusive) { "start must be <= endInclusive" }
        }

        override fun contains(ref: BookChapterVerse): Boolean =
            ref >= start && ref <= endInclusive
    }

    data class Union(val filters: List<BibleFilter>) : BibleFilter {
        override fun contains(ref: BookChapterVerse): Boolean = filters.any { it.contains(ref) }
    }
}

// Helper constructors for readability
fun books(range: IntRange): BibleFilter = BibleFilter.BookRange(range)
fun books(vararg ids: Int): BibleFilter = BibleFilter.BookSet(ids.toSet())
fun passage(start: BookChapterVerse, endInclusive: BookChapterVerse): BibleFilter = BibleFilter.Passage(start, endInclusive)
fun union(vararg filters: BibleFilter): BibleFilter = BibleFilter.Union(filters.toList())
