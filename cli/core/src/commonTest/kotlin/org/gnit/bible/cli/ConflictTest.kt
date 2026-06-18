package org.gnit.bible.cli

import org.gnit.bible.Books
import org.gnit.bible.SupportedTranslation
import kotlin.test.Test
import kotlin.test.assertTrue

class ConflictTest {

    /**
     * bbl [book name] [chapter verse]
     * and
     * bbl [sub command]
     * should not conflict
    */
    @Test
    fun bblSubCommandDoesNotConflictWithBookNames(){
        val subCommands = listOf("search", "rand", "list", "install", "uninstall", "config", "history", "help")
        val subCommandAliases = Bbl().aliases()
        val bookNames = Books.allBookNames.flatMap { it.asIterable() }.toSet()
        val allSubCommandTerms = (subCommands + subCommandAliases.keys + subCommandAliases.values.flatten()).toSet()

        val conflict = bookNames.intersect(allSubCommandTerms)
        assertTrue(conflict.isEmpty(), "Book names conflict with subcommands/aliases: $conflict")
    }

    /**
     * bbl search Jesus in [book name]
     * and
     * bbl search Jesus in [category name]
     * and
     * bbl search Jesus in [translation code]
     * should not conflict
    */
    @Test
    fun bblBookCategoryAndTranslationCodeDoesNotConflictWithBookNames(){
        val categoryKeys = Books.Category.entries
            .filterNot { it == Books.Category.ALL }
            .flatMap { it.key }
            .toSet()
        val translationCodes = SupportedTranslation.entries.map { it.code }.toSet()
        val bookNames = Books.allBookNames.flatMap { it.asIterable() }.toSet()

        assertTrue(
            bookNames.intersect(categoryKeys).isEmpty(),
            "Book names intersect with category keys: ${bookNames.intersect(categoryKeys)}"
        )
        assertTrue(
            bookNames.intersect(translationCodes).isEmpty(),
            "Book names intersect with translation codes: ${bookNames.intersect(translationCodes)}"
        )
        assertTrue(
            categoryKeys.intersect(translationCodes).isEmpty(),
            "Category keys intersect with translation codes: ${categoryKeys.intersect(translationCodes)}"
        )
    }
}
