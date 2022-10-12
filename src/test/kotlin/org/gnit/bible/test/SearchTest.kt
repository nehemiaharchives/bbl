package org.gnit.bible.test

import org.apache.lucene.document.IntPoint
import org.gnit.bible.*
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals

class SearchCoreTest {

    @Test
    fun indexPathTest() {
        assertEquals(Path("src/main/resources/index/webus"), indexPath(Translation.webus))
    }

    @Test
    fun chapterQueryTest() {
        assertEquals(IntPoint.newRangeQuery("chapter", 3, 3),  chapterQuery(3, null))
        assertEquals(IntPoint.newRangeQuery("chapter", 4, 28), chapterQuery(4, 28))
    }

    @Test
    fun `search Jesus Christ in webus`() {
        val result = search("Jesus Christ", null, null, null, 100, Translation.webus)
        assertEquals(
            "matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans`() {
        val result = search("Jesus Christ", bookNumber("romans"), null, null, 100, Translation.webus)
        assertEquals(
            "romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans 2`() {
        val result = search("Jesus Christ", bookNumber("romans"), 2, null, 100, Translation.webus)
        assertEquals(
            "romans 2:16 in the day when God will judge the secrets of men, according to my Good News, by Jesus Christ.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in webus in Romans 3-5`() {
        val result = search("Jesus Christ", bookNumber("romans"), 3, 5, 100, Translation.webus)
        assertEquals(
            "romans 5:21 that as sin reigned in death, even so grace might reign through righteousness to eternal life through Jesus Christ our Lord.",
            result.last().trim()
        )
    }

    @Test
    fun `search Jesus Christ in kjv`() {
        val result = search("Jesus Christ", null, null, null, 100, Translation.kjv)
        assertEquals(
            "matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in cunp`() {
        val result = search("耶稣基督", null, null, null, 100, Translation.cunp)
        assertEquals(
            "matthew 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：",
            result.first().trim()
        )
    }

    @Test
    fun `search Jesus Christ in krv`() {
        val result = search("예수 그리스도", null, null, null, 100, Translation.krv)
        assertEquals(
            "matthew 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라",
            result[8].trim()
        )
    }

    @Test
    fun `search Jesus Christ in jc`() {
        val result = search("イエス・キリスト", null, null, null, 100, Translation.jc)
        assertEquals(
            "matthew 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。",
            result.first().trim()
        )
    }
}

class SearchTest{

    private val searchCli = Search(env = Environment.TEST, Config())

    @Test
    fun `bbl search Jesus Christ in webus`(){
        searchCli.parse(arrayOf("Jesus Christ"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "matthew 1:1 The book of the genealogy of Jesus Christ, the son of David, the son of Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in Romans`(){
        searchCli.parse(arrayOf("Jesus Christ in rom"))
        assertEquals(
            "romans 1:1 Paul, a servant of Jesus Christ, called to be an apostle, set apart for the Good News of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in Romans 2`(){
        searchCli.parse(arrayOf("Jesus Christ in rom 2"))
        assertEquals(
            "romans 2:16 in the day when God will judge the secrets of men, according to my Good News, by Jesus Christ.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in webus in Romans 3-5`() {
        searchCli.parse(arrayOf("Jesus Christ in rom 3-5"))
        assertEquals(
            "romans 5:21 that as sin reigned in death, even so grace might reign through righteousness to eternal life through Jesus Christ our Lord.",
            searchCli.result.last().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in kjv`(){
        assertEquals(Translation.kjv, searchCli.getTranslationFrom("Jesus Christ in kjv"))
        searchCli.parse(arrayOf("Jesus Christ in kjv"))
        assertEquals(Translation.kjv, searchCli.translation)
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "matthew 1:1 The book of the generation of Jesus Christ, the son of David, the son of Abraham.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans in kjv`(){
        searchCli.parse(arrayOf("Jesus Christ in romans in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans 2 in kjv`(){
        searchCli.parse(arrayOf("Jesus Christ in romans 2 in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "romans 2:16 In the day when God shall judge the secrets of men by Jesus Christ according to my gospel.",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in romans 3-5 in kjv`(){
        searchCli.parse(arrayOf("Jesus Christ in romans 3-5 in kjv"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "romans 5:21 That as sin hath reigned unto death, even so might grace reign through righteousness unto eternal life by Jesus Christ our Lord.",
            searchCli.result.last().trim()
        )
    }

    @Test
    fun `bbl search Jesus Christ in kjv in romans`(){
        searchCli.parse(arrayOf("Jesus Christ in kjv in romans"))
        assertEquals("Jesus Christ", searchCli.term)
        assertEquals(
            "romans 1:1 Paul, a servant of Jesus Christ, called [to be] an apostle, separated unto the gospel of God,",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in cunp`() {
        searchCli.parse(arrayOf("耶稣基督 in cunp"))
        assertEquals(
            "matthew 1:1 亚伯拉罕 的后裔， 大卫 的子孙 ，耶稣基督的家谱：",
            searchCli.result.first().trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in krv`() {
        searchCli.parse(arrayOf("예수 그리스도 in krv"))
        assertEquals(
            "matthew 1:1 아브라함과 다윗의 자손 예수 그리스도의 세계라",
            searchCli.result[8].trim()
        )
    }

    @Test
    fun `cli search Jesus Christ in jc`() {
        searchCli.parse(arrayOf("イエス・キリスト in jc"))
        assertEquals(
            "matthew 1:1 アブラハムの子であるダビデの子、イエス・キリストの系図。",
            searchCli.result.first().trim()
        )
    }
}