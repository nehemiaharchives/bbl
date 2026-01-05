package org.gnit.bible

import kotlinx.serialization.Serializable

@Serializable
enum class SearchModuleId {
    COMMON,
    MORFOLOGIK,
    SMARTCN,
    NORI,
    KUROMOJI,
    EXTRA,
}
