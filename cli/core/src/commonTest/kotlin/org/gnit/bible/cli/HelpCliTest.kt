package org.gnit.bible.cli

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HelpCliTest {

    @Test
    fun `bbl help prints main help`() {
        val result = Bbl().test("help")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "Read, search Holy Bible")
    }

    @Test
    fun `bbl help search prints search help`() {
        val result = Bbl().test("help search")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "Search Bible text")
    }

    @Test
    fun `bbl help rand prints rand help`() {
        val result = Bbl().test("help rand")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "random verse")
    }

    @Test
    fun `bbl help list prints list help`() {
        val result = Bbl().test("help list")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "list")
    }

    @Test
    fun `bbl help install prints install help`() {
        val result = Bbl().test("help install")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "install")
    }

    @Test
    fun `bbl help uninstall prints uninstall help`() {
        val result = Bbl().test("help uninstall")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "uninstall")
    }

    @Test
    fun `bbl help config prints config help`() {
        val result = Bbl().test("help config")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "settings")
    }

    @Test
    fun `bbl help history prints history help`() {
        val result = Bbl().test("help history")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "bbl command history")
    }

    @Test
    fun `bbl help unknown returns error`() {
        val result = Bbl().test("help unknowncommand")
        assertNotEquals(0, result.statusCode)
    }
}
