package org.gnit.bible.test

import org.apache.lucene.document.IntPoint
import org.gnit.bible.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchIndexTest {

    @Test
    fun getTextPathTest() {
        val versePointer = VersePointer(Translation.webus, book = 1, chapter = 3)
        val textPathString = chapterTextPath(versePointer)
        val aChapter = File("src/main/resources/$textPathString").readText()
        assertEquals(webusGen3, aChapter)
    }

    @Test
    fun indexDirTest() {
        Translation.values().forEach { translation ->
            val indexDir = "src/main/resources/texts/$translation/index"
            assertTrue(File(indexDir).exists())

            indexFiles.forEach { indexFile ->
                assertTrue(File("$indexDir/$indexFile").exists())
            }
        }
    }
}

class SearchCoreTest {

    @Test
    fun indexPathTest() {
        assertEquals(Path("src/main/resources/texts/webus/index"), indexPath(Translation.webus))
    }

    @Test
    fun chapterQueryTest() {
        assertEquals(IntPoint.newRangeQuery("chapter", 3, 3), chapterQuery(3, null))
        assertEquals(IntPoint.newRangeQuery("chapter", 4, 28), chapterQuery(4, 28))
    }

    @Test
    fun `search Jesus Christ in webus`() {
        val result = search("Jesus Christ", null, null, null, 100, Translation.webus)
        assertEquals(
            "Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans`() {
        val result = search("Jesus Christ", bookNumber("romans"), null, null, 100, Translation.webus)
        assertEquals(
            "Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans 2`() {
        val result = search("Jesus Christ", bookNumber("romans"), 2, null, 100, Translation.webus)
        assertEquals(
            "Romans 2:16 in the day when God will judge the secrets of men, according to my Good News, by Jesus Christ.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans 3-5`() {
        val result = search("Jesus Christ", bookNumber("romans"), 3, 5, 100, Translation.webus)
        assertEquals(
            "Romans 5:21 that as sin reigned in death, even so grace might reign through righteousness to eternal life through Jesus Christ our Lord.",
            result.last().trim()
        )
    }

    @Test
    fun `search Jesus Christ in kjv`() {
        val result = search("Jesus Christ", null, null, null, 100, Translation.kjv)
        assertEquals(
            "Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in cunp`() {
        val result = search("耶稣基督", null, null, null, 100, Translation.cunp)
        assertEquals(
            "Matthew 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in krv`() {
        val result = search("예수 그리스도", null, null, null, 100, Translation.krv)
        assertEquals(
            "Matthew 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in jc`() {
        val result = search("イエス・キリスト", null, null, null, 100, Translation.jc)
        assertEquals(
            "Matthew 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。",
            result.first().trim()
        )
    }
}

class SearchTest {

    private val searchCli = SearchCli(env = Environment.TEST, Config())

    @Test
    fun `bbl search Jesus Christ in webus`() {
        searchCli.parse(arrayOf("Jesus Christ"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in romans`() {
        searchCli.parse(arrayOf("Jesus Christ in rom"))
        assertEquals(
            "Romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in romans 2`() {
        searchCli.parse(arrayOf("Jesus Christ in rom 2"))
        assertEquals(
            "Romans 2:16 in the day when God will judge the secrets of men, according to my Good News, by Jesus Christ.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in romans 3-5`() {
        searchCli.parse(arrayOf("Jesus Christ in rom 3-5"))
        assertEquals(
            "Romans 5:21 that as sin reigned in death, even so grace might reign through righteousness to eternal life through Jesus Christ our Lord.",
            searchCli.result.last().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in kjv`() {
        assertEquals(Translation.kjv, searchCli.getTranslationFrom("Jesus Christ in kjv"))
        searchCli.parse(arrayOf("Jesus Christ in kjv"))
        assertEquals(Translation.kjv, searchCli.translation)
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans in kjv`() {
        searchCli.parse(arrayOf("Jesus Christ in rom in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans 2 in kjv`() {
        searchCli.parse(arrayOf("Jesus Christ in rom 2 in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Romans 2:16 In the day when God shall judge the secrets of men by Jesus Christ according to my gospel.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans 3-5 in kjv`() {
        searchCli.parse(arrayOf("Jesus Christ in rom 3-5 in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Romans 5:21 That as sin hath reigned unto death, even so might grace reign through righteousness unto eternal life by Jesus Christ our Lord.",
            searchCli.result.last().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in kjv in romans`() {
        searchCli.parse(arrayOf("Jesus Christ in kjv in rom"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "Romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in rvr09`() {
        searchCli.parse(arrayOf("Jesucristo in rvr09"))
        assertEquals(
            "Matthew 1:1 LIBRO de la generación de Jesucristo, hijo de David, hijo de Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in tb`() {
        searchCli.parse(arrayOf("Jesus Cristo in tb"))
        assertEquals(
            "Matthew 1:1 Livro da geração de Jesus Cristo, filho de Davi, filho de Abraão.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in delut`() {
        searchCli.parse(arrayOf("Jesu Christi in delut"))
        assertEquals(
            "Matthew 1:1 Dies ist das Buch von der Geburt Jesu Christi, der da ist ein Sohn Davids, des Sohnes Abrahams.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in lsg`() {
        searchCli.parse(arrayOf("Jésus-Christ in lsg"))
        assertEquals(
            "Matthew 1:1 Généalogie de Jésus-Christ, fils de David, fils d’Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in sinod`() {
        searchCli.parse(arrayOf("Иисуса Христа in sinod"))
        assertEquals(
            "Matthew 1:1 Родословие Иисуса Христа, Сына Давидова, Сына Авраамова.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in svrj`() {
        searchCli.parse(arrayOf("JEZUS CHRISTUS in svrj"))
        assertEquals(
            "Matthew 1:1 Het boek des geslachts van JEZUS CHRISTUS, den Zoon van David, den zoon van Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in rdv24`() {
        searchCli.parse(arrayOf("Gesù Cristo in rdv24"))
        assertEquals(
            "Matthew 1:1 Genealogia di Gesù Cristo figliuolo di Davide, figliuolo d'Abramo.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in ubg`() {
        searchCli.parse(arrayOf("Jezusa Chrystusa in ubg"))
        assertEquals(
            "Matthew 1:1 Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in ubio`() {
        searchCli.parse(arrayOf("Ісуса Христа in ubio"))
        assertEquals(
            "Matthew 1:1 Книга родоводу Ісуса Христа, Сина Давидового, Сина Авраамового:",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in sven`() {
        searchCli.parse(arrayOf("Jesu Kristi in sven"))
        assertEquals(
            "Matthew 1:1 Detta är Jesu Kristi, Davids sons, Abrahams sons, släkttavla.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in cunp`() {
        searchCli.parse(arrayOf("耶稣基督 in cunp"))
        assertEquals(
            "Matthew 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in krv`() {
        searchCli.parse(arrayOf("예수 그리스도 in krv"))
        assertEquals(
            "Matthew 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in jc`() {
        searchCli.parse(arrayOf("イエス・キリスト in jc"))
        assertEquals(
            "Matthew 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。",
            searchCli.result.first().trim()
        )
    }
}