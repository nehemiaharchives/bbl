package org.gnit.bible

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BblVersion {
    private val manifestJson = Json { ignoreUnknownKeys = true }
    /**
     * The App version number. This string will be used as git tag for the release, note no "v" prefix, just the version number.
     */
    val version = "4.0.0"

    const val downloadRepository = "nehemiaharchives/bbl"
    const val legacyDownloadRepository = "nehemiaharchives/bbl-kmp"
    private const val serverResourcesPath = "bbl/resources"

    val releaseDownloadBaseUrl = "https://github.com/$downloadRepository/releases/download/$version"
    val serverResourcesBaseUrl = "https://raw.githubusercontent.com/$downloadRepository/$version/$serverResourcesPath"

    fun searchHelperVersionLine(binaryName: String): String = "$binaryName version $version"

    fun serverResourceUrl(relativePath: String): String = "$serverResourcesBaseUrl/$relativePath"

    fun serverResourcePath(repository: String, compatibilityVersion: String, relativePath: String): String {
        return "/$repository/$compatibilityVersion/$serverResourcesPath/$relativePath"
    }

    fun rawGithubUrl(repository: String, ref: String, relativePath: String): String {
        return "https://raw.githubusercontent.com/$repository/$ref/$relativePath"
    }

    fun releaseAssetPath(repository: String, compatibilityVersion: String, assetName: String): String {
        return "/$repository/releases/download/$compatibilityVersion/$assetName"
    }

    fun downloadUrlCandidates(url: String): List<String> {
        val fallbackUrl = url.replace("/$downloadRepository/", "/$legacyDownloadRepository/")
        return if (fallbackUrl == url) listOf(url) else listOf(url, fallbackUrl)
    }

    fun packManifestVersionOrNull(manifestJson: String): String? {
        return runCatching {
            val jsonElement = Json.parseToJsonElement(manifestJson)
            jsonElement.jsonObject["version"]?.jsonPrimitive?.contentOrNull
                ?: jsonElement.jsonObject["bblArtifactCompatibilityVersion"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}
