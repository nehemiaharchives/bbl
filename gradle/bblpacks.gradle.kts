import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Properties

// This script plugin is applied to the 'cli' project.
// It generates:
// - build/embedded/tar/*.tar
// - build/embedded/c/*.c
// - build/embedded/obj/*.o
// - build/embedded/libbibles.a
// - build/embedded/include/generated_bibles.h
// - build/generated/cli/org/gnit/bible/cli/GeneratedTarBindings.kt

val resourcesRoot = rootProject.layout.projectDirectory.dir("composeApp/src/commonMain/composeResources/files")
val bblpacksDir = resourcesRoot.dir("bblpacks")
val embedBuild = layout.buildDirectory.dir("embedded")
val cOutDir = embedBuild.map { it.dir("c") }
val oOutDir = embedBuild.map { it.dir("obj") }
val tarOutDir = embedBuild.map { it.dir("tar") }
val libOutFile = embedBuild.map { it.file("libbibles.a") }
val includeOutDir = embedBuild.map { it.dir("include") }
val generatedKtDir = layout.buildDirectory.dir("generated/cli")

private val kotlinVersionProvider = providers.provider {
    val versionsFile = rootProject.layout.projectDirectory.file("gradle/libs.versions.toml").asFile
    val text = versionsFile.readText()
    val regex = Regex("""^\s*kotlin\s*=\s*"(.*?)"\s*$""", RegexOption.MULTILINE)
    val match = regex.find(text) ?: throw GradleException("Could not find kotlin version in ${versionsFile.absolutePath}")
    match.groupValues[1]
}

private fun hostKonanKey(): String {
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.isMacOsX && (arch.contains("aarch") || arch.contains("arm")) -> "macos_arm64"
        os.isMacOsX -> "macos_x64"
        os.isLinux -> "linux_x64"
        os.isWindows -> "mingw_x64"
        else -> throw GradleException("Unsupported host '$os' for Kotlin/Native LLVM resolution.")
    }
}

private fun konanDataDir(): File =
    System.getenv("KONAN_DATA_DIR")?.let(::File) ?: File(System.getProperty("user.home"), ".konan")

private data class KotlinNativeRemote(val repo: String, val remotePath: String)

private data class KotlinNativeLlvmBundle(
    val dependencyName: String,
    val archiveUrl: String,
    val archiveExt: String,
    val llvmMajorVersion: Int
)

private fun loadKotlinNativeGradleProperties(kotlinVersion: String): Properties {
    val cacheDir = layout.buildDirectory.dir("kotlinNative/$kotlinVersion").get().asFile.also { it.mkdirs() }
    val gradlePropsFile = cacheDir.resolve("gradle.properties")

    if (!gradlePropsFile.exists()) {
        if (gradle.startParameter.isOffline) {
            throw GradleException("Offline and missing Kotlin/Native gradle.properties for Kotlin $kotlinVersion")
        }
        val remoteUrl = "https://raw.githubusercontent.com/JetBrains/kotlin/refs/tags/v${kotlinVersion}/kotlin-native/gradle.properties"
        logger.lifecycle("Fetching Kotlin/Native gradle.properties from $remoteUrl")
        curlDownload(remoteUrl, gradlePropsFile, retries = 3)
        if (!gradlePropsFile.exists()) {
            throw GradleException("Failed to download Kotlin/Native gradle.properties for Kotlin $kotlinVersion")
        }
    }

    return Properties().apply {
        gradlePropsFile.inputStream().use { load(it) }
    }
}

private fun parseRemote(spec: String): KotlinNativeRemote? {
    val trimmed = spec.trim()
    if (!trimmed.startsWith("remote:")) return null
    val parts = trimmed.split(':', limit = 3)
    if (parts.size != 3) return null
    val repo = parts[1]
    val path = parts[2]
    if (repo.isBlank() || path.isBlank()) return null
    return KotlinNativeRemote(repo, path)
}

private fun kotlinNativeLlvmBundleFor(kotlinVersion: String): KotlinNativeLlvmBundle {
    val props = loadKotlinNativeGradleProperties(kotlinVersion)
    val hostKey = hostKonanKey()
    val profile = props.getProperty("kotlin.native.llvm")?.trim()?.takeIf { it.isNotEmpty() } ?: "default"

    val versionKey = "kotlin.native.llvm.$profile.$hostKey.version"
    val versionString = props.getProperty(versionKey)?.trim()
        ?: throw GradleException("Missing '$versionKey' in Kotlin/Native gradle.properties for Kotlin $kotlinVersion")
    val llvmMajor = versionString.toIntOrNull()
        ?: throw GradleException("Invalid LLVM version '$versionString' at $versionKey")

    val essentialsKey = "kotlin.native.llvm.$profile.$hostKey.essentials"
    val essentialSpec = props.getProperty(essentialsKey)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("Missing '$essentialsKey' in Kotlin/Native gradle.properties for Kotlin $kotlinVersion")

    val remote = parseRemote(essentialSpec)
        ?: throw GradleException("Unsupported LLVM dependency spec '$essentialSpec' at $essentialsKey")

    val archiveExt = when {
        hostKey.startsWith("mingw") -> "zip"
        else -> "tar.gz"
    }

    val dependencyName = remote.remotePath.substringAfterLast('/')
        .takeIf { it.isNotEmpty() }
        ?: throw GradleException("Cannot extract dependency name from '${remote.remotePath}'")

    val baseUrl = when (remote.repo) {
        "public" -> "https://download.jetbrains.com/kotlin/native"
        else -> throw GradleException("Unsupported Kotlin/Native repository '${remote.repo}' in $essentialSpec")
    }

    val archiveUrl = "$baseUrl/${remote.remotePath}.${archiveExt}"

    return KotlinNativeLlvmBundle(
        dependencyName = dependencyName,
        archiveUrl = archiveUrl,
        archiveExt = archiveExt,
        llvmMajorVersion = llvmMajor
    )
}

private val kotlinNativeLlvmBundleProvider = providers.provider {
    kotlinNativeLlvmBundleFor(kotlinVersionProvider.get())
}

private fun llvmToolCandidates(root: File, toolName: String): List<File> {
    val exeSuffix = if (OperatingSystem.current().isWindows) ".exe" else ""
    return listOf(
        root.resolve("bin/$toolName$exeSuffix"),
        root.resolve("llvm/bin/$toolName$exeSuffix")
    )
}

private fun logToolCandidate(toolName: String, file: File) {
    val status = if (file.exists()) "exists" else "missing"
    logger.lifecycle("$toolName candidate $status: ${file.absolutePath}")
}

private fun logToolVersionOutput(toolFile: File) {
    try {
        val process = ProcessBuilder(toolFile.absolutePath, "--version")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        if (exit != 0) {
            throw GradleException("${toolFile.name} --version failed with exit $exit")
        }
        logger.lifecycle("${toolFile.name} --version:\n${output.trim()}")
    } catch (ex: IOException) {
        throw GradleException("Failed to execute ${toolFile.absolutePath} --version", ex)
    } catch (ex: InterruptedException) {
        Thread.currentThread().interrupt()
        throw GradleException("Interrupted while running ${toolFile.absolutePath} --version", ex)
    }
}

private fun curlDownload(url: String, destination: File, retries: Int = 3) {
    val curlExe = System.getenv("CURL") ?: "curl"
    destination.parentFile?.mkdirs()
    var lastExit = -1
    var lastErr: String? = null
    repeat(retries) { attempt ->
        if (destination.exists()) destination.delete()
        val pb = ProcessBuilder(
            curlExe,
            "-L",            // follow redirects
            "-f",            // fail on HTTP errors
            "-sS",           // silent + show errors
            "--connect-timeout", "10",
            "--retry", "2",
            "-o", destination.absolutePath,
            url
        )
        val proc = try { pb.start() } catch (e: IOException) {
            throw GradleException("Failed to start curl ($curlExe): ${e.message}", e)
        }
        val stderr = proc.errorStream.bufferedReader().use { it.readText() }
        val exit = proc.waitFor()
        if (exit == 0 && destination.isFile && destination.length() > 0) {
            if (attempt > 0) logger.lifecycle("curl succeeded for $url after ${attempt + 1} attempts")
            return
        }
        lastExit = exit
        lastErr = stderr.ifBlank { "exit=$exit" }
        logger.warn("curl attempt ${attempt + 1}/$retries failed for $url: $lastErr")
        Thread.sleep(1200L)
    }
    destination.delete()
    throw GradleException("curl failed to download $url (exit=$lastExit): $lastErr")
}

private fun File.ensureExecutable() {
    if (exists() && !canExecute()) {
        setExecutable(true)
    }
}

fun toolMajorVersion(exePath: String): Int? {
    return try {
        val pb = ProcessBuilder(exePath, "--version").redirectErrorStream(true)
        val proc = pb.start()
        val out = proc.inputStream.bufferedReader().use { it.readText() }
        runCatching { proc.waitFor() }
        logger.lifecycle("$exePath version found out: $out")
        Regex("""\b(?:LLVM|clang)\s+version\s+([0-9]+)""").find(out)?.groupValues?.getOrNull(1)?.toIntOrNull()
    } catch (_: Exception) { null }
}

// Removed findMacClang(): we must avoid mixing Apple clang (Xcode) with LLVM/clang used by Kotlin/Native.
// Enforce a single clang/llvm toolchain with the required major version (llvmVersion).

fun findTool(toolName: String, requiredMajorVersion: Int? = null): String {
    System.getenv(toolName.uppercase())?.let { p ->
        val f = File(p); if (f.canExecute()) return f.absolutePath
    }
    val os = OperatingSystem.current()
    val exeName = if (os.isWindows) "$toolName.exe" else toolName

    val needsVersioned = requiredMajorVersion != null

    // Prefer Kotlin/Native bundled toolchains (downloaded via Kotlin/Native Gradle tasks) to stay in sync with cinterop
    run {
        val home = System.getProperty("user.home")
        val konanDepsDir = File(home, ".konan/dependencies")
        val candidates = konanDepsDir.listFiles { f -> f.isDirectory && f.name.startsWith("llvm-") }
            ?.sortedByDescending { it.name }
            ?: emptyList()
        for (dir in candidates) {
            listOf(
                File(dir, "bin/$exeName"),
                File(dir, "llvm/bin/$exeName")
            ).firstOrNull { it.canExecute() }?.let { candidate ->
                if (!needsVersioned || toolMajorVersion(candidate.absolutePath) == requiredMajorVersion) {
                    logger.lifecycle("${candidate.absolutePath} found in .konan will be used")
                    return candidate.absolutePath
                }
            }
        }
    }

    val reason = requiredMajorVersion?.let { "Require major version $it for $toolName." } ?: ""
    val hint = "Ensure the Kotlin/Native toolchain is downloaded (run ':cli:downloadKonanLlvm') or set env ${toolName.uppercase()}"
    throw GradleException("Cannot find $toolName in Kotlin/Native dependencies. $reason $hint.")
}

// Always use version-gated clang/llvm on all platforms to avoid PCH/ABI mismatches
val clangPath = providers.provider { findTool("clang", kotlinNativeLlvmBundleProvider.get().llvmMajorVersion) }
val llvmArPath = providers.provider { findTool("llvm-ar", kotlinNativeLlvmBundleProvider.get().llvmMajorVersion) }

// 1) TAR bblpacks preserving path "bblpacks/<t>/..."
val tarBblpacks = tasks.register("tarBblpacks") {
    inputs.dir(bblpacksDir)
    outputs.dir(tarOutDir)

    doLast {
        val srcRoot = resourcesRoot.asFile
        val tarDir = tarOutDir.get().asFile
        tarDir.mkdirs()

        val translations = bblpacksDir.asFile.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted().orEmpty()
        if (translations.isEmpty()) logger.warn("No translations found in $bblpacksDir")

        val tarExe = System.getenv("TAR") ?: "tar"
        val tarAvailable = try {
            val result = providers.exec {
                commandLine(tarExe, "--version")
                isIgnoreExitValue = true
            }.result.get()
            if(result.exitValue != 0){
                throw GradleException("TAR not available or version check failed: ${result.exitValue}")
            }else{
                true
            }
        } catch (_: Exception) { false }

        translations.forEach { t ->
            val outTar = File(tarDir, "$t.tar")
            if (tarAvailable) {
                providers.exec {
                    workingDir = srcRoot
                    commandLine(tarExe, "-cf", outTar.absolutePath, "bblpacks/$t")
                }.result.get().assertNormalExitValue()
            } else {
                // Minimal non-POSIX fallback to keep builds unblocked (prefer installing tar).
                logger.warn("System 'tar' not found; writing a minimal fallback archive for $t (not POSIX TAR). Prefer installing tar.")
                val base = File(srcRoot, "bblpacks/$t")
                outTar.outputStream().use { out ->
                    base.walkTopDown().filter { it.isFile }.forEach { f ->
                        val rel = "bblpacks/$t/" + f.relativeTo(base).invariantSeparatorsPath
                        val bytes = f.readBytes()
                        val header = "FILE:$rel:${bytes.size}\n".toByteArray()
                        out.write(header); out.write(bytes); out.write("\nEND\n".toByteArray())
                    }
                }
            }
        }
    }
}

// 2) Generate C arrays and combined header
val generateCFromTar = tasks.register("generateCFromTar") {
    dependsOn(tarBblpacks)
    inputs.dir(tarOutDir)
    outputs.dir(cOutDir)
    outputs.dir(includeOutDir)

    doLast {
        val tarDir = tarOutDir.get().asFile
        val cDir = cOutDir.get().asFile.also { it.mkdirs() }
        val incDir = includeOutDir.get().asFile.also { it.mkdirs() }

        val tars = tarDir.listFiles { f -> f.isFile && f.name.endsWith(".tar") }?.sortedBy { it.name }.orEmpty()
        if (tars.isEmpty()) logger.warn("No .tar files in $tarDir")

        val header = File(incDir, "generated_bibles.h")
        val externs = StringBuilder().apply {
            appendLine("#pragma once")
            appendLine("#ifdef __cplusplus")
            appendLine("extern \"C\" {")
            appendLine("#endif")
            appendLine()
        }

        tars.forEach { tar ->
            val name = tar.nameWithoutExtension // translation
            val symbol = "${name}_tar"
            val bytes = Files.readAllBytes(tar.toPath())
            val cFile = File(cDir, "$symbol.c")

            cFile.printWriter().use { pw ->
                pw.println("/* Auto-generated. Do not edit. */")
                pw.println("#include <stddef.h>")
                pw.println("const unsigned char $symbol[] = {")
                bytes.asSequence().chunked(16).forEach { chunk ->
                    val line = chunk.joinToString(", ") { String.format("0x%02X", it) }
                    pw.println("  $line,")
                }
                pw.println("};")
                pw.println("const unsigned int ${symbol}_len = ${bytes.size};")
            }

            externs.appendLine("extern const unsigned char ${symbol}[];   extern const unsigned int ${symbol}_len;")
        }

        externs.appendLine()
        externs.appendLine("#ifdef __cplusplus")
        externs.appendLine("}")
        externs.appendLine("#endif")

        header.writeText(externs.toString())
        logger.lifecycle("Wrote ${header.absolutePath} and ${cDir.listFiles()?.size ?: 0} C files")
    }
}

// Ensure Kotlin/Native downloads its toolchain (llvm/clang, etc.) before we try to build C artifacts.
val ensureKonanToolchain = tasks.register("ensureKonanToolchain") {
    group = "build setup"
    description = "Ensures the Kotlin/Native toolchain (LLVM/Clang) is downloaded before embedding resources."
}

// Download the Kotlin/Native LLVM bundle into KONAN_DATA_DIR when absent.
val downloadKonanLlvm = tasks.register("downloadKonanLlvm") {
    group = "build setup"
    description = "Downloads the Kotlin/Native LLVM toolchain archive that ships with the configured Kotlin version."

    doLast {
        val kotlinVersion = kotlinVersionProvider.get()
        val bundle = kotlinNativeLlvmBundleProvider.get()
        val konanDataDir = konanDataDir()
        val dependenciesDir = konanDataDir.resolve("dependencies")
        val targetDir = dependenciesDir.resolve(bundle.dependencyName)

        if (targetDir.exists()) {
            logger.lifecycle("LLVM bundle directory exists: ${targetDir.absolutePath}")
        } else {
            logger.lifecycle("LLVM bundle directory missing: ${targetDir.absolutePath}")
        }

        val clangCandidates = llvmToolCandidates(targetDir, "clang")
        val llvmArCandidates = llvmToolCandidates(targetDir, "llvm-ar")

        clangCandidates.forEach { logToolCandidate("clang", it) }
        llvmArCandidates.forEach { logToolCandidate("llvm-ar", it) }

        val resolvedClang = clangCandidates.firstOrNull { it.canExecute() }
        val resolvedAr = llvmArCandidates.firstOrNull { it.canExecute() }

        val needsDownload = resolvedClang == null || resolvedAr == null

        if (needsDownload) {
            if (gradle.startParameter.isOffline) {
                throw GradleException("Offline mode cannot download Kotlin/Native LLVM bundle ${bundle.dependencyName}")
            }

            dependenciesDir.mkdirs()

            val tmpDir = layout.buildDirectory.dir("tmp/${bundle.dependencyName}").get().asFile.also { it.mkdirs() }
            val archiveFile = tmpDir.resolve("${bundle.dependencyName}.${bundle.archiveExt}")

            logger.lifecycle("Downloading Kotlin/Native LLVM bundle ${bundle.dependencyName} (Kotlin $kotlinVersion) from ${bundle.archiveUrl}")
            curlDownload(bundle.archiveUrl, archiveFile, retries = 3)
            if (!archiveFile.exists() || archiveFile.length() == 0L) {
                throw GradleException("Download produced an empty archive for ${bundle.archiveUrl}")
            }

            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }

            when (bundle.archiveExt) {
                "zip" -> project.copy { from(zipTree(archiveFile)); into(dependenciesDir) }
                "tar.gz" -> {
                    val tarExe = System.getenv("TAR") ?: "tar"
                    val result = providers.exec {
                        commandLine(tarExe, "-xzf", archiveFile.absolutePath, "-C", dependenciesDir.absolutePath)
                        isIgnoreExitValue = true
                    }.result.get()
                    if (result.exitValue != 0) {
                        throw GradleException("tar extraction failed for ${bundle.dependencyName} with exit ${result.exitValue}")
                    }
                }
                else -> throw GradleException("Unsupported archive extension '${bundle.archiveExt}' for ${bundle.dependencyName}")
            }

            archiveFile.delete()
            tmpDir.deleteRecursively()

            logger.lifecycle("Installed Kotlin/Native LLVM bundle to ${targetDir.absolutePath}")
        } else {
            logger.lifecycle("Reusing existing Kotlin/Native LLVM bundle at ${targetDir.absolutePath}")
        }

        val finalClang = clangCandidates.firstOrNull { it.exists() }
            ?: throw GradleException("clang binary not found in ${targetDir.absolutePath}")
        val finalAr = llvmArCandidates.firstOrNull { it.exists() }
            ?: throw GradleException("llvm-ar binary not found in ${targetDir.absolutePath}")

        finalClang.ensureExecutable()
        finalAr.ensureExecutable()

        logToolVersionOutput(finalClang)
        logToolVersionOutput(finalAr)
    }
}

// 3) Compile C → .o
val compileCToObjects = tasks.register("compileCToObjects") {
    dependsOn(ensureKonanToolchain)
    dependsOn(downloadKonanLlvm)
    dependsOn(generateCFromTar)
    inputs.dir(cOutDir)
    outputs.dir(oOutDir)

    doLast {
        val cDir = cOutDir.get().asFile
        val objDir = oOutDir.get().asFile.also { it.mkdirs() }
        val clang = clangPath.get()
        logger.lifecycle("Compiling c files to .o files with: $clang")

        val sources = cDir.listFiles { f -> f.isFile && f.extension == "c" }?.sortedBy { it.name }.orEmpty()
        sources.forEach { c ->
            val out = File(objDir, c.nameWithoutExtension + ".o")
            providers.exec { commandLine(clang, "-c", "-O2", c.absolutePath, "-o", out.absolutePath) }.result.get().assertNormalExitValue()
        }
    }
}

// 4) Archive .o → libbibles.a
val buildEmbeddedArchive = tasks.register("buildEmbeddedArchive") {
    dependsOn(compileCToObjects)
    inputs.dir(oOutDir)
    outputs.file(libOutFile)

    doLast {
        val objDir = oOutDir.get().asFile
        val objs = objDir.listFiles { f -> f.isFile && f.extension == "o" }?.map { it.absolutePath }.orEmpty()
        val ar = llvmArPath.get()
        val lib = libOutFile.get().asFile
        if (objs.isEmpty()) logger.warn("No object files to archive in $objDir")
        providers.exec { commandLine(ar, "rcs", lib.absolutePath, *objs.toTypedArray()) }.result.get().assertNormalExitValue()
        logger.lifecycle("from .o files, built Archive file: ${lib.absolutePath}")
    }
}

// 5) Generate Kotlin binding from generated header
val generateTarBindingsKt = tasks.register("generateTarBindingsKt") {
    dependsOn(generateCFromTar)
    val headerFile = includeOutDir.map { it.file("generated_bibles.h") }
    inputs.file(headerFile)
    outputs.dir(generatedKtDir)

    doLast {
        val incDir = includeOutDir.get().asFile
        val header = File(incDir, "generated_bibles.h")
        val ktDir = generatedKtDir.get().asFile.resolve("org/gnit/bible/cli").also { it.mkdirs() }
        val ktFile = ktDir.resolve("GeneratedTarBindings.kt")

        val names = header.readLines()
            .mapNotNull { Regex("""extern const unsigned char ([a-zA-Z0-9_]+)\[\];""").find(it)?.groupValues?.getOrNull(1) }
            .filter { it.endsWith("_tar") }
            .sorted()

        val body = buildString {
            appendLine("/* Auto-generated. Do not edit. */")
            appendLine("package org.gnit.bible.cli")
            appendLine()
            appendLine("import kotlinx.cinterop.CPointer")
            appendLine("import kotlinx.cinterop.UByteVar")
            appendLine("import org.gnit.bible.cli.ci.*")
            appendLine()
            appendLine("internal fun generatedReaderFor(translation: String): TarPtrReader? = when (translation) {")
            names.forEach { sym ->
                val t = sym.removeSuffix("_tar")
                appendLine("    \"$t\" -> TarPtrReader(${sym}, ${sym}_len.toInt())")
            }
            appendLine("    else -> null")
            appendLine("}")
        }

        Files.writeString(ktFile.toPath(), body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        logger.lifecycle("Wrote ${ktFile.absolutePath}")
    }
}

// 6) Aggregate task
tasks.register("embedBblpacks") {
    dependsOn(buildEmbeddedArchive, generateTarBindingsKt)
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    tasks.matching { it.name.startsWith("cinteropBibles") }.configureEach {
        dependsOn(tasks.named("embedBblpacks"))
        doFirst {
            logger.lifecycle("cinterop depends on header dir: ${includeOutDir.get().asFile.absolutePath}")
        }
    }
}

// 7) Make generated Kotlin visible to `nativeMain` (reflection-safe)
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    val kmpExt = extensions.findByName("kotlin") ?: return@withId
    // reflect: val sourceSets = kmpExt.sourceSets
    val sourceSets = kmpExt.javaClass.methods
        .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
        ?.invoke(kmpExt) as? Iterable<*> ?: return@withId

    val genDirFile = generatedKtDir.get().asFile

    sourceSets.forEach { ss ->
        val name = ss?.javaClass?.methods
            ?.firstOrNull { it.name == "getName" && it.parameterCount == 0 }
            ?.invoke(ss) as? String ?: return@forEach
        if (name == "nativeMain") {
            val kotlinSet = ss.javaClass.methods
                .firstOrNull { it.name == "getKotlin" && it.parameterCount == 0 }
                ?.invoke(ss) ?: return@forEach
            val srcDirMethod = kotlinSet.javaClass.methods
                .firstOrNull { it.name == "srcDir" && it.parameterCount == 1 }
                ?: return@forEach
            srcDirMethod.invoke(kotlinSet, genDirFile)
            logger.lifecycle("Added generated dir ${genDirFile.absolutePath} to nativeMain")
        }
    }
}

// 8) Ensure native link depends on embed
val kotlinNativeLinkClass = runCatching {
    Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink")
}.getOrNull()

if (kotlinNativeLinkClass == null) {
    logger.info("KotlinNativeLink class not found on classpath (plugin may not be applied yet).")
} else {
    tasks.matching { kotlinNativeLinkClass.isInstance(it) }.configureEach {
        dependsOn(tasks.named("embedBblpacks"))
    }
}
