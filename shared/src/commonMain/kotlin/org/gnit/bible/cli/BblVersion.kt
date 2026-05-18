package org.gnit.bible.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The App version number. This string will be used as git tag for the release, note no "v" prefix, just the version number.
 */
const val bblCliVersion = "4.0.0"

/**
 * The lucene-kmp generated index version number. This string will be used to determine compatibility between the bbl search helper and the generated index files.
 * This version constrains
 *
 * 1. bbl pack manifest
 * 2. bbl search binary
 * 3. bbl cli itself
 *
 * The constraints are also used by the default pack and search-helper download URLs.
 */
const val bblArtifactCompatibilityVersion = "1.0"

const val bblReleaseDownloadBaseUrl = "https://github.com/nehemiaharchives/bbl-kmp/releases/download/$bblArtifactCompatibilityVersion"

fun bblSearchHelperVersionLine(binaryName: String): String = "$binaryName version $bblCliVersion"

fun bblSearchHelperArtifactCompatibilityVersionLine(): String = bblArtifactCompatibilityVersion

fun packManifestArtifactCompatibilityVersionOrNull(manifestJson: String): String? {
    return runCatching {
        Json.parseToJsonElement(manifestJson).jsonObject["bblArtifactCompatibilityVersion"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
