package org.gnit.bible.cli

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.gnit.bible.Bible
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.Translation
import org.gnit.bible.BblVersion

class PackCli(
    private val bible: Bible
) : CoreCliktCommand(name = "pack") {

    private val logger = KotlinLogging.logger {}

    override fun help(context: Context): String {
        return "Pack a translation directory into a bblpack zip. By convention, run from the project root and use ./resources/bbltexts and ./resources/bblpacks; override with --source and --packs when needed."
    }

    private val target by argument(help = "translation code of new bblpack dir/zip to be created, e.g. webus, jc").optional()

    private val all by option("--all", help = "pack every downloadable translation").flag()
    private val sourceDir by option("--source", help = "source bbltexts directory").default("resources/bbltexts")
    private val packsDir by option("--packs", help = "output bblpacks directory").default("resources/bblpacks")

    override fun run() {
        if (all) {
            mainAll(sourceDir, packsDir)
            return
        }

        val normalizedTarget = target?.trim()?.lowercase().orEmpty()
        if (normalizedTarget.isEmpty()) {
            throw CliktError("Missing translation code")
        }

        createBblPack(
            inputPathString = "$sourceDir/$normalizedTarget",
            outputPathString = packsDir
        )
    }

    private val fileSystem = bible.assetManager.fileSystem

    fun createBblPack(
        inputPathString: String,
        outputPathString: String = "bblpack",
        updateIndexOnly: Boolean = false
    ) {
        val currentDir = currentDir()
        logger.debug { "currentDir: $currentDir" }

        val inputPath = currentDir.resolve(inputPathString, true)
        if (!(fileSystem.exists(inputPath) && fileSystem.metadata(inputPath).isDirectory)) {
            val msg = when {
                !fileSystem.exists(inputPath) -> "Input path $inputPath does not exist"
                else -> "Input path $inputPath is not a directory"
            }
            throw IllegalStateException(msg)
        }

        val outputPath = currentDir.resolve(outputPathString, true)
        if (!(fileSystem.exists(outputPath) && fileSystem.metadata(outputPath).isDirectory)) {
            val msg = when {
                !fileSystem.exists(outputPath) -> "Output path $outputPath does not exist"
                else -> "Output path $outputPath is not a directory"
            }
            throw IllegalStateException(msg)
        }

        val translationCode = inputPath.name
        logger.info { "translationCode: $translationCode" }

        val manifestPath = inputPath.resolve("$translationCode$MANIFEST_JSON_POSTFIX")
        if (!fileSystem.exists(manifestPath)) {
            val translationEntry = Translation.downloadableTranslationsCmp
                .firstOrNull { it.code == translationCode }
                ?: Translation.embeddedTranslations.firstOrNull { it.code == translationCode }

            if (translationEntry == null) {
                throw IllegalStateException(
                    "Manifest path $manifestPath does not exist, and no Translation entry found for $translationCode"
                )
            }

            val json = translationEntry.toJson()
            fileSystem.write(manifestPath) { writeUtf8(json) }
        }

        if (!fileSystem.metadata(manifestPath).isRegularFile) {
            throw IllegalStateException("Manifest path $manifestPath is not a file")
        }

        val rawManifest = runCatching { fileSystem.read(manifestPath) { readUtf8() } }
            .getOrElse { e -> throw IllegalStateException("Failed to read manifest $manifestPath: ${e.message}", e) }

        val translation: Translation = try {
            Translation.fromJson(rawManifest)
        } catch (e: Throwable) {
            throw IllegalStateException("Error while parsing manifest $manifestPath: ${e.message}", e)
        }.copy(bblArtifactCompatibilityVersion = BblVersion.artifactCompatibilityVersion)

        fileSystem.write(manifestPath) { writeUtf8(translation.toJson()) }

        val indexBuilder = IndexBuilder(bible)
        runCatching {
            indexBuilder.createLuceneKmpIndex(translation = translation, translationDir = inputPath)
        }.onFailure { e ->
            throw IllegalStateException(
                "Failed to create lucene-kmp index for ${translation.code} at $inputPath: ${e.message}",
                e
            )
        }

        if (updateIndexOnly) {
            return
        }

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

fun packTranslation(translationCode: String, sourceDir: String = "resources/bbltexts", packsDir: String = "resources/bblpacks") {
    PackCli(Bible()).createBblPack(
        inputPathString = "$sourceDir/$translationCode",
        outputPathString = packsDir
    )
}

fun mainAll(sourceDir: String = "resources/bbltexts", packsDir: String = "resources/bblpacks") {
    println("bbl-packer: use PackCli (developer tool)")
    val currentDir = currentDir()
    println("currentDir: $currentDir")

    val failures = mutableListOf<Pair<String, String>>()

    Translation.downloadableTranslationsCmp
        .map { it.code }
        .distinct()
        .forEach { translationCode ->
        runCatching {
            packTranslation(translationCode, sourceDir, packsDir)
        }.onFailure { e ->
            val msg = e.message ?: e.toString()
            failures += translationCode to msg
            println("FAILED to pack $translationCode: $msg")
        }
    }

    if (failures.isNotEmpty()) {
        println("\nSummary: ${failures.size} translation(s) failed to pack:")
        failures.forEach { (code, msg) ->
            println(" - $code: $msg")
        }
        throw IllegalStateException("Failed to pack ${failures.size} translations")
    }
}

fun main(args: Array<String>) {
    PackCli(Bible()).main(platformCommandLineArgs(args))
}
