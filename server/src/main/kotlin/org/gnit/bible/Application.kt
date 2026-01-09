package org.gnit.bible

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    val bible = Bible().apply { bibleResourcesReader = ServerBibleResourcesReader() }

    routing {
        get("/") {
            call.respondText("Genesis 1: ${bible.verses()}")
        }

        get("/list") {
            val list = mutableListOf<Translation>()
            downloadableTranslationCodeListCli.forEach{ translationCode ->
                val manifestJson =
                    getResourceAsText("/files/bbltexts/$translationCode/$translationCode$MANIFEST_JSON_POSTFIX")!!
                val manifest = Translation.fromJson(manifestJson)
                list.add(manifest)
            }

            val json = list.toJson()
            call.respondText(json)
        }
    }
}

fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
