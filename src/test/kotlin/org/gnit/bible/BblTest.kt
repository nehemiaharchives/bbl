package org.gnit.bible

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private val jsonProcessor = Json {
    encodeDefaults = true
    prettyPrint = true
}

class BblTest {
    @Test
    fun `parse config string`() {
        val configJson = """
            {
                "defaultVersion": "jc",
                "logLevel": "ERROR"
            }
        """.trimIndent()

        val expected = Config(defaultVersion = "jc")

        assertEquals(expected, jsonProcessor.decodeFromString(configJson))
    }

    @Test
    fun `write default config`() {
        val expected = """
            {
                "defaultVersion": "kjv",
                "logLevel": "ERROR"
            }
        """.trimIndent()

        assertEquals(expected, jsonProcessor.encodeToString(Config()))
    }
}