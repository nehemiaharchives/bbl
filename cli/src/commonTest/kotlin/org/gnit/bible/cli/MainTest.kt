package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
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
    fun testBblJohn3() {
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
}
