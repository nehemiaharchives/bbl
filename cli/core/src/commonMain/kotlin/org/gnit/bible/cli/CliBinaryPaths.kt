package org.gnit.bible.cli

import org.gnit.bible.SearchModuleId
import okio.Path
import okio.Path.Companion.toPath

object CliBinaryPaths {
    fun binDir(packDir: String): Path {
        val packPath = packDir.toPath()
        return (packPath.parent ?: error("Unable to resolve bbl dir from pack dir: $packDir")) / "bin"
    }

    fun binaryName(moduleId: SearchModuleId, platformName: String): String {
        val executableSuffix = if (platformName == "Windows") ".exe" else ""
        return "bbl-search-${moduleId.name.lowercase()}$executableSuffix"
    }
}
