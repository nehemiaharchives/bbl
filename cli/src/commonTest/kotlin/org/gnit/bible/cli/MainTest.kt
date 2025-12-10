package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import org.gnit.bible.jcGenesisChapterOne
import org.gnit.bible.webusGenesisChapterOne
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MainTest {

    @Test
    fun testBblWithVersionFlag(){
        val command = Bbl()
        val result = command.test(listOf("-v"))
        assertContains(result.stdout, "While you are in front of your console, you are not alone. God is with you.")
    }

    @Test
    fun testBblWithNoArgs() {
        val command = Bbl()
        val result = command.test()
        assertEquals("$webusGenesisChapterOne\n", result.stdout)
    }

    @Test
    fun testBblGen1() {
        val command = Bbl()
        val result = command.test("gen 1")
        assertEquals("$webusGenesisChapterOne\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16() {
        val command = Bbl()
        val result = command.test("john 3:16")
        val webusJohn3v16 = "16 For God so loved the world, that he gave his only born  Son, that whoever believes in him should not perish, but have eternal life. "
        assertEquals("$webusJohn3v16\n", result.stdout)
    }

    @Test
    fun testBblMatt28v18to20() {
        val command = Bbl()
        val result = command.test("matt 28:18-20")
        val webusMatt28v18to20 = """
            18 Jesus came to them and spoke to them, saying, “All authority has been given to me in heaven and on earth. 
            19 Go  and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit, 
            20 teaching them to observe all things that I commanded you. Behold, I am with you always, even to the end of the age.” Amen.
        """.trimIndent()
        assertEquals("$webusMatt28v18to20\n\n", result.stdout)
    }

    @Test
    fun testBblInJc() {
        val command = Bbl()
        val result = command.test("in jc")
        assertEquals("$jcGenesisChapterOne\n", result.stdout)
    }

    @Test
    fun testBblGen1InJc() {
        val command = Bbl()
        val result = command.test("gen 1 in jc")
        assertEquals("$jcGenesisChapterOne\n", result.stdout)
    }

    @Test
    fun testBblJohn3v16InJc() {
        val command = Bbl()
        val result = command.test("john 3:16 in jc")
        val jcJohn3v16 = "16 神はそのひとり子を賜わったほどに、この世を愛して下さった。それは御子を信じる者がひとりも滅びないで、永遠の命を得るためである。"
        assertEquals("$jcJohn3v16\n", result.stdout)
    }

    @Test
    fun testBblMatt28v18to20InJc() {
        val command = Bbl()
        val result = command.test("matt 28:18-20 in jc")
        val jcMatt28v18to20 = """
            18 イエスは彼らに近づいてきて言われた、「わたしは、天においても地においても、いっさいの権威を授けられた。
            19 それゆえに、あなたがたは行って、すべての国民を弟子として、父と子と聖霊との名によって、彼らにバプテスマを施し、
            20 あなたがたに命じておいたいっさいのことを守るように教えよ。見よ、わたしは世の終りまで、いつもあなたがたと共にいるのである」。
        """.trimIndent()
        assertEquals("$jcMatt28v18to20\n\n", result.stdout)
    }
}
