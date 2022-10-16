package org.gnit.bible.test

import org.gnit.bible.ListCli
import org.gnit.bible.Translation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TranslationTest {
    private val bibleList = """
        WEBUS, World English Bible, (World English Bible), English, 2000
        KJV, King James Version, (King James Version), English, 1611
        RVR09, Reina-Valera, (Reina-Valera), Spanish, 1909
        TB, Brazilian Translation, (Tradução Brasileira), Portuguese, 1917
        DELUT, Luther Bible, (Lutherbibel), German, 1912
        LSG, Louis Segond, (Bible Segond), French, 1910
        SINOD, Russian Synodal Bible, (Синодальный перевод), Russian, 1876
        SVRJ, Statenvertaling Jongbloed edition, (Statenvertaling Jongbloed-editie), Dutch, 1888
        RDV24, Revised Diodati Version, (Versione Diodati Riveduta), Italian, 1924
        UBG, Updated Gdansk Bible, (Uwspółcześniona Biblia gdańska), Polish, 2017
        UBIO, Ukrainian Bible, Ivan Ogienko, (Біблія в пер. Івана Огієнка), Ukrainian, 1962
        SVEN, Svenska 1917, (1917 års kyrkobibel), Swedish, 1917
        CUNP, Chinese Union Version with New Punctuation, (新標點和合本), Chinese, 1919
        KRV, Korean Revised Version, (개역한글), Korean, 1961
        JC, Japanese Colloquial Bible, (口語訳), Japanese, 1955
    """.trimIndent()

    val listCli = ListCli()

    @Test
    fun listTranslationsTest(){
        listCli.parse(emptyArray())
        assertEquals(bibleList.lines(), listCli.translationDescriptions)
    }

    @Test
    fun valueOfTest(){
        assertEquals(Translation.krv, Translation.valueOf("krv"))
    }
}