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
        listOf("Jesus Christ", "Jesus", "Christ").forEach { enTerm ->
            // English (en, webus)

            val actualWebus = bible.search(term = enTerm, translation = webus).first()
            assertEquals(VersePointer(webus, 40, 1, 1), actualWebus, "Failed on searching: $enTerm")

            val actualWebusRomans = bible.search(term = enTerm, bookNumber = romans, translation = webus).first()
            assertEquals(VersePointer(webus, romans, 1, 1), actualWebusRomans, "Failed on searching: $enTerm")

            val actualWebusRomans2 = bible.search(term = enTerm, bookNumber = romans, startChapter = 2, translation = webus).first()
            assertEquals(VersePointer(webus, romans, 2, 16), actualWebusRomans2, "Failed on searching: $enTerm")

            val actualWebusRomans3To5 = bible.search(term = enTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = webus).first()
            assertEquals(VersePointer(webus, romans, 3, 22), actualWebusRomans3To5, "Failed on searching: $enTerm")

            // English (en, kjv)
            val actualKjv = bible.search(term = enTerm, translation = kjv).first()
            assertEquals(VersePointer(kjv, 40, 1, 1), actualKjv, "Failed on searching: $enTerm")

            val actualKjvRomans = bible.search(term = enTerm, bookNumber = romans, translation = kjv).first()
            assertEquals(VersePointer(kjv, romans, 1, 1), actualKjvRomans, "Failed on searching: $enTerm")

            val actualKjvRomans2 = bible.search(term = enTerm, bookNumber = romans, startChapter = 2, translation = kjv).first()
            assertEquals(VersePointer(kjv, romans, 2, 16), actualKjvRomans2, "Failed on searching: $enTerm")

            val actualKjvRomans3To5 = bible.search(term = enTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = kjv).first()
            assertEquals(VersePointer(kjv, romans, 3, 22), actualKjvRomans3To5, "Failed on searching: $enTerm")
        }

        // Spanish (es)
        listOf("Jesucristo", "Jesús", "Cristo").forEach { esTerm ->
            val actualRvr09 = bible.search(term = esTerm, translation = rvr09).first()
            assertEquals(VersePointer(rvr09, 40, 1, 1), actualRvr09, "Failed on searching: $esTerm")

            val actualRvr09Romans = bible.search(term = esTerm, bookNumber = romans, translation = rvr09).first()
            assertEquals(VersePointer(rvr09, romans, 1, 1), actualRvr09Romans, "Failed on searching: $esTerm")

            val actualRvr09Romans2 = bible.search(term = esTerm, bookNumber = romans, startChapter = 2, translation = rvr09).first()
            assertEquals(VersePointer(rvr09, romans, 2, 16), actualRvr09Romans2, "Failed on searching: $esTerm")

            val actualRvr09Romans3To5 = bible.search(term = esTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = rvr09).first()
            assertEquals(VersePointer(rvr09, romans, 3, 22), actualRvr09Romans3To5, "Failed on searching: $esTerm")
        }

        // Portuguese (pt)
        listOf("Jesus Cristo", "Jesus", "Cristo").forEach { ptTerm ->
            val actualTb = bible.search(term = ptTerm, translation = tb).first()
            assertEquals(VersePointer(tb, 40, 1, 1), actualTb, "Failed on searching: $ptTerm")

            val actualTbRomans = bible.search(term = ptTerm, bookNumber = romans, translation = tb).first()
            assertEquals(VersePointer(tb, romans, 1, 1), actualTbRomans, "Failed on searching: $ptTerm")

            val actualTbRomans2 = bible.search(term = ptTerm, bookNumber = romans, startChapter = 2, translation = tb).first()
            assertEquals(VersePointer(tb, romans, 2, 16), actualTbRomans2, "Failed on searching: $ptTerm")

            val actualTbRomans3To5 = bible.search(term = ptTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = tb).first()
            assertEquals(VersePointer(tb, romans, 3, 22), actualTbRomans3To5, "Failed on searching: $ptTerm")
        }

        // German (de)
        listOf("Jesu Christi", "Jesu", "Christi").forEach { deTerm ->
            //bbl romans 1:1 in delut
            //1 Paulus, ein Knecht Jesu Christi, berufen zum Apostel, ausgesondert, zu predigen das Evangelium Gottes
            val actualDelut = bible.search(term = deTerm, translation = delut).first()
            assertEquals(VersePointer(delut, 40, 1, 1), actualDelut, "Failed on searching: $deTerm")

            val actualDelutRomans = bible.search(term = deTerm, bookNumber = romans, translation = delut).first()
            assertEquals(VersePointer(delut, romans, 1, 1), actualDelutRomans, "Failed on searching: $deTerm")

            //bbl romans 2:16 in delut
            //16 auf den Tag, da Gott das Verborgene der Menschen durch Jesus Christus richten wird laut meines Evangeliums.
            val actualDelutRomans2 = bible.search(term = deTerm, bookNumber = romans, startChapter = 2, translation = delut).first()
            assertEquals(VersePointer(delut, romans, 2, 16), actualDelutRomans2, "Failed on searching: $deTerm")

            val actualDelutRomans3To5 = bible.search(term = deTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = delut).first()
            assertEquals(VersePointer(delut, romans, 3, 22), actualDelutRomans3To5, "Failed on searching: $deTerm")
        }

        // French (fr)
        listOf("Jésus-Christ", "Jésus", "Christ").forEach { frTerm ->
            val actualLsg = bible.search(term = frTerm, translation = lsg).first()
            assertEquals(VersePointer(lsg, 40, 1, 1), actualLsg, "Failed on searching: $frTerm")

            val actualLsgRomans = bible.search(term = frTerm, bookNumber = romans, translation = lsg).first()
            assertEquals(VersePointer(lsg, romans, 1, 1), actualLsgRomans, "Failed on searching: $frTerm")

            val actualLsgRomans2 = bible.search(term = frTerm, bookNumber = romans, startChapter = 2, translation = lsg).first()
            assertEquals(VersePointer(lsg, romans, 2, 16), actualLsgRomans2, "Failed on searching: $frTerm")

            val actualLsgRomans3To5 = bible.search(term = frTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = lsg).first()
            assertEquals(VersePointer(lsg, romans, 3, 22), actualLsgRomans3To5, "Failed on searching: $frTerm")
        }

        // Russian (ru)
        listOf("Иисуса Христа", "Иисуса", "Христа").forEach { ruTerm ->
            val actualSinod = bible.search(term = ruTerm, translation = sinod).first()
            assertEquals(VersePointer(sinod, 40, 1, 1), actualSinod, "Failed on searching: $ruTerm")

            val actualSinodRomans = bible.search(term = ruTerm, bookNumber = romans, translation = sinod).first()
            assertEquals(VersePointer(sinod, romans, 1, 1), actualSinodRomans, "Failed on searching: $ruTerm")

            val actualSinodRomans2 = bible.search(term = ruTerm, bookNumber = romans, startChapter = 2, translation = sinod).first()
            assertEquals(VersePointer(sinod, romans, 2, 16), actualSinodRomans2, "Failed on searching: $ruTerm")

            val actualSinodRomans3To5 = bible.search(term = ruTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = sinod).first()
            assertEquals(VersePointer(sinod, romans, 3, 22), actualSinodRomans3To5, "Failed on searching: $ruTerm")
        }

        // Dutch (nl)
        listOf("JEZUS CHRISTUS", "JEZUS", "CHRISTUS").forEach { nlTerm ->
            val actualSvrj = bible.search(term = nlTerm, translation = svrj).first()
            assertEquals(VersePointer(svrj, 40, 1, 1), actualSvrj, "Failed on searching: $nlTerm")

            val actualSvrjRomans = bible.search(term = nlTerm, bookNumber = romans, translation = svrj).first()
            assertEquals(VersePointer(svrj, romans, 1, 1), actualSvrjRomans, "Failed on searching: $nlTerm")

            val actualSvrjRomans2 = bible.search(term = nlTerm, bookNumber = romans, startChapter = 2, translation = svrj).first()
            assertEquals(VersePointer(svrj, romans, 2, 16), actualSvrjRomans2, "Failed on searching: $nlTerm")

            val actualSvrjRomans3To5 = bible.search(term = nlTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = svrj).first()
            assertEquals(VersePointer(svrj, romans, 3, 22), actualSvrjRomans3To5, "Failed on searching: $nlTerm")
        }

        // Italian (it)
        listOf("Gesù Cristo", "Gesù", "Cristo").forEach { itTerm ->
            val actualRdv24 = bible.search(term = itTerm, translation = rdv24).first()
            assertEquals(VersePointer(rdv24, 40, 1, 1), actualRdv24, "Failed on searching: $itTerm")

            val actualRdv24Romans = bible.search(term = itTerm, bookNumber = romans, translation = rdv24).first()
            assertEquals(VersePointer(rdv24, romans, 1, 1), actualRdv24Romans, "Failed on searching: $itTerm")

            val actualRdv24Romans2 = bible.search(term = itTerm, bookNumber = romans, startChapter = 2, translation = rdv24).first()
            assertEquals(VersePointer(rdv24, romans, 2, 16), actualRdv24Romans2, "Failed on searching: $itTerm")

            val actualRdv24Romans3To5 = bible.search(term = itTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = rdv24).first()
            assertEquals(VersePointer(rdv24, romans, 3, 22), actualRdv24Romans3To5, "Failed on searching: $itTerm")
        }

        // Swedish (sv)
        listOf("Jesu Kristi", "Jesu", "Kristi").forEach { svTerm ->
            val actualSven = bible.search(term = svTerm, translation = sven).first()
            assertEquals(VersePointer(sven, 40, 1, 1), actualSven, "Failed on searching: $svTerm")

            val actualSvenRomans = bible.search(term = svTerm, bookNumber = romans, translation = sven).first()
            assertEquals(VersePointer(sven, romans, 1, 1), actualSvenRomans, "Failed on searching: $svTerm")

            val actualSvenRomans2 = bible.search(term = svTerm, bookNumber = romans, startChapter = 2, translation = sven).first()
            assertEquals(VersePointer(sven, romans, 2, 16), actualSvenRomans2, "Failed on searching: $svTerm")

            val actualSvenRomans3To5 = bible.search(term = svTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = sven).first()
            assertEquals(VersePointer(sven, romans, 3, 22), actualSvenRomans3To5, "Failed on searching: $svTerm")
        }
    }

    fun searchJesusChristCommonDownloaded(){
        // Indonesian (id)
        listOf("Yesus Kristus", "Yesus", "Kristus").forEach { idTerm ->
            val actualAyt = bible.search(term = idTerm, translation = ayt).first()
            assertEquals(VersePointer(ayt, 40, 1, 1), actualAyt, "Failed on searching: $idTerm")

            val actualAytRomans = bible.search(term = idTerm, bookNumber = romans, translation = ayt).first()
            assertEquals(VersePointer(ayt, romans, 1, 1), actualAytRomans, "Failed on searching: $idTerm")

            val actualAytRomans2 = bible.search(term = idTerm, bookNumber = romans, startChapter = 2, translation = ayt).first()
            assertEquals(VersePointer(ayt, romans, 2, 16), actualAytRomans2, "Failed on searching: $idTerm")

            val actualAytRomans3To5 = bible.search(term = idTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ayt).first()
            assertEquals(VersePointer(ayt, romans, 3, 22), actualAytRomans3To5, "Failed on searching: $idTerm")
        }

        // Thai (th)
        listOf("พระเยซูคริสต์", "พระเยซู", "คริสต์").forEach { thTerm ->
            val actualTh1971 = bible.search(term = thTerm, translation = th1971).first()
            assertEquals(VersePointer(th1971, 40, 1, 1), actualTh1971, "Failed on searching: $thTerm")

            val actualTh1971Romans = bible.search(term = thTerm, bookNumber = romans, translation = th1971).first()
            assertEquals(VersePointer(th1971, romans, 1, 1), actualTh1971Romans, "Failed on searching: $thTerm")

            val actualTh1971Romans2 = bible.search(term = thTerm, bookNumber = romans, startChapter = 2, translation = th1971).first()
            assertEquals(VersePointer(th1971, romans, 2, 16), actualTh1971Romans2, "Failed on searching: $thTerm")

            val actualTh1971Romans3To5 = bible.search(term = thTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = th1971).first()
            assertEquals(VersePointer(th1971, romans, 3, 22), actualTh1971Romans3To5, "Failed on searching: $thTerm")
        }

        // Hindi (hi)
        listOf("यीशु मसीह", "यीशु", "मसीह").forEach { hiTerm ->
            val actualIrvHin = bible.search(term = hiTerm, translation = irvhin).first()
            assertEquals(VersePointer(irvhin, 40, 1, 1), actualIrvHin, "Failed on searching: $hiTerm")

            val actualIrvHinRomans = bible.search(term = hiTerm, bookNumber = romans, translation = irvhin).first()
            assertEquals(VersePointer(irvhin, romans, 1, 1), actualIrvHinRomans, "Failed on searching: $hiTerm")

            val actualIrvHinRomans2 = bible.search(term = hiTerm, bookNumber = romans, startChapter = 2, translation = irvhin).first()
            assertEquals(VersePointer(irvhin, romans, 2, 16), actualIrvHinRomans2, "Failed on searching: $hiTerm")

            val actualIrvHinRomans3To5 = bible.search(term = hiTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvhin).first()
            assertEquals(VersePointer(irvhin, romans, 3, 22), actualIrvHinRomans3To5, "Failed on searching: $hiTerm")
        }

        // Bengali (bn)
        listOf("যীশু খ্রীষ্ট", "যীশু", "খ্রীষ্ট").forEach { bnTerm ->
            val actualIrvBen = bible.search(term = bnTerm, translation = irvben).first()
            assertEquals(VersePointer(irvben, 40, 1, 1), actualIrvBen, "Failed on searching: $bnTerm")

            val actualIrvBenRomans = bible.search(term = bnTerm, bookNumber = romans, translation = irvben).first()
            assertEquals(VersePointer(irvben, romans, 1, 1), actualIrvBenRomans, "Failed on searching: $bnTerm")

            val actualIrvBenRomans2 = bible.search(term = bnTerm, bookNumber = romans, startChapter = 2, translation = irvben).first()
            assertEquals(VersePointer(irvben, romans, 2, 16), actualIrvBenRomans2, "Failed on searching: $bnTerm")

            val actualIrvBenRomans3To5 = bible.search(term = bnTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvben).first()
            assertEquals(VersePointer(irvben, romans, 3, 22), actualIrvBenRomans3To5, "Failed on searching: $bnTerm")
        }

        // Telugu (te)
        listOf("యేసు క్రీస్తు", "యేసు", "క్రీస్తు").forEach { teTerm ->
            val actualIrvTel = bible.search(term = teTerm, translation = irvtel).first()
            assertEquals(VersePointer(irvtel, 40, 1, 1), actualIrvTel, "Failed on searching: $teTerm")

            val actualIrvTelRomans = bible.search(term = teTerm, bookNumber = romans, translation = irvtel).first()
            assertEquals(VersePointer(irvtel, romans, 1, 1), actualIrvTelRomans, "Failed on searching: $teTerm")

            val actualIrvTelRomans2 = bible.search(term = teTerm, bookNumber = romans, startChapter = 2, translation = irvtel).first()
            assertEquals(VersePointer(irvtel, romans, 2, 16), actualIrvTelRomans2, "Failed on searching: $teTerm")

            val actualIrvTelRomans3To5 = bible.search(term = teTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvtel).first()
            assertEquals(VersePointer(irvtel, romans, 3, 22), actualIrvTelRomans3To5, "Failed on searching: $teTerm")
        }

        // Tamil (ta)
        listOf("இயேசுகிறிஸ்து", "இயேசு", "கிறிஸ்து").forEach { taTerm ->
            val actualIrvTam = bible.search(term = taTerm, translation = irvtam).first()
            assertEquals(VersePointer(irvtam, 40, 1, 1), actualIrvTam, "Failed on searching: $taTerm")

            val actualIrvTamRomans = bible.search(term = taTerm, bookNumber = romans, translation = irvtam).first()
            assertEquals(VersePointer(irvtam, romans, 1, 1), actualIrvTamRomans, "Failed on searching: $taTerm")

            val actualIrvTamRomans2 = bible.search(term = taTerm, bookNumber = romans, startChapter = 2, translation = irvtam).first()
            assertEquals(VersePointer(irvtam, romans, 2, 16), actualIrvTamRomans2, "Failed on searching: $taTerm")

            val actualIrvTamRomans3To5 = bible.search(term = taTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvtam).first()
            assertEquals(VersePointer(irvtam, romans, 3, 22), actualIrvTamRomans3To5, "Failed on searching: $taTerm")
        }

        // Nepali (ne)
        listOf("येशू ख्रीष्‍ट", "येशू", "ख्रीष्‍ट").forEach { neTerm ->
            val actualNpiUlb = bible.search(term = neTerm, translation = npiulb).first()
            assertEquals(VersePointer(npiulb, 40, 1, 1), actualNpiUlb, "Failed on searching: $neTerm")

            val actualNpiUlbRomans = bible.search(term = neTerm, bookNumber = romans, translation = npiulb).first()
            assertEquals(VersePointer(npiulb, romans, 1, 1), actualNpiUlbRomans, "Failed on searching: $neTerm")

            val actualNpiUlbRomans2 = bible.search(term = neTerm, bookNumber = romans, startChapter = 2, translation = npiulb).first()
            assertEquals(VersePointer(npiulb, romans, 2, 16), actualNpiUlbRomans2, "Failed on searching: $neTerm")

            val actualNpiUlbRomans3To5 = bible.search(term = neTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = npiulb).first()
            assertEquals(VersePointer(npiulb, romans, 3, 22), actualNpiUlbRomans3To5, "Failed on searching: $neTerm")
        }
    }

    fun searchJesusChristMorfologik() {
        // Polish (pl)
        listOf("Jezusa Chrystusa", "Jezusa", "Chrystusa").forEach { plTerm ->
            val actualUbg = bible.search(term = plTerm, translation = ubg).first()
            assertEquals(VersePointer(ubg, 40, 1, 1), actualUbg, "Failed on searching: $plTerm")

            val actualUbgRomans = bible.search(term = plTerm, bookNumber = romans, translation = ubg).first()
            assertEquals(VersePointer(ubg, romans, 1, 1), actualUbgRomans, "Failed on searching: $plTerm")

            val actualUbgRomans2 = bible.search(term = plTerm, bookNumber = romans, startChapter = 2, translation = ubg).first()
            assertEquals(VersePointer(ubg, romans, 2, 16), actualUbgRomans2, "Failed on searching: $plTerm")

            val actualUbgRomans3To5 = bible.search(term = plTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ubg).first()
            assertEquals(VersePointer(ubg, romans, 3, 22), actualUbgRomans3To5, "Failed on searching: $plTerm")
        }

        // Ukrainian (uk)
        listOf("Ісуса Христа", "Ісуса", "Христа").forEach { ukTerm ->
            val actualUbio = bible.search(term = ukTerm, translation = ubio).first()
            assertEquals(VersePointer(ubio, 40, 1, 1), actualUbio, "Failed on searching: $ukTerm")

            val actualUbioRomans = bible.search(term = ukTerm, bookNumber = romans, translation = ubio).first()
            assertEquals(VersePointer(ubio, romans, 1, 1), actualUbioRomans, "Failed on searching: $ukTerm")

            val actualUbioRomans2 = bible.search(term = ukTerm, bookNumber = romans, startChapter = 2, translation = ubio).first()
            assertEquals(VersePointer(ubio, romans, 2, 16), actualUbioRomans2, "Failed on searching: $ukTerm")

            val actualUbioRomans3To5 = bible.search(term = ukTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ubio).first()
            assertEquals(VersePointer(ubio, romans, 3, 22), actualUbioRomans3To5, "Failed on searching: $ukTerm")
        }
    }

    fun searchJesusChristSmartcn() {
        // Chinese (zh)
        listOf("耶稣基督", "耶稣", "基督").forEach { zhTerm ->
            val actualCunp = bible.search(term = zhTerm, translation = cunp).first()
            assertEquals(VersePointer(cunp, 40, 1, 1), actualCunp, "Failed on searching: $zhTerm")

            val actualCunpRomans = bible.search(term = zhTerm, bookNumber = romans, translation = cunp).first()
            assertEquals(VersePointer(cunp, romans, 1, 1), actualCunpRomans, "Failed on searching: $zhTerm")

            val actualCunpRomans2 = bible.search(term = zhTerm, bookNumber = romans, startChapter = 2, translation = cunp).first()
            assertEquals(VersePointer(cunp, romans, 2, 16), actualCunpRomans2, "Failed on searching: $zhTerm")

            val actualCunpRomans3To5 = bible.search(term = zhTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = cunp).first()
            assertEquals(VersePointer(cunp, romans, 3, 22), actualCunpRomans3To5, "Failed on searching: $zhTerm")
        }
    }

    fun searchJesusChristNori() {
        // Korean (ko)
        listOf("예수그리스도", "예수 그리스도", "예수", "그리스도").forEach { koTerm ->
            val actualKrv = bible.search(term = koTerm, translation = krv).first()
            assertEquals(VersePointer(krv, 40, 1, 1), actualKrv, "Failed on searching: $koTerm")

            val actualKrvRomans = bible.search(term = koTerm, bookNumber = romans, translation = krv).first()
            assertEquals(VersePointer(krv, romans, 1, 1), actualKrvRomans, "Failed on searching: $koTerm")

            val actualKrvRomans2 = bible.search(term = koTerm, bookNumber = romans, startChapter = 2, translation = krv).first()
            assertEquals(VersePointer(krv, romans, 2, 16), actualKrvRomans2, "Failed on searching: $koTerm")

            val actualKrvRomans3To5 = bible.search(term = koTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = krv).first()
            assertEquals(VersePointer(krv, romans, 3, 22), actualKrvRomans3To5, "Failed on searching: $koTerm")
        }
    }

    fun searchJesusChristKuromoji() {
        // Japanese (ja)
        listOf("イエス・キリスト", "イエス", "キリスト").forEach { jaTerm ->
            val actualJc = bible.search(term = jaTerm, translation = jc).first()
            assertEquals(VersePointer(jc, 40, 1, 1), actualJc, "Failed on searching: $jaTerm")

            val actualJcRomans = bible.search(term = jaTerm, bookNumber = romans, translation = jc).first()
            assertEquals(VersePointer(jc, romans, 1, 1), actualJcRomans, "Failed on searching: $jaTerm")

            val actualJcRomans2 = bible.search(term = jaTerm, bookNumber = romans, startChapter = 2, translation = jc).first()
            assertEquals(VersePointer(jc, romans, 2, 16), actualJcRomans2, "Failed on searching: $jaTerm")

            val actualJcRomans3To5 = bible.search(term = jaTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = jc).first()
            assertEquals(VersePointer(jc, romans, 3, 22), actualJcRomans3To5, "Failed on searching: $jaTerm")
        }
    }

    fun searchJesusChristExtra() {
        // Tagalog (tl)
        listOf("Jesucristo", "Jesus", "Cristo").forEach { tlTerm ->
            val actualAbtag = bible.search(term = tlTerm, translation = abtag).first()
            assertEquals(VersePointer(abtag, 40, 1, 1), actualAbtag, "Failed on searching: $tlTerm")

            val actualAbtagRomans = bible.search(term = tlTerm, bookNumber = romans, translation = abtag).first()
            assertEquals(VersePointer(abtag, romans, 1, 1), actualAbtagRomans, "Failed on searching: $tlTerm")

            val actualAbtagRomans2 = bible.search(term = tlTerm, bookNumber = romans, startChapter = 2, translation = abtag).first()
            assertEquals(VersePointer(abtag, romans, 2, 16), actualAbtagRomans2, "Failed on searching: $tlTerm")

            val actualAbtagRomans3To5 = bible.search(term = tlTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = abtag).first()
            assertEquals(VersePointer(abtag, romans, 3, 22), actualAbtagRomans3To5, "Failed on searching: $tlTerm")
        }


        // Vietnamese (vi)
        listOf("Jêsus-Christ", "Jêsus", "Christ").forEach { viTerm ->
            val actualKttv = bible.search(term = viTerm, translation = kttv).first()
            assertEquals(VersePointer(kttv, 40, 1, 1), actualKttv, "Failed on searching: $viTerm")

            val actualKttvRomans = bible.search(term = viTerm, bookNumber = romans, translation = kttv).first()
            assertEquals(VersePointer(kttv, romans, 1, 1), actualKttvRomans, "Failed on searching: $viTerm")

            val actualKttvRomans2 = bible.search(term = viTerm, bookNumber = romans, startChapter = 2, translation = kttv).first()
            assertEquals(VersePointer(kttv, romans, 2, 16), actualKttvRomans2, "Failed on searching: $viTerm")

            val actualKttvRomans3To5 = bible.search(term = viTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = kttv).first()
            assertEquals(VersePointer(kttv, romans, 3, 22), actualKttvRomans3To5, "Failed on searching: $viTerm")
        }

        // Gujarati (gu)
        listOf("ઈસુ ખ્રિસ્ત", "ઈસુ", "ખ્રિસ્ત").forEach { guTerm ->
            val actualIrvGuj = bible.search(term = guTerm, translation = irvguj).first()
            assertEquals(VersePointer(irvguj, 40, 1, 1), actualIrvGuj, "Failed on searching: $guTerm")

            val actualIrvGujRomans = bible.search(term = guTerm, bookNumber = romans, translation = irvguj).first()
            assertEquals(VersePointer(irvguj, romans, 1, 1), actualIrvGujRomans, "Failed on searching: $guTerm")

            val actualIrvGujRomans2 = bible.search(term = guTerm, bookNumber = romans, startChapter = 2, translation = irvguj).first()
            assertEquals(VersePointer(irvguj, romans, 2, 16), actualIrvGujRomans2, "Failed on searching: $guTerm")

            val actualIrvGujRomans3To5 = bible.search(term = guTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvguj).first()
            assertEquals(VersePointer(irvguj, romans, 3, 22), actualIrvGujRomans3To5, "Failed on searching: $guTerm")
        }


        // Marathi (mr)
        listOf("येशू ख्रिस्त", "येशू", "ख्रिस्त").forEach { mrTerm ->
            val actualIrvMar = bible.search(term = mrTerm, translation = irvmar).first()
            assertEquals(VersePointer(irvmar, 40, 1, 1), actualIrvMar, "Failed on searching: $mrTerm")

            val actualIrvMarRomans = bible.search(term = mrTerm, bookNumber = romans, translation = irvmar).first()
            assertEquals(VersePointer(irvmar, romans, 1, 1), actualIrvMarRomans, "Failed on searching: $mrTerm")

            val actualIrvMarRomans2 = bible.search(term = mrTerm, bookNumber = romans, startChapter = 2, translation = irvmar).first()
            assertEquals(VersePointer(irvmar, romans, 2, 16), actualIrvMarRomans2, "Failed on searching: $mrTerm")

            val actualIrvMarRomans3To5 = bible.search(term = mrTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvmar).first()
            assertEquals(VersePointer(irvmar, romans, 3, 22), actualIrvMarRomans3To5, "Failed on searching: $mrTerm")
        }


        // Urdu (ur)
        listOf("ईसा मसीह", "ईसा", "मसीह").forEach { urTerm ->
            val actualIrvUrd = bible.search(term = urTerm, translation = irvurd).first()
            assertEquals(VersePointer(irvurd, 40, 1, 1), actualIrvUrd, "Failed on searching: $urTerm")

            val actualIrvUrdRomans = bible.search(term = urTerm, bookNumber = romans, translation = irvurd).first()
            assertEquals(VersePointer(irvurd, romans, 1, 1), actualIrvUrdRomans, "Failed on searching: $urTerm")

            val actualIrvUrdRomans2 = bible.search(term = urTerm, bookNumber = romans, startChapter = 2, translation = irvurd).first()
            assertEquals(VersePointer(irvurd, romans, 2, 16), actualIrvUrdRomans2, "Failed on searching: $urTerm")

            val actualIrvUrdRomans3To5 = bible.search(term = urTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvurd).first()
            assertEquals(VersePointer(irvurd, romans, 3, 22), actualIrvUrdRomans3To5, "Failed on searching: $urTerm")
        }
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
