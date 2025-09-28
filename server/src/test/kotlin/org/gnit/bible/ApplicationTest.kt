package org.gnit.bible

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.gnit.bible.cli.CliBibleTextReader
import kotlin.test.*

class ApplicationTest {

    val bible = Bible().apply { bibleTextReader = CliBibleTextReader() }

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Genesis 1: ${bible.verses()}", response.bodyAsText())
    }
}