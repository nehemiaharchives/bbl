package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import org.gnit.bible.webusGenesisChapterOne
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun testBblWithNoArgs() {
        val command = Bbl()
        val result = command.test()
        assertEquals("$webusGenesisChapterOne\n", result.stdout)
    }
}