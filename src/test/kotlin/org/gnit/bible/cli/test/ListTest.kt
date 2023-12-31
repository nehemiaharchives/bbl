package org.gnit.bible.cli.test

import org.gnit.bible.cli.ListCli
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ListTest {
    private val outContent = ByteArrayOutputStream()
    private val originalOut = System.out

    @AfterEach
    fun putBackOutput() {
        System.setOut(originalOut)
    }

    @BeforeEach
    fun switchOutputToCapture(): Unit {
        System.setOut(PrintStream(outContent))
    }

    private val bibleList = """
        WEBUS | World English Bible                        | World English Bible              | English    | 2000 
        KJV   | King James Version                         | King James Version               | English    | 1611 
        RVR09 | Reina-Valera                               | Reina-Valera                     | Spanish    | 1909 
        TB    | Brazilian Translation                      | Tradução Brasileira              | Portuguese | 1917 
        DELUT | Luther Bible                               | Lutherbibel                      | German     | 1912 
        LSG   | Louis Segond                               | Bible Segond                     | French     | 1910 
        SINOD | Russian Synodal Bible                      | Синодальный перевод              | Russian    | 1876 
        SVRJ  | Statenvertaling Jongbloed edition          | Statenvertaling Jongbloed-editie | Dutch      | 1888 
        RDV24 | Revised Diodati Version                    | Versione Diodati Riveduta        | Italian    | 1924 
        UBG   | Updated Gdansk Bible                       | Uwspółcześniona Biblia gdańska   | Polish     | 2017 
        UKRK  | Ukrainian Bible, P. Kulisha and I. Pulyuya | Біблія в пер. П.Куліша та І.Пулюя| Ukrainian  | 1905 
        SVEN  | Svenska 1917                               | 1917 års kyrkobibel              | Swedish    | 1917 
        CUNP  | Chinese Union Version with New Punctuation | 新標點和合本                     | Chinese    | 1919 
        KRV   | Korean Revised Version                     | 개역한글                         | Korean     | 1961 
        JC    | Japanese Colloquial Bible                  | 口語訳                           | Japanese   | 1955 
        
    """.trimIndent()

    val listCli = ListCli()

    /**
     * EOL in this code is unix, but console output EOL can be windows CRLF.
     * So we use [assertLinesMatch] instead of [assertEquals] which fails
     */
    @Test
    fun listTranslationsDefaultTest() {
        listCli.parse(emptyArray())
        assertLinesMatch(bibleList.lines(), outContent.toString().lines())
    }

    @ParameterizedTest
    @ValueSource(strings = ["bible", "bibles", "translation", "translations", "version", "versions"])
    fun listTranslationTest(inquired: String) {
        listCli.parse(arrayOf(inquired))
        assertLinesMatch(bibleList.lines(), outContent.toString().lines())
    }

    @ParameterizedTest
    @ValueSource(strings = ["book", "books"])
    fun listBooksTest(inquired: String) {
        listCli.parse(arrayOf(inquired))
        val lines = outContent.toString().lines().filter { it.isNotBlank() }

        assertEquals(66, lines.size)
        assertEquals("genesis, gen, ge, gn", lines.first())
        assertEquals("revelation, rev, re, the revelation", lines.last())
    }
}