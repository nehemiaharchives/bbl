package org.gnit.bible.test

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import org.gnit.bible.getPlatform
import kotlin.test.assertEquals

interface SearchTestBase {
    var bible: Bible

    fun searchJesusChristCommonEmbedded(){
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
    }

    fun searchJesusChristCommonDownloaded(){
        // Indonesian (id)
        val actualAyt = bible.search(term = "Yesus Kristus", translation = Translation.ayt).first()
        assertEquals(VersePointer(Translation.ayt, 40, 1, 1), actualAyt)

        // Thai (th)
        val actualTh1971 = bible.search(term = "พระเยซูคริสต์", translation = Translation.th1971).first()
        assertEquals(VersePointer(Translation.th1971, 40, 1, 1), actualTh1971)

        // Hindi (hi)
        val actualIrvHin = bible.search(term = "यीशु मसीह", translation = Translation.irvhin).first()
        assertEquals(VersePointer(Translation.irvhin, 40, 1, 1), actualIrvHin)

        // Bengali (bn)
        val actualIrvBen = bible.search(term = "যীশু খ্রীষ্ট", translation = Translation.irvben).first()
        assertEquals(VersePointer(Translation.irvben, 40, 1, 1), actualIrvBen)

        // Telugu (te)
        val actualIrvTel = bible.search(term = "యేసు క్రీస్తు", translation = Translation.irvtel).first()
        assertEquals(VersePointer(Translation.irvtel, 40, 1, 1), actualIrvTel)

        // Tamil (ta)
        val actualIrvTam = bible.search(term = "இயேசுகிறிஸ்து", translation = Translation.irvtam).first()
        assertEquals(VersePointer(Translation.irvtam, 40, 1, 1), actualIrvTam)

        // Nepali (ne)
        val actualNpiUlb = bible.search(term = "येशू ख्रीष्‍ट", translation = Translation.npiulb).first()
        assertEquals(VersePointer(Translation.npiulb, 40, 1, 1), actualNpiUlb)

    }

    fun searchJesusChristMorfologik() {
        // Polish (pl)
        val actualUbg = bible.search(term = "Jezusa Chrystusa", translation = Translation.ubg).first()
        assertEquals(VersePointer(Translation.ubg, 40, 1, 1), actualUbg)

        // Ukrainian (uk)
        val actualUbio = bible.search(term = "Ісуса Христа", translation = Translation.ubio).first()
        assertEquals(VersePointer(Translation.ubio, 40, 1, 1), actualUbio)
    }

    fun searchJesusChristSmartcn() {
        // Chinese (zh)
        val actualCunp = bible.search(term = "耶稣基督", translation = Translation.cunp).first()
        assertEquals(VersePointer(Translation.cunp, 40, 1, 1), actualCunp)
    }

    fun searchJesusChristNori() {
        // Korean (ko)
        var actualKrv = bible.search(term = "예수그리스도", translation = Translation.krv).first()
        assertEquals(VersePointer(Translation.krv, 40, 1, 1), actualKrv)

        // with space between Jesus Christ
        actualKrv = bible.search(term = "예수 그리스도", translation = Translation.krv).first()
        assertEquals(VersePointer(Translation.krv, 40, 1, 1), actualKrv)
    }

    fun searchJesusChristKuromoji() {
        // Japanese (ja)
        val actualJc = bible.search(term = "イエス・キリスト", translation = Translation.jc).first()
        assertEquals(VersePointer(Translation.jc, 40, 1, 1), actualJc)
    }

    fun searchJesusChristExtra() {
        //TODO implement
    }
}

open class CliSearchTestBase(private val analyzerProvider: AnalyzerProvider) : SearchTestBase {

    override lateinit var bible: Bible

    open fun setup(){
        val platform = getPlatform()
        platform.overridePlatformPackDir = "../../../server/src/main/resources/files/bblpacks/"

        val am = AssetManagerImpl(platform = platform)

        bible = Bible(am, analyzerProvider)
    }
}
