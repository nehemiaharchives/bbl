package org.gnit.bible.test

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Translation.Companion.delut
import org.gnit.bible.Translation.Companion.webus
import org.gnit.bible.Translation.Companion.kjv
import org.gnit.bible.Translation.Companion.lsg
import org.gnit.bible.Translation.Companion.rdv24
import org.gnit.bible.Translation.Companion.rvr09
import org.gnit.bible.Translation.Companion.sinod
import org.gnit.bible.Translation.Companion.sven
import org.gnit.bible.Translation.Companion.svrj
import org.gnit.bible.Translation.Companion.tb
import org.gnit.bible.Translation.Companion.ayt
import org.gnit.bible.Translation.Companion.th1971
import org.gnit.bible.Translation.Companion.irvhin
import org.gnit.bible.Translation.Companion.irvben
import org.gnit.bible.Translation.Companion.irvtel
import org.gnit.bible.Translation.Companion.irvtam
import org.gnit.bible.Translation.Companion.npiulb
import org.gnit.bible.Translation.Companion.ubg
import org.gnit.bible.Translation.Companion.ubio
import org.gnit.bible.Translation.Companion.cunp
import org.gnit.bible.Translation.Companion.krv
import org.gnit.bible.Translation.Companion.jc
import org.gnit.bible.Translation.Companion.abtag
import org.gnit.bible.Translation.Companion.kttv
import org.gnit.bible.Translation.Companion.irvguj
import org.gnit.bible.Translation.Companion.irvmar
import org.gnit.bible.Translation.Companion.irvurd
import org.gnit.bible.VersePointer
import org.gnit.bible.bookNumber
import org.gnit.bible.getPlatform
import kotlin.test.assertEquals

/**
 * When you add new downloadable bible for testing search, add bbl zip file to this dir: `composeApp/src/androidDeviceTest/assets/bblpacks`
 */
interface SearchTestBase {
    companion object {
        val romans: Int = bookNumber("romans")
    }

    var bible: Bible

    fun searchJesusChristCommonEmbedded(){

        // English (en, webus)
        val enTerm = "Jesus Christ"

        val actualWebus = bible.search(term = enTerm, translation = webus).first()
        assertEquals(VersePointer(webus, 40, 1,1), actualWebus)

        val actualWebusRomans = bible.search(term = enTerm, bookNumber = romans, translation = webus).first()
        assertEquals(VersePointer(webus, romans, 1, 1), actualWebusRomans)

        val actualWebusRomans2 = bible.search(term = enTerm, bookNumber = romans, startChapter = 2, translation = webus).first()
        assertEquals(VersePointer(webus, romans, 2, 16), actualWebusRomans2)
        /*
        @Test
        override fun searchJesusChristInWebusInRomans3To5() = super.searchJesusChristInWebusInRomans3To5()
        */

        // English (en, kjv)
        val actualKjv = bible.search(term = enTerm, translation = kjv).first()
        assertEquals(VersePointer(kjv, 40, 1,1), actualKjv)

        val actualKjvRomans = bible.search(term = enTerm, bookNumber = romans, translation = kjv).first()
        assertEquals(VersePointer(kjv, romans, 1, 1), actualKjvRomans)

        val actualKjvRomans2 = bible.search(term = enTerm, bookNumber = romans, startChapter = 2, translation = kjv).first()
        assertEquals(VersePointer(kjv, romans, 2, 16), actualKjvRomans2)

        // Spanish (es)
        val esTerm = "Jesucristo"

        val actualRvr09 = bible.search(term = esTerm, translation = rvr09).first()
        assertEquals(VersePointer(rvr09, 40, 1, 1), actualRvr09)

        val actualRvr09Romans = bible.search(term = esTerm, bookNumber = romans, translation = rvr09).first()
        assertEquals(VersePointer(rvr09, romans, 1, 1), actualRvr09Romans)

        val actualRvr09Romans2 = bible.search(term = esTerm, bookNumber = romans, startChapter = 2, translation = rvr09).first()
        assertEquals(VersePointer(rvr09, romans, 2, 16), actualRvr09Romans2)

        // Portuguese (pt)
        val ptTerm = "Jesus Cristo"

        val actualTb = bible.search(term = ptTerm, translation = tb).first()
        assertEquals(VersePointer(tb, 40, 1, 1), actualTb)

        val actualTbRomans = bible.search(term = ptTerm, bookNumber = romans, translation = tb).first()
        assertEquals(VersePointer(tb, romans, 1, 1), actualTbRomans)

        val actualTbRomans2 = bible.search(term = ptTerm, bookNumber = romans, startChapter = 2, translation = tb).first()
        assertEquals(VersePointer(tb, romans, 2, 16), actualTbRomans2)

        // German (de)
        val deTerm = "Jesu Christi"

        //bbl romans 1:1 in delut
        //1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes
        val actualDelut = bible.search(term = deTerm, translation = delut).first()
        assertEquals(VersePointer(delut, 40, 1, 1), actualDelut)

        val actualDelutRomans = bible.search(term = deTerm, bookNumber = romans, translation = delut).first()
        assertEquals(VersePointer(delut, romans, 1, 1), actualDelutRomans)

        //bbl romans 2:16 in delut
        //16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.
        val actualDelutRomans2 = bible.search(term = deTerm, bookNumber = romans, startChapter = 2, translation = delut).first()
        assertEquals(VersePointer(delut, romans, 2, 16), actualDelutRomans2)

        // French (fr)
        val frTerm = "Jésus-Christ"

        val actualLsg = bible.search(term = frTerm, translation = lsg).first()
        assertEquals(VersePointer(lsg, 40, 1, 1), actualLsg)

        val actualLsgRomans = bible.search(term = frTerm, bookNumber = romans, translation = lsg).first()
        assertEquals(VersePointer(lsg, romans, 1, 1), actualLsgRomans)

        val actualLsgRomans2 = bible.search(term = frTerm, bookNumber = romans, startChapter = 2, translation = lsg).first()
        assertEquals(VersePointer(lsg, romans, 2, 16), actualLsgRomans2)

        // Russian (ru)
        val ruTerm = "Иисуса Христа"

        val actualSinod = bible.search(term = ruTerm, translation = sinod).first()
        assertEquals(VersePointer(sinod, 40, 1, 1), actualSinod)

        val actualSinodRomans = bible.search(term = ruTerm, bookNumber = romans, translation = sinod).first()
        assertEquals(VersePointer(sinod, romans, 1, 1), actualSinodRomans)

        val actualSinodRomans2 = bible.search(term = ruTerm, bookNumber = romans, startChapter = 2, translation = sinod).first()
        assertEquals(VersePointer(sinod, romans, 2, 16), actualSinodRomans2)

        // Dutch (nl)
        val nlTerm = "JEZUS CHRISTUS"

        val actualSvrj = bible.search(term = nlTerm, translation = svrj).first()
        assertEquals(VersePointer(svrj, 40, 1, 1), actualSvrj)

        val actualSvrjRomans = bible.search(term = nlTerm, bookNumber = romans, translation = svrj).first()
        assertEquals(VersePointer(svrj, romans, 1, 1), actualSvrjRomans)

        val actualSvrjRomans2 = bible.search(term = nlTerm, bookNumber = romans, startChapter = 2, translation = svrj).first()
        assertEquals(VersePointer(svrj, romans, 2, 16), actualSvrjRomans2)

        // Italian (it)
        val itTerm = "Gesù Cristo"

        val actualRdv24 = bible.search(term = itTerm, translation = rdv24).first()
        assertEquals(VersePointer(rdv24, 40, 1, 1), actualRdv24)

        val actualRdv24Romans = bible.search(term = itTerm, bookNumber = romans, translation = rdv24).first()
        assertEquals(VersePointer(rdv24, romans, 1, 1), actualRdv24Romans)

        val actualRdv24Romans2 = bible.search(term = itTerm, bookNumber = romans, startChapter = 2, translation = rdv24).first()
        assertEquals(VersePointer(rdv24, romans, 2, 16), actualRdv24Romans2)

        // Swedish (sv)
        val svTerm = "Jesu Kristi"

        val actualSven = bible.search(term = svTerm, translation = sven).first()
        assertEquals(VersePointer(sven, 40, 1, 1), actualSven)

        val actualSvenRomans = bible.search(term = svTerm, bookNumber = romans, translation = sven).first()
        assertEquals(VersePointer(sven, romans, 1, 1), actualSvenRomans)

        val actualSvenRomans2 = bible.search(term = svTerm, bookNumber = romans, startChapter = 2, translation = sven).first()
        assertEquals(VersePointer(sven, romans, 2, 16), actualSvenRomans2)
    }

    fun searchJesusChristCommonDownloaded(){
        // Indonesian (id)
        val idTerm = "Yesus Kristus"
        val actualAyt = bible.search(term = idTerm, translation = ayt).first()
        assertEquals(VersePointer(ayt, 40, 1, 1), actualAyt)

        val actualAytRomans = bible.search(term = idTerm, bookNumber = romans, translation = ayt).first()
        assertEquals(VersePointer(ayt, romans, 1, 1), actualAytRomans)

        val actualAytRomans2 = bible.search(term = idTerm, bookNumber = romans, startChapter = 2, translation = ayt).first()
        assertEquals(VersePointer(ayt, romans, 2, 16), actualAytRomans2)

        // Thai (th)
        val thTerm = "พระเยซูคริสต์"
        val actualTh1971 = bible.search(term = thTerm, translation = th1971).first()
        assertEquals(VersePointer(th1971, 40, 1, 1), actualTh1971)

        val actualTh1971Romans = bible.search(term = thTerm, bookNumber = romans, translation = th1971).first()
        assertEquals(VersePointer(th1971, romans, 1, 1), actualTh1971Romans)

        val actualTh1971Romans2 = bible.search(term = thTerm, bookNumber = romans, startChapter = 2, translation = th1971).first()
        assertEquals(VersePointer(th1971, romans, 2, 16), actualTh1971Romans2)

        // Hindi (hi)
        val hiTerm = "यीशु मसीह"
        val actualIrvHin = bible.search(term = hiTerm, translation = irvhin).first()
        assertEquals(VersePointer(irvhin, 40, 1, 1), actualIrvHin)

        val actualIrvHinRomans = bible.search(term = hiTerm, bookNumber = romans, translation = irvhin).first()
        assertEquals(VersePointer(irvhin, romans, 1, 1), actualIrvHinRomans)

        val actualIrvHinRomans2 = bible.search(term = hiTerm, bookNumber = romans, startChapter = 2, translation = irvhin).first()
        assertEquals(VersePointer(irvhin, romans, 2, 16), actualIrvHinRomans2)

        // Bengali (bn)
        val bnTerm = "যীশু খ্রীষ্ট"
        val actualIrvBen = bible.search(term = bnTerm, translation = irvben).first()
        assertEquals(VersePointer(irvben, 40, 1, 1), actualIrvBen)

        val actualIrvBenRomans = bible.search(term = bnTerm, bookNumber = romans, translation = irvben).first()
        assertEquals(VersePointer(irvben, romans, 1, 1), actualIrvBenRomans)

        val actualIrvBenRomans2 = bible.search(term = bnTerm, bookNumber = romans, startChapter = 2, translation = irvben).first()
        assertEquals(VersePointer(irvben, romans, 2, 16), actualIrvBenRomans2)

        // Telugu (te)
        val teTerm = "యేసు క్రీస్తు"
        val actualIrvTel = bible.search(term = teTerm, translation = irvtel).first()
        assertEquals(VersePointer(irvtel, 40, 1, 1), actualIrvTel)

        val actualIrvTelRomans = bible.search(term = teTerm, bookNumber = romans, translation = irvtel).first()
        assertEquals(VersePointer(irvtel, romans, 1, 1), actualIrvTelRomans)

        val actualIrvTelRomans2 = bible.search(term = teTerm, bookNumber = romans, startChapter = 2, translation = irvtel).first()
        assertEquals(VersePointer(irvtel, romans, 2, 16), actualIrvTelRomans2)

        // Tamil (ta)
        val taTerm = "இயேசுகிறிஸ்து"
        val actualIrvTam = bible.search(term = taTerm, translation = irvtam).first()
        assertEquals(VersePointer(irvtam, 40, 1, 1), actualIrvTam)

        val actualIrvTamRomans = bible.search(term = taTerm, bookNumber = romans, translation = irvtam).first()
        assertEquals(VersePointer(irvtam, romans, 1, 1), actualIrvTamRomans)

        val actualIrvTamRomans2 = bible.search(term = taTerm, bookNumber = romans, startChapter = 2, translation = irvtam).first()
        assertEquals(VersePointer(irvtam, romans, 2, 16), actualIrvTamRomans2)

        // Nepali (ne)
        val neTerm = "येशू ख्रीष्‍ट"
        val actualNpiUlb = bible.search(term = neTerm, translation = npiulb).first()
        assertEquals(VersePointer(npiulb, 40, 1, 1), actualNpiUlb)

        val actualNpiUlbRomans = bible.search(term = neTerm, bookNumber = romans, translation = npiulb).first()
        assertEquals(VersePointer(npiulb, romans, 1, 1), actualNpiUlbRomans)

        val actualNpiUlbRomans2 = bible.search(term = neTerm, bookNumber = romans, startChapter = 2, translation = npiulb).first()
        assertEquals(VersePointer(npiulb, romans, 2, 16), actualNpiUlbRomans2)
    }

    fun searchJesusChristMorfologik() {
        // Polish (pl)
        val plTerm = "Jezusa Chrystusa"
        val actualUbg = bible.search(term = plTerm, translation = ubg).first()
        assertEquals(VersePointer(ubg, 40, 1, 1), actualUbg)

        val actualUbgRomans = bible.search(term = plTerm, bookNumber = romans, translation = ubg).first()
        assertEquals(VersePointer(ubg, romans, 1, 1), actualUbgRomans)

        val actualUbgRomans2 = bible.search(term = plTerm, bookNumber = romans, startChapter = 2, translation = ubg).first()
        assertEquals(VersePointer(ubg, romans, 2, 16), actualUbgRomans2)

        // Ukrainian (uk)
        val ukTerm = "Ісуса Христа"
        val actualUbio = bible.search(term = ukTerm, translation = ubio).first()
        assertEquals(VersePointer(ubio, 40, 1, 1), actualUbio)

        val actualUbioRomans = bible.search(term = ukTerm, bookNumber = romans, translation = ubio).first()
        assertEquals(VersePointer(ubio, romans, 1, 1), actualUbioRomans)

        val actualUbioRomans2 = bible.search(term = ukTerm, bookNumber = romans, startChapter = 2, translation = ubio).first()
        assertEquals(VersePointer(ubio, romans, 2, 16), actualUbioRomans2)
    }

    fun searchJesusChristSmartcn() {
        // Chinese (zh)
        val zhTerm = "耶稣基督"
        val actualCunp = bible.search(term = zhTerm, translation = cunp).first()
        assertEquals(VersePointer(cunp, 40, 1, 1), actualCunp)

        val actualCunpRomans = bible.search(term = zhTerm, bookNumber = romans, translation = cunp).first()
        assertEquals(VersePointer(cunp, romans, 1, 1), actualCunpRomans)

        val actualCunpRomans2 = bible.search(term = zhTerm, bookNumber = romans, startChapter = 2, translation = cunp).first()
        assertEquals(VersePointer(cunp, romans, 2, 16), actualCunpRomans2)
    }

    fun searchJesusChristNori() {
        // Korean (ko)
        val koTerm = "예수그리스도"
        var actualKrv = bible.search(term = koTerm, translation = krv).first()
        assertEquals(VersePointer(krv, 40, 1, 1), actualKrv)

        var actualKrvRomans = bible.search(term = koTerm, bookNumber = romans, translation = krv).first()
        assertEquals(VersePointer(krv, romans, 1, 1), actualKrvRomans)

        val actualKrvRomans2 = bible.search(term = koTerm, bookNumber = romans, startChapter = 2, translation = krv).first()
        assertEquals(VersePointer(krv, romans, 2, 16), actualKrvRomans2)

        // with space between Jesus Christ
        val koTermSpaced = "예수 그리스도"
        actualKrv = bible.search(term = koTermSpaced, translation = krv).first()
        assertEquals(VersePointer(krv, 40, 1, 1), actualKrv)

        actualKrvRomans = bible.search(term = koTermSpaced, bookNumber = romans, translation = krv).first()
        assertEquals(VersePointer(krv, romans, 1, 1), actualKrvRomans)

        val actualKrvRomans2Spaced = bible.search(term = koTermSpaced, bookNumber = romans, startChapter = 2, translation = krv).first()
        assertEquals(VersePointer(krv, romans, 2, 16), actualKrvRomans2Spaced)
    }

    fun searchJesusChristKuromoji() {
        // Japanese (ja)
        val jaTerm = "イエス・キリスト"
        val actualJc = bible.search(term = jaTerm, translation = jc).first()
        assertEquals(VersePointer(jc, 40, 1, 1), actualJc)

        val actualJcRomans = bible.search(term = jaTerm, bookNumber = romans, translation = jc).first()
        assertEquals(VersePointer(jc, romans, 1, 1), actualJcRomans)

        val actualJcRomans2 = bible.search(term = jaTerm, bookNumber = romans, startChapter = 2, translation = jc).first()
        assertEquals(VersePointer(jc, romans, 2, 16), actualJcRomans2)
    }

    fun searchJesusChristExtra() {
        // Tagalog (tl)
        val tlTerm = "Jesucristo"
        val actualAbtag = bible.search(term = tlTerm, translation = abtag).first()
        assertEquals(VersePointer(abtag, 40, 1, 1), actualAbtag)

        val actualAbtagRomans = bible.search(term = tlTerm, bookNumber = romans, translation = abtag).first()
        assertEquals(VersePointer(abtag, romans, 1, 1), actualAbtagRomans)

        val actualAbtagRomans2 = bible.search(term = tlTerm, bookNumber = romans, startChapter = 2, translation = abtag).first()
        assertEquals(VersePointer(abtag, romans, 2, 16), actualAbtagRomans2)


        // Vietnamese (vi)
        val viTerm = "Jêsus-Christ"
        val actualKttv = bible.search(term = viTerm, translation = kttv).first()
        assertEquals(VersePointer(kttv, 40, 1, 1), actualKttv)

        val actualKttvRomans = bible.search(term = viTerm, bookNumber = romans, translation = kttv).first()
        assertEquals(VersePointer(kttv, romans, 1, 1), actualKttvRomans)

        val actualKttvRomans2 = bible.search(term = viTerm, bookNumber = romans, startChapter = 2, translation = kttv).first()
        assertEquals(VersePointer(kttv, romans, 2, 16), actualKttvRomans2)

        // Gujarati (gu)
        val guTerm = "ઈસુ ખ્રિસ્ત"
        val actualIrvGuj = bible.search(term = guTerm, translation = irvguj).first()
        assertEquals(VersePointer(irvguj, 40, 1, 1), actualIrvGuj)

        val actualIrvGujRomans = bible.search(term = guTerm, bookNumber = romans, translation = irvguj).first()
        assertEquals(VersePointer(irvguj, romans, 1, 1), actualIrvGujRomans)

        val actualIrvGujRomans2 = bible.search(term = guTerm, bookNumber = romans, startChapter = 2, translation = irvguj).first()
        assertEquals(VersePointer(irvguj, romans, 2, 16), actualIrvGujRomans2)


        // Marathi (mr)
        val mrTerm = "येशू ख्रिस्त"
        val actualIrvMar = bible.search(term = mrTerm, translation = irvmar).first()
        assertEquals(VersePointer(irvmar, 40, 1, 1), actualIrvMar)

        val actualIrvMarRomans = bible.search(term = mrTerm, bookNumber = romans, translation = irvmar).first()
        assertEquals(VersePointer(irvmar, romans, 1, 1), actualIrvMarRomans)

        val actualIrvMarRomans2 = bible.search(term = mrTerm, bookNumber = romans, startChapter = 2, translation = irvmar).first()
        assertEquals(VersePointer(irvmar, romans, 2, 16), actualIrvMarRomans2)


        // Urdu (ur)
        val urTerm = "ईसा मसीह"
        val actualIrvUrd = bible.search(term = urTerm, translation = irvurd).first()
        assertEquals(VersePointer(irvurd, 40, 1, 1), actualIrvUrd)

        val actualIrvUrdRomans = bible.search(term = urTerm, bookNumber = romans, translation = irvurd).first()
        assertEquals(VersePointer(irvurd, romans, 1, 1), actualIrvUrdRomans)

        val actualIrvUrdRomans2 = bible.search(term = urTerm, bookNumber = romans, startChapter = 2, translation = irvurd).first()
        assertEquals(VersePointer(irvurd, romans, 2, 16), actualIrvUrdRomans2)
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
