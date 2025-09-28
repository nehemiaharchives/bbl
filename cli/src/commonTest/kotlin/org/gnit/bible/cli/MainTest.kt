package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertContains

class MainTest {
    @Test
    fun testBblWithNoArgs() {
        val command = Bbl()
        val result = command.test()
        assertContains(result.output, "1 In the beginning, God created the heavens and the earth.")
    }
}