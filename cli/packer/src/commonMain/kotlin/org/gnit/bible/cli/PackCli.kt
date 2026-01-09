package org.gnit.bible.cli

// Extracted from :cli to :cli:packer as part of Phase 6.
// This tool is developer-only: it creates bbl pack zip files (with lucene-kmp indexes) for publishing.

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.downloadableTranslationCodeList

class PackCli(
    private val bible: Bible
) : CliktCommand(name = "pack") {

    private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Pack a directory named with translation code into a zip file of bblpack file which can be imported for bbl"
    }

    private val target by argument(help = "translation code of new bblpack dir/zip to be created, e.g. webus, jc")

    override fun run() {
        val normalizedTarget = target.trim().lowercase()
        if (normalizedTarget.isEmpty()) {
            throw CliktError("Missing translation code")
        }

        val inputPathString = when {
            normalizedTarget.contains("/") || normalizedTarget.contains("\\") -> normalizedTarget
            else -> "../server/src/main/resources/files/bbltexts/$normalizedTarget"
        }

        val outputPathString = "../server/src/main/resources/files/bblpacks"
        createBblPack(inputPathString = inputPathString, outputPathString = outputPathString)
    }

    private val fileSystem = bible.assetManager.fileSystem

    fun createBblPack(inputPathString: String, outputPathString: String = "bblpack") {
        val currentDir = currentDir()
        logger.debug { "currentDir: $currentDir" }

        // validate input path
        val inputPath = currentDir.resolve(inputPathString, true)
        if (!(fileSystem.exists(inputPath) && fileSystem.metadata(inputPath).isDirectory)) {
            val msg = when {
                !fileSystem.exists(inputPath) -> "Input path $inputPath does not exist"
                else -> "Input path $inputPath is not a directory"
            }
            throw IllegalStateException(msg)
        }

        // validate output path
        val outputPath = currentDir.resolve(outputPathString, true)
        if (!(fileSystem.exists(outputPath) && fileSystem.metadata(outputPath).isDirectory)) {
            val msg = when {
                !fileSystem.exists(outputPath) -> "Output path $outputPath does not exist"
                else -> "Output path $outputPath is not a directory"
            }
            throw IllegalStateException(msg)
        }

        // get translation code
        val translationCode = inputPath.name
        logger.info { "translationCode: $translationCode" }

        // validate manifest json
        // Convention: manifest file name is '<translationCode>.0.manifest.json' (MANIFEST_JSON_POSTFIX)
        val manifestPath = inputPath.resolve("$translationCode$MANIFEST_JSON_POSTFIX")
        if (!(fileSystem.exists(manifestPath) && fileSystem.metadata(manifestPath).isRegularFile)) {
            val msg = when {
                !fileSystem.exists(manifestPath) ->
                    "Manifest path $manifestPath does not exist. Expected <translationCode>$MANIFEST_JSON_POSTFIX in $inputPath"
                else -> "Manifest path $manifestPath is not a file"
            }
            throw IllegalStateException(msg)
        }

        val rawManifest = runCatching { fileSystem.read(manifestPath) { readUtf8() } }
            .getOrElse { e -> throw IllegalStateException("Failed to read manifest $manifestPath: ${e.message}", e) }

        val translation: Translation = try {
            Translation.fromJson(rawManifest)
        } catch (e: Throwable) {
            throw IllegalStateException("Error while parsing manifest $manifestPath: ${e.message}", e)
        }

        val indexBuilder = IndexBuilder(bible)
        runCatching {
            indexBuilder.createLuceneKmpIndex(translation = translation, translationDir = inputPath)
        }.onFailure { e ->
            throw IllegalStateException(
                "Failed to create lucene-kmp index for ${translation.code} at $inputPath: ${e.message}",
                e
            )
        }

        // zip everything into ${translationCode}.zip
        val dir = File(outputPath.toString())
        if (!(dir.exists && dir.isDirectory)) {
            val msg = when {
                !dir.exists -> "Output directory ${dir.name} does not exist"
                else -> "Output directory ${dir.name} is not a directory"
            }
            throw IllegalStateException(msg)
        }
        val sourceDirectory = File(inputPath.toString())
        if (!(sourceDirectory.exists && sourceDirectory.isDirectory)) {
            val msg = when {
                !sourceDirectory.exists -> "Source directory ${sourceDirectory.name} does not exist"
                else -> "Source directory ${sourceDirectory.name} is not a directory"
            }
            throw IllegalStateException(msg)
        }

        val zip = File(dir, "${translation.code}.zip")
        if (zip.exists) {
            logger.info { "zip file ${zip.name} exists; deleting to recreate (ZipFile does not support overwriting entries)" }
            val deleted = runCatching { runBlocking { zip.delete() } }
                .onFailure { e -> logger.error { "failed to delete existing zip file ${zip.name}: ${e.message}" } }
                .getOrElse { false }
            if (!deleted || zip.exists) {
                throw IllegalStateException("Failed to delete existing zip file ${zip.name}")
            }
        }

        val manifestFile = File(manifestPath.toString())
        if (!(manifestFile.exists && !manifestFile.isDirectory)) {
            val msg = when {
                !manifestFile.exists -> "Manifest file ${manifestFile.name} does not exist"
                else -> "Manifest file ${manifestFile.name} is dir and so is not a file"
            }
            throw IllegalStateException(msg)
        }

        runBlocking {
            ZipFile(zip, FileMode.Write).use {
                it.zipFile(manifestFile)
                it.zipDirectory(sourceDirectory, shallow = false) { name ->
                    val normalized = name.replace('\\', '/')
                    normalized.endsWith(".txt") ||
                        (normalized.startsWith("index/") && !normalized.endsWith("write.lock"))
                }
            }
        }
    }
}

fun packTranslation(translationCode: String) {
    PackCli(Bible()).createBblPack(
        inputPathString = "../../server/src/main/resources/files/bbltexts/$translationCode",
        outputPathString = "../../server/src/main/resources/files/bblpacks/"
    )
}

fun mainAll() {
    println("bbl-packer: use PackCli (developer tool)")
    val currentDir = currentDir()
    println("currentDir: $currentDir")

    val failures = mutableListOf<Pair<String, String>>()

    downloadableTranslationCodeList.forEach { translationCode ->
        runCatching {
            packTranslation(translationCode)
        }.onFailure { e ->
            val msg = e.message ?: e.toString()
            failures += translationCode to msg
            // Keep this kmp-friendly (commonMain): no java.lang.System.
            println("FAILED to pack $translationCode: $msg")
        }
    }

    if (failures.isNotEmpty()) {
        println("\nSummary: ${failures.size} translation(s) failed to pack:")
        failures.forEach { (code, msg) ->
            println(" - $code: $msg")
        }
        // Make CI / automation fail.
        throw IllegalStateException("Failed to pack ${failures.size} translations")
    }
}

fun main(){
    packTranslation("krv")
}
