package org.gnit.bible.cli

import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.test.CliSearchTestBase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchCommonTest: CliSearchTestBase(CommonAnalyzerProvider()) {

    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun searchJesusChrist(){
        val englishTerm = "Jesus Christ"
        val actual = bible.search(term = englishTerm, translation = Translation.webus).first()
        assertEquals(VersePointer(Translation.webus, 40, 1,1), actual)

        val actualKjv = bible.search(term = englishTerm, translation = Translation.kjv).first()
        assertEquals(VersePointer(Translation.kjv, 40, 1,1), actualKjv)

        // Spanish (es)
        val actualRvr09 = bible.search(term = "Jesucristo", translation = Translation.rvr09).first()
        assertEquals(VersePointer(Translation.rvr09, 40, 1, 1), actualRvr09)

        // Portuguese (pt)
        val actualTb = bible.search(term = "Jesus Cristo", translation = Translation.tb).first()
        assertEquals(VersePointer(Translation.tb, 40, 1, 1), actualTb)

        // German (de)
        val actualDelut = bible.search(term = "Jesu Christi", translation = Translation.delut).first()
        assertEquals(VersePointer(Translation.delut, 40, 1, 1), actualDelut)

        // French (fr)
        val actualLsg = bible.search(term = "Jésus-Christ", translation = Translation.lsg).first()
        assertEquals(VersePointer(Translation.lsg, 40, 1, 1), actualLsg)

        // Russian (ru)
        val actualSinod = bible.search(term = "Иисуса Христа", translation = Translation.sinod).first()
        assertEquals(VersePointer(Translation.sinod, 40, 1, 1), actualSinod)

        // Dutch (nl)
        val actualSvrj = bible.search(term = "JEZUS CHRISTUS", translation = Translation.svrj).first()
        assertEquals(VersePointer(Translation.svrj, 40, 1, 1), actualSvrj)

        // Italian (it)
        val actualRdv24 = bible.search(term = "Gesù Cristo", translation = Translation.rdv24).first()
        assertEquals(VersePointer(Translation.rdv24, 40, 1, 1), actualRdv24)

        // Swedish (sv)
        val actualSven = bible.search(term = "Jesu Kristi", translation = Translation.sven).first()
        assertEquals(VersePointer(Translation.sven, 40, 1, 1), actualSven)

        // Thai (th)
        val actualTh1971 = bible.search(term = "พระเยซูคริสต์", translation = Translation.th1971).first()
        assertEquals(VersePointer(Translation.th1971, 40, 1, 1), actualTh1971)

        // Hindi (hi)
        val actualIrvHin = bible.search(term = "यीशु मसीह", translation = Translation.irvhin).first()
        assertEquals(VersePointer(Translation.irvhin, 40, 1, 1), actualIrvHin)

        // Bengali (bn)
        val actualIrvBen = bible.search(term = "যীশু খ্রীষ্ট", translation = Translation.irvben).first()
        assertEquals(VersePointer(Translation.irvben, 40, 1, 1), actualIrvBen)

        // Tamil (ta)
        val actualIrvTam = bible.search(term = "இயேசுகிறிஸ்து", translation = Translation.irvtam).first()
        assertEquals(VersePointer(Translation.irvtam, 40, 1, 1), actualIrvTam)

    }
}
