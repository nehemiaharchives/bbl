package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

fun suppressKotlinLoggingStartupMessage() {
    KotlinLoggingConfiguration.logStartupMessage = false
}
