package org.gnit.bible.test.search.person

import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.Bible
import org.gnit.bible.BibleFilter
import org.gnit.bible.Books
import org.gnit.bible.SupportedTranslation
import org.gnit.bible.SupportedTranslation.*
import org.gnit.bible.Translation
import org.gnit.bible.VersePointer
import kotlin.test.assertEquals

class NTGospelsPersonTest(
    private val bible: Bible,
    private val analyzerProvider: AnalyzerProvider,
    private val translationsToBeTested: List<SupportedTranslation>,
) {

    private val matthew: Int = Books.bookNumber("matthew")
    private val john: Int = Books.bookNumber("john")
    private val romans: Int = Books.bookNumber("romans")
    private val firstJohn: Int = Books.bookNumber("1john")

    private fun search(
        term: String,
        bookNumber: Int? = null,
        startChapter: Int? = null,
        endChapter: Int? = null,
        filter: BibleFilter? = null,
        translation: Translation,
    ): List<VersePointer> {
        return if (filter == null) {
            bible.search(
                term = term,
                bookNumber = bookNumber,
                startChapter = startChapter,
                endChapter = endChapter,
                translation = translation,
                analyzerProvider = analyzerProvider
            )
        } else {
            bible.search(
                term = term,
                bookNumber = bookNumber,
                startChapter = startChapter,
                endChapter = endChapter,
                filter = filter,
                translation = translation,
                analyzerProvider = analyzerProvider
            )
        }
    }

    fun runAllTests() {
        searchJesusChrist()
        // TODO add more search term specific test functions here as we implement more search person names.
        // fun searchXXX()
        // fun searchYYY()
        // ...

    }

    fun searchJesusChrist() {
        translationsToBeTested.forEach { supportedTranslation ->
            when (supportedTranslation) {
                WEBUS -> {
                    val webus = supportedTranslation.translation
                    val enTerms = listOf("Jesus Christ", "Jesus", "Christ")
                    enTerms.forEach { enTerm ->
                        val actualWebus = search(term = enTerm, translation = webus).first()
                        assertEquals(VersePointer(webus, 40, 1, 1), actualWebus, "Failed on searching: $enTerm")

                        val actualWebusRomans = search(term = enTerm, bookNumber = romans, translation = webus).first()
                        assertEquals(VersePointer(webus, romans, 1, 1), actualWebusRomans, "Failed on searching: $enTerm")

                        val actualWebusRomans2 = search(term = enTerm, bookNumber = romans, startChapter = 2, translation = webus).first()
                        assertEquals(VersePointer(webus, romans, 2, 16), actualWebusRomans2, "Failed on searching: $enTerm")

                        val actualWebusRomans3To5 = search(term = enTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = webus).first()
                        assertEquals(VersePointer(webus, romans, 3, 22), actualWebusRomans3To5, "Failed on searching: $enTerm")

                        val actualWebusJohnsLetter = search(term = enTerm, filter = Books.Category.filterOf("johns letters"), translation = webus).first()
                        assertEquals(VersePointer(webus, firstJohn, 1, 3), actualWebusJohnsLetter, "Failed on searching: $enTerm")
                    }

                    val enTermWept = "Jesus wept"
                    val actualMatthew26 = search(term = enTermWept, translation = webus).first() // the unquoted term still ranks Matthew 26:75 first in current search behavior.
                    // Matthew 26:75 Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” Then he went out and wept bitterly.
                    val expectedMatthew26 = VersePointer(webus, matthew, 26, 75)
                    assertEquals(expectedMatthew26, actualMatthew26, "Failed on searching: $enTermWept")

                    val enTermWeep = "Jesus weep"
                    val actualMatthew23byWeep = search(term = enTermWeep, translation = webus).first() // the "weep" and other variants of "wept" are normalized to the same root form "weep".
                    assertEquals(expectedMatthew26, actualMatthew23byWeep, "Failed on searching: $enTermWeep")

                    val actualJohn11 = search(term = """"$enTermWept"""", translation = webus).first() // the exact quot only appears in John 11:35, so it should come as top result.
                    // John 11:35 Jesus wept.
                    assertEquals(VersePointer(webus, john, 11, 35), actualJohn11, "Failed on searching exact quoted: $enTermWept")
                }

                KJV -> {
                    val kjv = supportedTranslation.translation
                    val enTerms = listOf("Jesus Christ", "Jesus", "Christ")
                    enTerms.forEach { enTerm ->
                        val actualKjv = search(term = enTerm, translation = kjv).first()
                        assertEquals(VersePointer(kjv, 40, 1, 1), actualKjv, "Failed on searching: $enTerm")

                        val actualKjvRomans = search(term = enTerm, bookNumber = romans, translation = kjv).first()
                        assertEquals(VersePointer(kjv, romans, 1, 1), actualKjvRomans, "Failed on searching: $enTerm")

                        val actualKjvRomans2 = search(term = enTerm, bookNumber = romans, startChapter = 2, translation = kjv).first()
                        assertEquals(VersePointer(kjv, romans, 2, 16), actualKjvRomans2, "Failed on searching: $enTerm")

                        val actualKjvRomans3To5 = search(term = enTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = kjv).first()
                        assertEquals(VersePointer(kjv, romans, 3, 22), actualKjvRomans3To5, "Failed on searching: $enTerm")

                        val actualKjvJohnsLetter = search(term = enTerm, filter = Books.Category.filterOf("johns letters"), translation = kjv).first()
                        assertEquals(VersePointer(kjv, firstJohn, 1, 3), actualKjvJohnsLetter, "Failed on searching: $enTerm")
                    }

                    val enTermWept = "Jesus wept"
                    val actualMatthew26 = search(term = enTermWept, translation = kjv).first() // the unquoted term still ranks Matthew 26:75 first in current search behavior.
                    // Matthew 26:75 Then Peter remembered the word which Jesus had said to him, “Before the rooster crows, you will deny me three times.” He went out and wept bitterly.
                    val expectedMatthew26 = VersePointer(kjv, matthew, 26, 75)
                    assertEquals(expectedMatthew26, actualMatthew26, "Failed on searching: $enTermWept")

                    val enTermWeep = "Jesus weep"
                    val actualMatthew23byWeep = search(term = enTermWeep, translation = kjv).first() // the "weep" and other variants of "wept" are normalized to the same root form "weep".
                    assertEquals(expectedMatthew26, actualMatthew23byWeep, "Failed on searching: $enTermWeep")

                    val actualJohn11 = search(term = """"$enTermWept"""", translation = kjv).first() // the exact quot only appears in John 11:35, so it should come as top result.
                    // John 11:35 Jesus wept.
                    assertEquals(VersePointer(kjv, john, 11, 35), actualJohn11, "Failed on searching exact quoted: $enTermWept")
                }

                RVR09 -> {
                    val rvr09 = supportedTranslation.translation
                    listOf("Jesucristo", "Jesús", "Cristo").forEach { esTerm ->
                        val actualRvr09 = search(term = esTerm, translation = rvr09).first()
                        assertEquals(VersePointer(rvr09, 40, 1, 1), actualRvr09, "Failed on searching: $esTerm")

                        val actualRvr09Romans = search(term = esTerm, bookNumber = romans, translation = rvr09).first()
                        assertEquals(VersePointer(rvr09, romans, 1, 1), actualRvr09Romans, "Failed on searching: $esTerm")

                        val actualRvr09Romans2 = search(term = esTerm, bookNumber = romans, startChapter = 2, translation = rvr09).first()
                        assertEquals(VersePointer(rvr09, romans, 2, 16), actualRvr09Romans2, "Failed on searching: $esTerm")

                        val actualRvr09Romans3To5 = search(term = esTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = rvr09).first()
                        assertEquals(VersePointer(rvr09, romans, 3, 22), actualRvr09Romans3To5, "Failed on searching: $esTerm")

                        val actualRvr09JohnsLetter = search(term = esTerm, filter = Books.Category.filterOf("johns letters"), translation = rvr09).first()
                        assertEquals(VersePointer(rvr09, firstJohn, 1, 3), actualRvr09JohnsLetter, "Failed on searching: $esTerm")
                    }
                }

                TB -> {
                    val tb = supportedTranslation.translation
                    listOf("Jesus Cristo", "Jesus", "Cristo").forEach { ptTerm ->
                        val actualTb = search(term = ptTerm, translation = tb).first()
                        assertEquals(VersePointer(tb, 40, 1, 1), actualTb, "Failed on searching: $ptTerm")

                        val actualTbRomans = search(term = ptTerm, bookNumber = romans, translation = tb).first()
                        assertEquals(VersePointer(tb, romans, 1, 1), actualTbRomans, "Failed on searching: $ptTerm")

                        val actualTbRomans2 = search(term = ptTerm, bookNumber = romans, startChapter = 2, translation = tb).first()
                        assertEquals(VersePointer(tb, romans, 2, 16), actualTbRomans2, "Failed on searching: $ptTerm")

                        val actualTbRomans3To5 = search(term = ptTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = tb).first()
                        assertEquals(VersePointer(tb, romans, 3, 22), actualTbRomans3To5, "Failed on searching: $ptTerm")

                        val actualTbJohnsLetter = search(term = ptTerm, filter = Books.Category.filterOf("johns letters"), translation = tb).first()
                        assertEquals(VersePointer(tb, firstJohn, 1, 3), actualTbJohnsLetter, "Failed on searching: $ptTerm")
                    }
                }

                DELUT -> {
                    val delut = supportedTranslation.translation
                    listOf("Jesu Christi", "Jesu", "Christi").forEach { deTerm ->
                        val actualDelut = search(term = deTerm, translation = delut).first()
                        assertEquals(VersePointer(delut, 40, 1, 1), actualDelut, "Failed on searching: $deTerm")

                        val actualDelutRomans = search(term = deTerm, bookNumber = romans, translation = delut).first()
                        assertEquals(VersePointer(delut, romans, 1, 1), actualDelutRomans, "Failed on searching: $deTerm")

                        val actualDelutRomans2 = search(term = deTerm, bookNumber = romans, startChapter = 2, translation = delut).first()
                        assertEquals(VersePointer(delut, romans, 2, 16), actualDelutRomans2, "Failed on searching: $deTerm")

                        val actualDelutRomans3To5 = search(term = deTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = delut).first()
                        assertEquals(VersePointer(delut, romans, 3, 22), actualDelutRomans3To5, "Failed on searching: $deTerm")

                        val actualDelutJohnsLetter = search(term = deTerm, filter = Books.Category.filterOf("johns letters"), translation = delut).first()
                        assertEquals(VersePointer(delut, firstJohn, 1, 3), actualDelutJohnsLetter, "Failed on searching: $deTerm")
                    }
                }

                LSG -> {
                    val lsg = supportedTranslation.translation
                    listOf("Jésus-Christ", "Jésus", "Christ").forEach { frTerm ->
                        val actualLsg = search(term = frTerm, translation = lsg).first()
                        assertEquals(VersePointer(lsg, 40, 1, 1), actualLsg, "Failed on searching: $frTerm")

                        val actualLsgRomans = search(term = frTerm, bookNumber = romans, translation = lsg).first()
                        assertEquals(VersePointer(lsg, romans, 1, 1), actualLsgRomans, "Failed on searching: $frTerm")

                        val actualLsgRomans2 = search(term = frTerm, bookNumber = romans, startChapter = 2, translation = lsg).first()
                        assertEquals(VersePointer(lsg, romans, 2, 16), actualLsgRomans2, "Failed on searching: $frTerm")

                        val actualLsgRomans3To5 = search(term = frTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = lsg).first()
                        assertEquals(VersePointer(lsg, romans, 3, 22), actualLsgRomans3To5, "Failed on searching: $frTerm")

                        val actualLsgJohnsLetter = search(term = frTerm, filter = Books.Category.filterOf("johns letters"), translation = lsg).first()
                        assertEquals(VersePointer(lsg, firstJohn, 1, 3), actualLsgJohnsLetter, "Failed on searching: $frTerm")
                    }
                }

                SINOD -> {
                    val sinod = supportedTranslation.translation
                    listOf("Иисуса Христа", "Иисуса", "Христа").forEach { ruTerm ->
                        val actualSinod = search(term = ruTerm, translation = sinod).first()
                        assertEquals(VersePointer(sinod, 40, 1, 1), actualSinod, "Failed on searching: $ruTerm")

                        val actualSinodRomans = search(term = ruTerm, bookNumber = romans, translation = sinod).first()
                        assertEquals(VersePointer(sinod, romans, 1, 1), actualSinodRomans, "Failed on searching: $ruTerm")

                        val actualSinodRomans2 = search(term = ruTerm, bookNumber = romans, startChapter = 2, translation = sinod).first()
                        assertEquals(VersePointer(sinod, romans, 2, 16), actualSinodRomans2, "Failed on searching: $ruTerm")

                        val actualSinodRomans3To5 = search(term = ruTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = sinod).first()
                        assertEquals(VersePointer(sinod, romans, 3, 22), actualSinodRomans3To5, "Failed on searching: $ruTerm")

                        val actualSinodJohnsLetter = search(term = ruTerm, filter = Books.Category.filterOf("johns letters"), translation = sinod).first()
                        assertEquals(VersePointer(sinod, firstJohn, 1, 3), actualSinodJohnsLetter, "Failed on searching: $ruTerm")
                    }
                }

                SVRJ -> {
                    val svrj = supportedTranslation.translation
                    listOf("JEZUS CHRISTUS", "JEZUS", "CHRISTUS").forEach { nlTerm ->
                        val actualSvrj = search(term = nlTerm, translation = svrj).first()
                        assertEquals(VersePointer(svrj, 40, 1, 1), actualSvrj, "Failed on searching: $nlTerm")

                        val actualSvrjRomans = search(term = nlTerm, bookNumber = romans, translation = svrj).first()
                        assertEquals(VersePointer(svrj, romans, 1, 1), actualSvrjRomans, "Failed on searching: $nlTerm")

                        val actualSvrjRomans2 = search(term = nlTerm, bookNumber = romans, startChapter = 2, translation = svrj).first()
                        assertEquals(VersePointer(svrj, romans, 2, 16), actualSvrjRomans2, "Failed on searching: $nlTerm")

                        val actualSvrjRomans3To5 = search(term = nlTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = svrj).first()
                        assertEquals(VersePointer(svrj, romans, 3, 22), actualSvrjRomans3To5, "Failed on searching: $nlTerm")

                        val actualSvrjJohnsLetter = search(term = nlTerm, filter = Books.Category.filterOf("johns letters"), translation = svrj).first()
                        assertEquals(VersePointer(svrj, firstJohn, 1, 3), actualSvrjJohnsLetter, "Failed on searching: $nlTerm")
                    }
                }

                RDV24 -> {
                    val rdv24 = supportedTranslation.translation
                    listOf("Gesù Cristo", "Gesù", "Cristo").forEach { itTerm ->
                        val actualRdv24 = search(term = itTerm, translation = rdv24).first()
                        assertEquals(VersePointer(rdv24, 40, 1, 1), actualRdv24, "Failed on searching: $itTerm")

                        val actualRdv24Romans = search(term = itTerm, bookNumber = romans, translation = rdv24).first()
                        assertEquals(VersePointer(rdv24, romans, 1, 1), actualRdv24Romans, "Failed on searching: $itTerm")

                        val actualRdv24Romans2 = search(term = itTerm, bookNumber = romans, startChapter = 2, translation = rdv24).first()
                        assertEquals(VersePointer(rdv24, romans, 2, 16), actualRdv24Romans2, "Failed on searching: $itTerm")

                        val actualRdv24Romans3To5 = search(term = itTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = rdv24).first()
                        assertEquals(VersePointer(rdv24, romans, 3, 22), actualRdv24Romans3To5, "Failed on searching: $itTerm")

                        val actualRdv24JohnsLetter = search(term = itTerm, filter = Books.Category.filterOf("johns letters"), translation = rdv24).first()
                        assertEquals(VersePointer(rdv24, firstJohn, 1, 3), actualRdv24JohnsLetter, "Failed on searching: $itTerm")
                    }
                }

                UBG -> {
                    val ubg = supportedTranslation.translation
                    listOf("Jezusa Chrystusa", "Jezusa", "Chrystusa").forEach { plTerm ->
                        val actualUbg = search(term = plTerm, translation = ubg).first()
                        assertEquals(VersePointer(ubg, 40, 1, 1), actualUbg, "Failed on searching: $plTerm")

                        val actualUbgRomans = search(term = plTerm, bookNumber = romans, translation = ubg).first()
                        assertEquals(VersePointer(ubg, romans, 1, 1), actualUbgRomans, "Failed on searching: $plTerm")

                        val actualUbgRomans2 = search(term = plTerm, bookNumber = romans, startChapter = 2, translation = ubg).first()
                        assertEquals(VersePointer(ubg, romans, 2, 16), actualUbgRomans2, "Failed on searching: $plTerm")

                        val actualUbgRomans3To5 = search(term = plTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ubg).first()
                        assertEquals(VersePointer(ubg, romans, 3, 22), actualUbgRomans3To5, "Failed on searching: $plTerm")

                        val actualUbgJohnsLetter = search(term = plTerm, filter = Books.Category.filterOf("johns letters"), translation = ubg).first()
                        assertEquals(VersePointer(ubg, firstJohn, 1, 3), actualUbgJohnsLetter, "Failed on searching: $plTerm")
                    }
                }

                UBIO -> {
                    val ubio = supportedTranslation.translation
                    listOf("Ісуса Христа", "Ісуса", "Христа").forEach { ukTerm ->
                        val actualUbio = search(term = ukTerm, translation = ubio).first()
                        assertEquals(VersePointer(ubio, 40, 1, 1), actualUbio, "Failed on searching: $ukTerm")

                        val actualUbioRomans = search(term = ukTerm, bookNumber = romans, translation = ubio).first()
                        assertEquals(VersePointer(ubio, romans, 1, 1), actualUbioRomans, "Failed on searching: $ukTerm")

                        val actualUbioRomans2 = search(term = ukTerm, bookNumber = romans, startChapter = 2, translation = ubio).first()
                        assertEquals(VersePointer(ubio, romans, 2, 16), actualUbioRomans2, "Failed on searching: $ukTerm")

                        val actualUbioRomans3To5 = search(term = ukTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ubio).first()
                        assertEquals(VersePointer(ubio, romans, 3, 22), actualUbioRomans3To5, "Failed on searching: $ukTerm")

                        val actualUbioJohnsLetter = search(term = ukTerm, filter = Books.Category.filterOf("johns letters"), translation = ubio).first()
                        assertEquals(VersePointer(ubio, firstJohn, 1, 3), actualUbioJohnsLetter, "Failed on searching: $ukTerm")
                    }
                }

                SVEN -> {
                    val sven = supportedTranslation.translation
                    listOf("Jesu Kristi", "Jesu", "Kristi").forEach { svTerm ->
                        val actualSven = search(term = svTerm, translation = sven).first()
                        assertEquals(VersePointer(sven, 40, 1, 1), actualSven, "Failed on searching: $svTerm")

                        val actualSvenRomans = search(term = svTerm, bookNumber = romans, translation = sven).first()
                        assertEquals(VersePointer(sven, romans, 1, 1), actualSvenRomans, "Failed on searching: $svTerm")

                        val actualSvenRomans2 = search(term = svTerm, bookNumber = romans, startChapter = 2, translation = sven).first()
                        assertEquals(VersePointer(sven, romans, 2, 16), actualSvenRomans2, "Failed on searching: $svTerm")

                        val actualSvenRomans3To5 = search(term = svTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = sven).first()
                        assertEquals(VersePointer(sven, romans, 3, 22), actualSvenRomans3To5, "Failed on searching: $svTerm")

                        val actualSvenJohnsLetter = search(term = svTerm, filter = Books.Category.filterOf("johns letters"), translation = sven).first()
                        assertEquals(VersePointer(sven, firstJohn, 1, 3), actualSvenJohnsLetter, "Failed on searching: $svTerm")
                    }
                }

                CUNP -> {
                    val cunp = supportedTranslation.translation
                    listOf("耶稣基督", "耶稣", "基督").forEach { zhTerm ->
                        val actualCunp = search(term = zhTerm, translation = cunp).first()
                        assertEquals(VersePointer(cunp, 40, 1, 1), actualCunp, "Failed on searching: $zhTerm")

                        val actualCunpRomans = search(term = zhTerm, bookNumber = romans, translation = cunp).first()
                        assertEquals(VersePointer(cunp, romans, 1, 1), actualCunpRomans, "Failed on searching: $zhTerm")

                        val actualCunpRomans2 = search(term = zhTerm, bookNumber = romans, startChapter = 2, translation = cunp).first()
                        assertEquals(VersePointer(cunp, romans, 2, 16), actualCunpRomans2, "Failed on searching: $zhTerm")

                        val actualCunpRomans3To5 = search(term = zhTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = cunp).first()
                        assertEquals(VersePointer(cunp, romans, 3, 22), actualCunpRomans3To5, "Failed on searching: $zhTerm")

                        val actualCunpJohnsLetter = search(term = zhTerm, filter = Books.Category.filterOf("johns letters"), translation = cunp).first()
                        assertEquals(VersePointer(cunp, firstJohn, 1, 3), actualCunpJohnsLetter, "Failed on searching: $zhTerm")
                    }
                }

                KRV -> {
                    val krv = supportedTranslation.translation
                    listOf("예수그리스도", "예수 그리스도", "예수", "그리스도").forEach { koTerm ->
                        val actualKrv = search(term = koTerm, translation = krv).first()
                        assertEquals(VersePointer(krv, 40, 1, 1), actualKrv, "Failed on searching: $koTerm")

                        val actualKrvRomans = search(term = koTerm, bookNumber = romans, translation = krv).first()
                        assertEquals(VersePointer(krv, romans, 1, 1), actualKrvRomans, "Failed on searching: $koTerm")

                        val actualKrvRomans2 = search(term = koTerm, bookNumber = romans, startChapter = 2, translation = krv).first()
                        assertEquals(VersePointer(krv, romans, 2, 16), actualKrvRomans2, "Failed on searching: $koTerm")

                        val actualKrvRomans3To5 = search(term = koTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = krv).first()
                        assertEquals(VersePointer(krv, romans, 3, 22), actualKrvRomans3To5, "Failed on searching: $koTerm")

                        val actualKrvJohnsLetter = search(term = koTerm, filter = Books.Category.filterOf("johns letters"), translation = krv).first()
                        assertEquals(VersePointer(krv, firstJohn, 1, 3), actualKrvJohnsLetter, "Failed on searching: $koTerm")
                    }
                }

                JC -> {
                    val jc = supportedTranslation.translation
                    listOf("イエス・キリスト", "イエス", "キリスト").forEach { jaTerm ->
                        val actualJc = search(term = jaTerm, translation = jc).first()
                        assertEquals(VersePointer(jc, 40, 1, 1), actualJc, "Failed on searching: $jaTerm")

                        val actualJcRomans = search(term = jaTerm, bookNumber = romans, translation = jc).first()
                        assertEquals(VersePointer(jc, romans, 1, 1), actualJcRomans, "Failed on searching: $jaTerm")

                        val actualJcRomans2 = search(term = jaTerm, bookNumber = romans, startChapter = 2, translation = jc).first()
                        assertEquals(VersePointer(jc, romans, 2, 16), actualJcRomans2, "Failed on searching: $jaTerm")

                        val actualJcRomans3To5 = search(term = jaTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = jc).first()
                        assertEquals(VersePointer(jc, romans, 3, 22), actualJcRomans3To5, "Failed on searching: $jaTerm")

                        val actualJcJohnsLetter = search(term = jaTerm, filter = Books.Category.filterOf("johns letters"), translation = jc).first()
                        assertEquals(VersePointer(jc, firstJohn, 1, 3), actualJcJohnsLetter, "Failed on searching: $jaTerm")
                    }
                }

                AYT -> {
                    val ayt = supportedTranslation.translation
                    listOf("Yesus Kristus", "Yesus", "Kristus").forEach { idTerm ->
                        val actualAyt = search(term = idTerm, translation = ayt).first()
                        assertEquals(VersePointer(ayt, 40, 1, 1), actualAyt, "Failed on searching: $idTerm")

                        val actualAytRomans = search(term = idTerm, bookNumber = romans, translation = ayt).first()
                        assertEquals(VersePointer(ayt, romans, 1, 1), actualAytRomans, "Failed on searching: $idTerm")

                        val actualAytRomans2 = search(term = idTerm, bookNumber = romans, startChapter = 2, translation = ayt).first()
                        assertEquals(VersePointer(ayt, romans, 2, 16), actualAytRomans2, "Failed on searching: $idTerm")

                        val actualAytRomans3To5 = search(term = idTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = ayt).first()
                        assertEquals(VersePointer(ayt, romans, 3, 22), actualAytRomans3To5, "Failed on searching: $idTerm")

                        val actualAytJohnsLetter = search(term = idTerm, filter = Books.Category.filterOf("johns letters"), translation = ayt).first()
                        assertEquals(VersePointer(ayt, firstJohn, 1, 3), actualAytJohnsLetter, "Failed on searching: $idTerm")
                    }
                }

                TH1971 -> {
                    val th1971 = supportedTranslation.translation
                    listOf("พระเยซูคริสต์", "พระเยซู", "คริสต์").forEach { thTerm ->
                        val actualTh1971 = search(term = thTerm, translation = th1971).first()
                        assertEquals(VersePointer(th1971, 40, 1, 1), actualTh1971, "Failed on searching: $thTerm")

                        val actualTh1971Romans = search(term = thTerm, bookNumber = romans, translation = th1971).first()
                        assertEquals(VersePointer(th1971, romans, 1, 1), actualTh1971Romans, "Failed on searching: $thTerm")

                        val actualTh1971Romans2 = search(term = thTerm, bookNumber = romans, startChapter = 2, translation = th1971).first()
                        assertEquals(VersePointer(th1971, romans, 2, 16), actualTh1971Romans2, "Failed on searching: $thTerm")

                        val actualTh1971Romans3To5 = search(term = thTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = th1971).first()
                        assertEquals(VersePointer(th1971, romans, 3, 22), actualTh1971Romans3To5, "Failed on searching: $thTerm")

                        val actualTh1971JohnsLetter = search(term = thTerm, filter = Books.Category.filterOf("johns letters"), translation = th1971).first()
                        assertEquals(VersePointer(th1971, firstJohn, 1, 3), actualTh1971JohnsLetter, "Failed on searching: $thTerm")
                    }
                }

                IRVHIN -> {
                    val irvhin = supportedTranslation.translation
                    listOf("यीशु मसीह", "यीशु", "मसीह").forEach { hiTerm ->
                        val actualIrvHin = search(term = hiTerm, translation = irvhin).first()
                        assertEquals(VersePointer(irvhin, 40, 1, 1), actualIrvHin, "Failed on searching: $hiTerm")

                        val actualIrvHinRomans = search(term = hiTerm, bookNumber = romans, translation = irvhin).first()
                        assertEquals(VersePointer(irvhin, romans, 1, 1), actualIrvHinRomans, "Failed on searching: $hiTerm")

                        val actualIrvHinRomans2 = search(term = hiTerm, bookNumber = romans, startChapter = 2, translation = irvhin).first()
                        assertEquals(VersePointer(irvhin, romans, 2, 16), actualIrvHinRomans2, "Failed on searching: $hiTerm")

                        val actualIrvHinRomans3To5 = search(term = hiTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvhin).first()
                        assertEquals(VersePointer(irvhin, romans, 3, 22), actualIrvHinRomans3To5, "Failed on searching: $hiTerm")

                        val actualIrvHinJohnsLetter = search(term = hiTerm, filter = Books.Category.filterOf("johns letters"), translation = irvhin).first()
                        assertEquals(VersePointer(irvhin, firstJohn, 1, 3), actualIrvHinJohnsLetter, "Failed on searching: $hiTerm")
                    }
                }

                IRVBEN -> {
                    val irvben = supportedTranslation.translation
                    listOf("যীশু খ্রীষ্ট", "যীশু", "খ্রীষ্ট").forEach { bnTerm ->
                        val actualIrvBen = search(term = bnTerm, translation = irvben).first()
                        assertEquals(VersePointer(irvben, 40, 1, 1), actualIrvBen, "Failed on searching: $bnTerm")

                        val actualIrvBenRomans = search(term = bnTerm, bookNumber = romans, translation = irvben).first()
                        assertEquals(VersePointer(irvben, romans, 1, 1), actualIrvBenRomans, "Failed on searching: $bnTerm")

                        val actualIrvBenRomans2 = search(term = bnTerm, bookNumber = romans, startChapter = 2, translation = irvben).first()
                        assertEquals(VersePointer(irvben, romans, 2, 16), actualIrvBenRomans2, "Failed on searching: $bnTerm")

                        val actualIrvBenRomans3To5 = search(term = bnTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvben).first()
                        assertEquals(VersePointer(irvben, romans, 3, 22), actualIrvBenRomans3To5, "Failed on searching: $bnTerm")

                        val actualIrvBenJohnsLetter = search(term = bnTerm, filter = Books.Category.filterOf("johns letters"), translation = irvben).first()
                        assertEquals(VersePointer(irvben, firstJohn, 1, 3), actualIrvBenJohnsLetter, "Failed on searching: $bnTerm")
                    }
                }

                IRVTAM -> {
                    val irvtam = supportedTranslation.translation
                    listOf("இயேசுகிறிஸ்து", "இயேசு", "கிறிஸ்து").forEach { taTerm ->
                        val actualIrvTam = search(term = taTerm, translation = irvtam).first()
                        assertEquals(VersePointer(irvtam, 40, 1, 1), actualIrvTam, "Failed on searching: $taTerm")

                        val actualIrvTamRomans = search(term = taTerm, bookNumber = romans, translation = irvtam).first()
                        assertEquals(VersePointer(irvtam, romans, 1, 1), actualIrvTamRomans, "Failed on searching: $taTerm")

                        val actualIrvTamRomans2 = search(term = taTerm, bookNumber = romans, startChapter = 2, translation = irvtam).first()
                        assertEquals(VersePointer(irvtam, romans, 2, 16), actualIrvTamRomans2, "Failed on searching: $taTerm")

                        val actualIrvTamRomans3To5 = search(term = taTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvtam).first()
                        assertEquals(VersePointer(irvtam, romans, 3, 22), actualIrvTamRomans3To5, "Failed on searching: $taTerm")

                        val actualIrvTamJohnsLetter = search(term = taTerm, filter = Books.Category.filterOf("johns letters"), translation = irvtam).first()
                        assertEquals(VersePointer(irvtam, firstJohn, 1, 3), actualIrvTamJohnsLetter, "Failed on searching: $taTerm")
                    }
                }

                NPIULB -> {
                    val npiulb = supportedTranslation.translation
                    listOf("येशू ख्रीष्‍ट", "येशू", "ख्रीष्‍ट").forEach { neTerm ->
                        val actualNpiUlb = search(term = neTerm, translation = npiulb).first()
                        assertEquals(VersePointer(npiulb, 40, 1, 1), actualNpiUlb, "Failed on searching: $neTerm")

                        val actualNpiUlbRomans = search(term = neTerm, bookNumber = romans, translation = npiulb).first()
                        assertEquals(VersePointer(npiulb, romans, 1, 1), actualNpiUlbRomans, "Failed on searching: $neTerm")

                        val actualNpiUlbRomans2 = search(term = neTerm, bookNumber = romans, startChapter = 2, translation = npiulb).first()
                        assertEquals(VersePointer(npiulb, romans, 2, 16), actualNpiUlbRomans2, "Failed on searching: $neTerm")

                        val actualNpiUlbRomans3To5 = search(term = neTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = npiulb).first()
                        assertEquals(VersePointer(npiulb, romans, 3, 22), actualNpiUlbRomans3To5, "Failed on searching: $neTerm")

                        val actualNpiUlbJohnsLetter = search(term = neTerm, filter = Books.Category.filterOf("johns letters"), translation = npiulb).first()
                        assertEquals(VersePointer(npiulb, firstJohn, 1, 3), actualNpiUlbJohnsLetter, "Failed on searching: $neTerm")
                    }
                }

                ABTAG -> {
                    val abtag = supportedTranslation.translation
                    listOf("Jesucristo", "Jesus", "Cristo").forEach { tlTerm ->
                        val actualAbtag = search(term = tlTerm, translation = abtag).first()
                        assertEquals(VersePointer(abtag, 40, 1, 1), actualAbtag, "Failed on searching: $tlTerm")

                        val actualAbtagRomans = search(term = tlTerm, bookNumber = romans, translation = abtag).first()
                        assertEquals(VersePointer(abtag, romans, 1, 1), actualAbtagRomans, "Failed on searching: $tlTerm")

                        val actualAbtagRomans2 = search(term = tlTerm, bookNumber = romans, startChapter = 2, translation = abtag).first()
                        assertEquals(VersePointer(abtag, romans, 2, 16), actualAbtagRomans2, "Failed on searching: $tlTerm")

                        val actualAbtagRomans3To5 = search(term = tlTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = abtag).first()
                        assertEquals(VersePointer(abtag, romans, 3, 22), actualAbtagRomans3To5, "Failed on searching: $tlTerm")

                        val actualAbtagJohnsLetter = search(term = tlTerm, filter = Books.Category.filterOf("johns letters"), translation = abtag).first()
                        assertEquals(VersePointer(abtag, firstJohn, 1, 3), actualAbtagJohnsLetter, "Failed on searching: $tlTerm")
                    }
                }

                KTTV -> {
                    val kttv = supportedTranslation.translation
                    listOf("Jêsus-Christ", "Jêsus", "Christ").forEach { viTerm ->
                        val actualKttv = search(term = viTerm, translation = kttv).first()
                        assertEquals(VersePointer(kttv, 40, 1, 1), actualKttv, "Failed on searching: $viTerm")

                        val actualKttvRomans = search(term = viTerm, bookNumber = romans, translation = kttv).first()
                        assertEquals(VersePointer(kttv, romans, 1, 1), actualKttvRomans, "Failed on searching: $viTerm")

                        val actualKttvRomans2 = search(term = viTerm, bookNumber = romans, startChapter = 2, translation = kttv).first()
                        assertEquals(VersePointer(kttv, romans, 2, 16), actualKttvRomans2, "Failed on searching: $viTerm")

                        val actualKttvRomans3To5 = search(term = viTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = kttv).first()
                        assertEquals(VersePointer(kttv, romans, 3, 22), actualKttvRomans3To5, "Failed on searching: $viTerm")

                        val actualKttvJohnsLetter = search(term = viTerm, filter = Books.Category.filterOf("johns letters"), translation = kttv).first()
                        assertEquals(VersePointer(kttv, firstJohn, 1, 3), actualKttvJohnsLetter, "Failed on searching: $viTerm")
                    }
                }

                IRVGUJ -> {
                    val irvguj = supportedTranslation.translation
                    listOf("ઈસુ ખ્રિસ્ત", "ઈસુ", "ખ્રિસ્ત").forEach { guTerm ->
                        val actualIrvGuj = search(term = guTerm, translation = irvguj).first()
                        assertEquals(VersePointer(irvguj, 40, 1, 1), actualIrvGuj, "Failed on searching: $guTerm")

                        val actualIrvGujRomans = search(term = guTerm, bookNumber = romans, translation = irvguj).first()
                        assertEquals(VersePointer(irvguj, romans, 1, 1), actualIrvGujRomans, "Failed on searching: $guTerm")

                        val actualIrvGujRomans2 = search(term = guTerm, bookNumber = romans, startChapter = 2, translation = irvguj).first()
                        assertEquals(VersePointer(irvguj, romans, 2, 16), actualIrvGujRomans2, "Failed on searching: $guTerm")

                        val actualIrvGujRomans3To5 = search(term = guTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvguj).first()
                        assertEquals(VersePointer(irvguj, romans, 3, 22), actualIrvGujRomans3To5, "Failed on searching: $guTerm")

                        val actualIrvGujJohnsLetter = search(term = guTerm, filter = Books.Category.filterOf("johns letters"), translation = irvguj).first()
                        assertEquals(VersePointer(irvguj, firstJohn, 1, 3), actualIrvGujJohnsLetter, "Failed on searching: $guTerm")
                    }
                }

                IRVMAR -> {
                    val irvmar = supportedTranslation.translation
                    listOf("येशू ख्रिस्त", "येशू", "ख्रिस्त").forEach { mrTerm ->
                        val actualIrvMar = search(term = mrTerm, translation = irvmar).first()
                        assertEquals(VersePointer(irvmar, 40, 1, 1), actualIrvMar, "Failed on searching: $mrTerm")

                        val actualIrvMarRomans = search(term = mrTerm, bookNumber = romans, translation = irvmar).first()
                        assertEquals(VersePointer(irvmar, romans, 1, 1), actualIrvMarRomans, "Failed on searching: $mrTerm")

                        val actualIrvMarRomans2 = search(term = mrTerm, bookNumber = romans, startChapter = 2, translation = irvmar).first()
                        assertEquals(VersePointer(irvmar, romans, 2, 16), actualIrvMarRomans2, "Failed on searching: $mrTerm")

                        val actualIrvMarRomans3To5 = search(term = mrTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvmar).first()
                        assertEquals(VersePointer(irvmar, romans, 3, 22), actualIrvMarRomans3To5, "Failed on searching: $mrTerm")

                        val actualIrvMarJohnsLetter = search(term = mrTerm, filter = Books.Category.filterOf("johns letters"), translation = irvmar).first()
                        assertEquals(VersePointer(irvmar, firstJohn, 1, 3), actualIrvMarJohnsLetter, "Failed on searching: $mrTerm")
                    }
                }

                IRVTEL -> {
                    val irvtel = supportedTranslation.translation
                    listOf("యేసు క్రీస్తు", "యేసు", "క్రీస్తు").forEach { teTerm ->
                        val actualIrvTel = search(term = teTerm, translation = irvtel).first()
                        assertEquals(VersePointer(irvtel, 40, 1, 1), actualIrvTel, "Failed on searching: $teTerm")

                        val actualIrvTelRomans = search(term = teTerm, bookNumber = romans, translation = irvtel).first()
                        assertEquals(VersePointer(irvtel, romans, 1, 1), actualIrvTelRomans, "Failed on searching: $teTerm")

                        val actualIrvTelRomans2 = search(term = teTerm, bookNumber = romans, startChapter = 2, translation = irvtel).first()
                        assertEquals(VersePointer(irvtel, romans, 2, 16), actualIrvTelRomans2, "Failed on searching: $teTerm")

                        val actualIrvTelRomans3To5 = search(term = teTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvtel).first()
                        assertEquals(VersePointer(irvtel, romans, 3, 22), actualIrvTelRomans3To5, "Failed on searching: $teTerm")

                        val actualIrvTelJohnsLetter = search(term = teTerm, filter = Books.Category.filterOf("johns letters"), translation = irvtel).first()
                        assertEquals(VersePointer(irvtel, firstJohn, 1, 3), actualIrvTelJohnsLetter, "Failed on searching: $teTerm")
                    }
                }

                IRVURD -> {
                    val irvurd = supportedTranslation.translation
                    val urTermJesusChrist = "ईसा मसीह"
                    val urTermJesus = "ईसा"
                    val urTermChrist = "मसीह"

                    listOf(urTermJesusChrist, urTermJesus).forEach { urTerm ->
                        val actualIrvUrd = search(term = urTerm, translation = irvurd).first()
                        assertEquals(VersePointer(irvurd, 40, 1, 1), actualIrvUrd, "Failed on searching: $urTerm")

                        val actualIrvUrdRomans = search(term = urTerm, bookNumber = romans, translation = irvurd).first()
                        assertEquals(VersePointer(irvurd, romans, 1, 1), actualIrvUrdRomans, "Failed on searching: $urTerm")

                        val actualIrvUrdRomans2 = search(term = urTerm, bookNumber = romans, startChapter = 2, translation = irvurd).first()
                        assertEquals(VersePointer(irvurd, romans, 2, 16), actualIrvUrdRomans2, "Failed on searching: $urTerm")

                        val actualIrvUrdRomans3To5 = search(term = urTerm, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvurd).first()
                        assertEquals(VersePointer(irvurd, romans, 3, 22), actualIrvUrdRomans3To5, "Failed on searching: $urTerm")

                        val actualIrvUrdJohnsLetter = search(term = urTerm, filter = Books.Category.filterOf("johns letters"), translation = irvurd).first()
                        assertEquals(VersePointer(irvurd, firstJohn, 1, 3), actualIrvUrdJohnsLetter, "Failed on searching: $urTerm")
                    }

                    val actualIrvUrdChrist = search(term = urTermChrist, translation = irvurd).first()
                    assertEquals(VersePointer(irvurd, 19, 2, 2), actualIrvUrdChrist, "Failed on searching: $urTermChrist")

                    val actualIrvUrdChristRomans = search(term = urTermChrist, bookNumber = romans, translation = irvurd).first()
                    assertEquals(VersePointer(irvurd, romans, 1, 1), actualIrvUrdChristRomans, "Failed on searching: $urTermChrist")

                    val actualIrvUrdChristRomans2 = search(term = urTermChrist, bookNumber = romans, startChapter = 2, translation = irvurd).first()
                    assertEquals(VersePointer(irvurd, romans, 2, 16), actualIrvUrdChristRomans2, "Failed on searching: $urTermChrist")

                    val actualIrvUrdChristRomans3To5 = search(term = urTermChrist, bookNumber = romans, startChapter = 3, endChapter = 5, translation = irvurd).first()
                    assertEquals(VersePointer(irvurd, romans, 3, 22), actualIrvUrdChristRomans3To5, "Failed on searching: $urTermChrist")

                    val actualIrvUrdChristJohnsLetter = search(term = urTermChrist, filter = Books.Category.filterOf("johns letters"), translation = irvurd).first()
                    assertEquals(VersePointer(irvurd, firstJohn, 1, 3), actualIrvUrdChristJohnsLetter, "Failed on searching: $urTermChrist")
                }
            }
        }
    }

    // fun searchXXX(){}
    // fun searchYYY(){}
    // ...
}
