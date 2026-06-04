package org.gnit.bible.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.gnit.bible.Bible
import org.gnit.bible.SERVER_PORT
import org.gnit.bible.Translation
import org.gnit.bible.toJson

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    val bible = Bible().apply { bibleResourcesReader = ServerBibleResourcesReader() }
    val downloadableTranslations = Translation.downloadableTranslationsCmp

    routing {
        get("/") {
            call.respondText("Genesis 1: ${bible.verses()}")
        }

        get("/list") {
            call.respondText(downloadableTranslations.toJson())
        }
    }
}
