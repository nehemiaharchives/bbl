package org.gnit.bible.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * This string will be used as git tag for the release, note no "v" prefix, just the version number.
 * This version constrains
 *
 * 1. bbl pack manifest
 * 2. bbl search binary
 * 3. bbl cli itself
 *
 * The constraints are also used by the default pack and search-helper download URLs.
 */
const val bblCliVersion = "4.0.0"

const val bblReleaseDownloadBaseUrl = "https://github.com/nehemiaharchives/bbl-kmp/releases/download/$bblCliVersion"

fun bblSearchHelperVersionLine(binaryName: String): String = "$binaryName version $bblCliVersion"

fun packManifestBblVersionOrNull(manifestJson: String): String? {
    return runCatching {
        Json.parseToJsonElement(manifestJson).jsonObject["bblVersion"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
