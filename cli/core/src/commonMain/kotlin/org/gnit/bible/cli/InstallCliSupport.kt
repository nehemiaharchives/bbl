package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktError
import org.gnit.bible.Bible
import org.gnit.bible.BblVersion
import org.gnit.bible.ZipBibleResourcesReader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object InstallCliSupport {
    private fun installedPackManifestJsonOrNull(bible: Bible, translationCode: String): String? {
        val am = bible.assetManager
        return runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrNull()
    }

    private fun installedPackVersion(manifestJson: String): String {
        val jsonElement = Json.parseToJsonElement(manifestJson)
        return jsonElement.jsonObject["version"]?.jsonPrimitive?.content
            ?: error("Pack manifest is missing version")
    }

    fun isInstalledPackCompatible(bible: Bible, translationCode: String): Boolean {
        val manifestJson = installedPackManifestJsonOrNull(bible, translationCode) ?: return false
        return installedPackVersion(manifestJson) == BblVersion.VERSION
    }

    fun validateInstalledPackVersionOrThrow(bible: Bible, translationCode: String) {
        val am = bible.assetManager
        val manifestJson = runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrElse { error ->
            runCatching { am.delete(translationCode) }
            throw CliktError("Installing $translationCode failed: unable to read pack manifest: ${error.message}")
        }

        val packVersion = installedPackVersion(manifestJson)
        if (packVersion != BblVersion.VERSION) {
            runCatching { am.delete(translationCode) }
            throw CliktError(
                "Installing $translationCode failed: pack manifest version $packVersion is incompatible with bbl ${BblVersion.VERSION}"
            )
        }
    }
}
