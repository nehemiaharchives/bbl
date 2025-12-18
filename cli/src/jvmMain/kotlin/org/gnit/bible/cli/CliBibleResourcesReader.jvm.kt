package org.gnit.bible.cli

import org.gnit.bible.BibleResourcesReader
import org.gnit.bible.SearchEngine

actual class CliBibleResourcesReader : BibleResourcesReader {

    /**
     * Corresponds to the following in `cli/build.gradle.kts`:
     * ```
     *         jvmMain.get().resources.srcDir(
     *             rootProject.layout.projectDirectory
     *                 .dir("composeApp/src/commonMain/composeResources").asFile
     *         )
     * ```
     */
    actual override fun chapterFile(translation: String, book: Int, chapter: Int): String {
        return "/files/$base/$translation/$translation.$book.$chapter.txt"
    }

    actual override fun readByPath(path: String): String {
        val stream = object {}.javaClass.getResourceAsStream(path)
        val text = stream!!.bufferedReader().use { it.readText() }
        return text
    }

    actual override fun listIndexFiles(translation: String): List<String> {
        val translationIndexManifestFile =
            "/files/$base/$translation/index/$translation${SearchEngine.INDEX_MANIFEST_FILENAME_POSTFIX}"
        val indexManifestFiles = object {}.javaClass.getResource(translationIndexManifestFile)!!.readText().lines().filter { it.isNotEmpty() }
        return indexManifestFiles
    }

    actual override fun readIndexFile(
        translation: String,
        name: String
    ): ByteArray {
        TODO("Not yet implemented")
    }
}