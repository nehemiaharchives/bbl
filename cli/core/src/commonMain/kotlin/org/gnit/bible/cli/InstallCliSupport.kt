package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CliktError
import org.gnit.bible.Bible
import org.gnit.bible.ZipBibleResourcesReader
import org.gnit.bible.BblVersion

object InstallCliSupport {
    fun installedPackCompatibilityVersionOrNull(bible: Bible, translationCode: String): String? {
        val am = bible.assetManager
        val manifestJson = runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrNull() ?: return null
        return BblVersion.packManifestArtifactCompatibilityVersionOrNull(manifestJson)
    }

    fun isInstalledPackCompatible(bible: Bible, translationCode: String): Boolean {
        return installedPackCompatibilityVersionOrNull(bible, translationCode) == BblVersion.artifactCompatibilityVersion
    }

    fun validateInstalledPackVersionOrThrow(bible: Bible, translationCode: String) {
        val am = bible.assetManager
        val manifestJson = runCatching {
            ZipBibleResourcesReader(platform = am.platform, fileSystem = am.fileSystem).getManifestJson(translationCode)
        }.getOrElse { error ->
            runCatching { am.delete(translationCode) }
            throw CliktError("Installing $translationCode failed: unable to read pack manifest: ${error.message}")
        }

        val packVersion = BblVersion.packManifestArtifactCompatibilityVersionOrNull(manifestJson)
        if (packVersion != BblVersion.artifactCompatibilityVersion) {
            runCatching { am.delete(translationCode) }
            val actual = packVersion ?: "<missing>"
            throw CliktError(
                "Installing $translationCode failed: pack manifest bblArtifactCompatibilityVersion $actual is incompatible with bbl ${BblVersion.artifactCompatibilityVersion}"
            )
        }
    }
}
