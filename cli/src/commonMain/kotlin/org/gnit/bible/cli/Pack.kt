package org.gnit.bible.cli

import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okio.SYSTEM
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.downloadableTranslationCodeList

class Packer {

    val fileSystem = okio.FileSystem.SYSTEM

    private val logger = KotlinLogging.logger {}

    fun createBblPack(inputPathString: String, outputPathString: String = "bblpack") {
        val currentDir = currentDir()
        logger.debug { "currentDir: $currentDir" }

        // validate input path
        val inputPath = currentDir.resolve(inputPathString, true)
        if (fileSystem.exists(inputPath) && fileSystem.metadata(inputPath).isDirectory) {
            logger.info { "Input path $inputPath exists and is dir" }
        } else {
            if (!fileSystem.exists(inputPath)) {
                logger.error { "Input path $inputPath does not exits" }; return
            }
            if (!fileSystem.metadata(inputPath).isDirectory) {
                logger.error { "Input path $inputPath is not a directory" }; return
            }
        }

        // validate output path
        val outputPath = currentDir.resolve(outputPathString, true)
        if (fileSystem.exists(outputPath) && fileSystem.metadata(outputPath).isDirectory) {
            logger.info { "Output path $outputPath exists and is dir" }
        } else {
            if (!fileSystem.exists(outputPath)) {
                logger.error { "Input path $outputPath does not exits" }; return
            }
            if (!fileSystem.metadata(outputPath).isDirectory) {
                logger.error { "Input path $outputPath is not a directory" }; return
            }
        }

        // get translation code
        val translationCode = inputPath.name
        logger.info { "translationCode: $translationCode" }

        // validate manifest json
        val manifestPath = inputPath.resolve("$translationCode$MANIFEST_JSON_POSTFIX")
        if (fileSystem.exists(manifestPath) && fileSystem.metadata(manifestPath).isRegularFile) {
            logger.info { "Manifest path $manifestPath exists and is file" }
        } else {
            if (!fileSystem.exists(manifestPath)) {
                logger.error { "Manifest path $manifestPath does not exits" }; return
            }
            if (!fileSystem.metadata(manifestPath).isRegularFile) {
                logger.error { "Manifest path $manifestPath is not a file" }; return
            }
        }

        val translation: Translation
        try {
            translation = Translation.fromJson(fileSystem.read(manifestPath) { readUtf8() })
        } catch (e: Throwable) {
            logger.error { "error while reading/parsing $manifestPath: ${e.message}" }; return
        }

        // TODO create lucene-kmp index later

        // zip everything into ${translationCode}.zip
        val dir = File(outputPath.toString())
        if (dir.exists && dir.isDirectory) {
            logger.info { "Output directory ${dir.name} exists and is directory" }
        }else{
            if (!dir.exists) {
                logger.error { "Output directory ${dir.name} does not exits" }; return
            }
            if (!dir.isDirectory) {
                logger.error { "Output directory ${dir.name} is not a directory" }; return
            }
        }
        val sourceDirectory = File(inputPath.toString())
        if (sourceDirectory.exists && sourceDirectory.isDirectory) {
            logger.info { "Source directory ${sourceDirectory.name} exists and is directory" }
        } else {
            if (!sourceDirectory.exists) {
                logger.error { "Source directory ${sourceDirectory.name} does not exits" }; return
            }
            if (!sourceDirectory.isDirectory) {
                logger.error { "Source directory ${sourceDirectory.name} is not a directory" }; return
            }
        }

        val zip = File(dir, "${translation.code}.zip")
        if (zip.exists) {
            logger.info { "zip file ${zip.name} exists" }
        } else {
            logger.info { "Zip file ${zip.name} does not exist as expected, creating new one" }
        }

        val manifestFile = File(manifestPath.toString())
        if(manifestFile.exists && !manifestFile.isDirectory) {
            logger.info { "Manifest file ${manifestFile.name} exists and is not dir (so maybe is file as expected)" }
        } else {
            if (!manifestFile.exists) {
                logger.error { "Manifest file ${manifestFile.name} does not exits" }; return
            }
            if (!manifestFile.isDirectory) {
                logger.error { "Manifest file ${manifestFile.name} is dir and so is not a file" }; return
            }
        }

        runBlocking {
            ZipFile(zip, FileMode.Write).use {
                it.zipFile(manifestFile)
                it.zipDirectory(sourceDirectory, shallow = true) {
                    name -> name.endsWith(".txt")
                }
            }
        }

    }
}

fun packTranslation(translationCode: String){
    Packer().createBblPack(
        inputPathString = "../server/src/main/resources/files/bbltexts/$translationCode",
        outputPathString = "../server/src/main/resources/files/bblpacks/"
    )
}

fun main() {
    downloadableTranslationCodeList.forEach { translationCode ->
        packTranslation(translationCode)
    }
}
