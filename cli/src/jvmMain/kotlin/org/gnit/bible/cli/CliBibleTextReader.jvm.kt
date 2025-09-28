package org.gnit.bible.cli

import org.gnit.bible.BibleTextReader

actual class CliBibleTextReader : BibleTextReader {

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
}