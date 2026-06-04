package org.gnit.bible

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

object LoggingSetup {
    fun suppressKotlinLoggingStartupMessage() {
        KotlinLoggingConfiguration.logStartupMessage = false
    }
}
