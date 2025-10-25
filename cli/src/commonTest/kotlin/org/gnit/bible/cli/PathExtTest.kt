package org.gnit.bible.cli

import kotlin.test.Test

class PathExtTest {

    @Test
    fun testCurrentDir() {
        val dir = currentDir()
        println("Current directory: $dir")
    }
}
