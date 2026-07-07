package org.gnit.bible.app

import io.github.oshai.kotlinlogging.KotlinLogging

val logger = KotlinLogging.logger {}

const val BUTTON_SIZE = 30
const val SPACE_BETWEEN_BUTTON_WITH_SLIDER = 1
const val BUTTON_ROUND = 5
const val BUTTON_TEXT_FONT_SIZE = 15
const val BUTTON_CONTENT_PADDING = 0

/**
 * This value is either 0 or positive integer. When the value is 0, the title and
 * book control stick together. When the value is positive, the title goes down
 * vertically and overwraps with book control by that integer value in dp.
 */
const val TITLE_BOOK_CONTROL_VERTICAL_OVERWRAP_DELTA: Int = 0
