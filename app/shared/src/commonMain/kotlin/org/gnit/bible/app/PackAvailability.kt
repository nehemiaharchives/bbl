package org.gnit.bible.app

sealed interface PackAvailability {
    data object BuiltIn : PackAvailability
    data object Installed : PackAvailability
    data object Downloadable : PackAvailability
}
