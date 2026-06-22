package org.gnit.bible

object BblVersion {
    /**
     * The App version number. This string is used as the git tag for the release.
     */
    const val VERSION = "v2.0"

    const val BBL_REPOSITORY = "nehemiaharchives/bbl"

    //TODO remove this after successful repository migration from bbl-kmp to bbl
    const val BBL_REPOSITORY_LEGACY = "nehemiaharchives/bbl-kmp"

    const val RELEASE_DOWNLOAD_URL = "https://github.com/$BBL_REPOSITORY/releases/download/$VERSION"

    const val SERVER_RESOURCE_PATH = "/$BBL_REPOSITORY/$VERSION/resources"

    //TODO remove this after successful repository migration from bbl-kmp to bbl
    const val SERVER_RESOURCE_PATH_LEGACY = "/$BBL_REPOSITORY_LEGACY/$VERSION/resources"
}
