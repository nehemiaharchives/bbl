import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Sync
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.Locale
import javax.inject.Inject

// This script plugin is applied to the 'cli' project.
// It generates:
// - build/embedded/tar/*.tar
// - build/embedded/c/*.c
// - build/embedded/obj/*.o
// - build/embedded/libbibles.a
// - build/embedded/include/generated_bibles.h
// - build/generated/cli/org/gnit/bible/cli/GeneratedTarBindings.kt

val resourcesRoot = rootProject.layout.projectDirectory.dir("composeApp/src/commonMain/composeResources/files")
val bblpacksDirProvider = resourcesRoot.dir("bblpacks")
val embedBuild = layout.buildDirectory.dir("embedded")
val cOutDir = embedBuild.map { it.dir("c") }
val tarOutDir = embedBuild.map { it.dir("tar") }
val includeOutDir = embedBuild.map { it.dir("include") }
val generatedKtDir = layout.buildDirectory.dir("generated/cli-linuxX64Main")
val cinteropConfigDirProvider = layout.projectDirectory.dir("src/nativeMain/cinterop")
val cinteropDefFileProvider = layout.projectDirectory.file("src/nativeMain/cinterop/bibles.def")

val cinteropCompilerOptionsProvider = includeOutDir.map { includeDir ->
    listOf("-I" + includeDir.asFile.absolutePath)
}

extensions.extraProperties.set("bblpacksCompilerOpts", cinteropCompilerOptionsProvider)

private val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

private val kotlinVersionProvider = providers.provider {
    libsCatalog.findVersion("kotlin").orElse(null)?.requiredVersion
        ?: throw GradleException("Could not resolve Kotlin version from libs catalog 'libs'.")
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

private fun ensureDependencyMarkedExtracted(dependenciesDir: File, dependencyName: String, logger: org.gradle.api.logging.Logger) {
    val marker = dependenciesDir.resolve(".extracted")
    val existing = if (marker.exists()) {
        marker.readLines().mapNotNull { it.trim().takeIf(String::isNotEmpty) }.toMutableSet()
    } else {
        mutableSetOf()
    }

    if (existing.add(dependencyName)) {
        marker.parentFile?.mkdirs()
        val sorted = existing.toMutableList().sorted()
        marker.writeText(sorted.joinToString(separator = "\n", postfix = "\n"))
        logger.lifecycle("Marked Kotlin/Native dependency '$dependencyName' as extracted in ${marker.absolutePath}")
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

@CacheableTask
abstract class TarBblpacksTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    /**
     * Only include files that actually go into the output TARs.
     *
     * Important: Do not snapshot the whole bblpacks directory, because IDE sync and other tooling
     * may touch unrelated files (or create transient lock/tmp files), which would invalidate the task.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bblpacksInputFiles: ConfigurableFileCollection

    @get:Internal
    abstract val bblpacksDir: DirectoryProperty

    @get:Internal
    abstract val resourcesRootDir: DirectoryProperty

    // NOTE: This task produces deterministic TARs from bible pack text/index files only.
    // Do NOT add unrelated inputs here (like cinterop def/include dirs), otherwise IDE sync
    // or build directory changes will invalidate the task and defeat UP-TO-DATE checking.

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val tarExecutable: Property<String>

    // Note: Kotlin version does not influence tar outputs; avoid making this task sensitive to version catalog changes.

    @TaskAction
    fun generate() {
        val srcRoot = resourcesRootDir.get().asFile
        val tarDir = outputDirectory.get().asFile

        val translations = bblpacksDir.get().asFile.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()

        // If there are no translations (or bblpacks is missing), ensure outputs are cleared so the task is stable.
        if (translations.isEmpty()) {
            if (tarDir.exists()) {
                tarDir.deleteRecursively()
            }
            tarDir.mkdirs()
            logger.warn("No translations found in ${bblpacksDir.get().asFile}")
            return
        }

        tarDir.mkdirs()

        val tarExe = tarExecutable.get()

        val tarVersion = try {
            val result = execOperations.exec {
                commandLine(tarExe, "--version")
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                execOperations.exec {
                    commandLine(tarExe, "--version")
                    isIgnoreExitValue = true
                    // capture output is not directly available here; best-effort via exit code
                }
                "ok"
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        // GNU tar supports reproducibility flags; enable determinism when we can.
        val isGnuTar = try {
            val pb = ProcessBuilder(tarExe, "--version").redirectErrorStream(true)
            val proc = pb.start()
            val out = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor()
            out.contains("GNU tar", ignoreCase = true)
        } catch (_: Exception) {
            false
        }

        fun stageTranslationFiles(translation: String): File {
            val source = File(srcRoot, "bblpacks/$translation")
            if (!source.isDirectory) {
                throw GradleException("Expected translation directory to exist: ${source.absolutePath}")
            }

            // Stage only what the CLI needs:
            // - chapter text files: *.txt (flat)
            // - lucene-kmp index files: index/**
            // This makes the embedded TAR deterministic and guarantees index files are present.
            val stageRoot = File(temporaryDir, "bblpacks-stage/$translation").also {
                if (it.exists()) it.deleteRecursively()
                it.mkdirs()
            }
            val stageTranslationDir = File(stageRoot, "bblpacks/$translation").also { it.mkdirs() }

            // Ensure deterministic traversal order by sorting by relative path.
            val included = source.walkTopDown()
                .filter { it.isFile }
                .mapNotNull { f ->
                    val rel = f.relativeTo(source).invariantSeparatorsPath
                    val include = rel.endsWith(".txt") || rel.startsWith("index/")
                    if (!include) return@mapNotNull null
                    if (rel == "index/write.lock") return@mapNotNull null
                    rel to f
                }
                .toList()
                .sortedBy { (rel, _) -> rel }

            for ((rel, f) in included) {
                val dest = File(stageTranslationDir, rel)
                dest.parentFile?.mkdirs()
                Files.copy(f.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }

            return stageRoot
        }

        // Avoid parallel writes to reduce non-determinism and make diagnostics stable.
        for (t in translations) {
            logger.lifecycle("Archiving bblpack for $t")
            val outTar = File(tarDir, "$t.tar")
            val stageRoot = stageTranslationFiles(t)

            val tarAvailable = tarVersion != null
            if (tarAvailable) {
                val args = mutableListOf<String>()
                // tar -C <dir> ... is more portable than setting workingDir on some platforms.
                // However, Gradle Exec supports workingDir fine; keeping it.

                if (isGnuTar) {
                    // Deterministic archives across invocations on the same inputs.
                    args.addAll(
                        listOf(
                            "--sort=name",
                            "--mtime=@0",
                            "--owner=0",
                            "--group=0",
                            "--numeric-owner"
                        )
                    )
                }

                val execResult = execOperations.exec {
                    workingDir = stageRoot
                    commandLine(tarExe, *args.toTypedArray(), "-cf", outTar.absolutePath, "bblpacks/$t")
                }
                execResult.assertNormalExitValue()
            } else {
                // Fallback deterministic writer (stable order because of sorting above).
                val base = File(stageRoot, "bblpacks/$t")
                outTar.outputStream().use { out ->
                    base.walkTopDown()
                        .filter { it.isFile }
                        .map { f ->
                            val rel = "bblpacks/$t/" + f.relativeTo(base).invariantSeparatorsPath
                            rel to f
                        }
                        .toList()
                        .sortedBy { (rel, _) -> rel }
                        .forEach { (rel, f) ->
                            val bytes = f.readBytes()
                            val header = "FILE:$rel:${bytes.size}\n".toByteArray()
                            out.write(header)
                            out.write(bytes)
                            out.write("\nEND\n".toByteArray())
                        }
                }
            }
        }
    }
}

@CacheableTask
abstract class ObjcopyTarToObjectsTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tarInputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val objectOutputDir: DirectoryProperty

    @get:Input
    abstract val objcopyExecutable: Property<String>

    @get:Input
    abstract val outputFormat: Property<String>

    @get:Input
    abstract val binaryArch: Property<String>

    @TaskAction
    fun generate() {
        val tarDir = tarInputDir.get().asFile
        val objDir = objectOutputDir.get().asFile.also { it.mkdirs() }
        val objcopy = objcopyExecutable.get()
        val tars = tarDir.listFiles { f -> f.isFile && f.name.endsWith(".tar") }
            ?.sortedBy { it.name }
            .orEmpty()

        if (tars.isEmpty()) {
            logger.warn("No .tar files to objcopy in $tarDir")
        }

        for (tar in tars) {
            val out = File(objDir, tar.nameWithoutExtension + "_tar.o")
            val execResult = execOperations.exec {
                workingDir = tarDir
                commandLine(
                    objcopy,
                    "--input-target=binary",
                    "--output-target=${'$'}{outputFormat.get()}",
                    "--binary-architecture=${'$'}{binaryArch.get()}",
                    tar.name,
                    out.absolutePath
                )
            }
            execResult.assertNormalExitValue()
            logger.lifecycle("Objcopied ${tar.name} -> ${out.name}")
        }
    }
}

@CacheableTask
abstract class GenerateCFromTarTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tarInputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val cOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val includeOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val tarDir = tarInputDir.get().asFile
        val cDir = cOutputDir.get().asFile.also {
            it.mkdirs(); it.listFiles()?.forEach(File::deleteRecursively)
        }
        val incDir = includeOutputDir.get().asFile.also {
            it.mkdirs(); it.listFiles()?.forEach(File::deleteRecursively)
        }

        val tars = tarDir.listFiles { f -> f.isFile && f.name.endsWith(".tar") }
            ?.sortedBy { it.name }
            .orEmpty()

        if (tars.isEmpty()) {
            logger.warn("No .tar files in $tarDir")
        }

        fun symbolBase(name: String) = name.replace(Regex("[^A-Za-z0-9]"), "_")

        val entries = tars.map { tar ->
            val base = symbolBase(tar.name)
            Triple(tar.nameWithoutExtension, "_binary_${base}_start", "_binary_${base}_end")
        }

        val header = File(incDir, "generated_bibles.h")
        header.writeText(
            buildString {
                appendLine("#pragma once")
                appendLine("#include <stddef.h>")
                appendLine("#ifdef __cplusplus")
                appendLine("extern \"C\" {")
                appendLine("#endif")
                appendLine("struct EmbeddedTar { const char* name; const unsigned char* data; size_t len; };")
                appendLine("const struct EmbeddedTar* find_embedded_tar(const char* name);")
                appendLine("const struct EmbeddedTar* embedded_tar_at(size_t index);")
                appendLine("size_t embedded_tar_count(void);")
                appendLine()
                entries.forEach { (_, startSym, endSym) ->
                    appendLine("extern const unsigned char ${startSym}[];")
                    appendLine("extern const unsigned char ${endSym}[];")
                }
                appendLine("#ifdef __cplusplus")
                appendLine("}")
                appendLine("#endif")
            }
        )

        val tableC = File(cDir, "generated_bibles_table.c")
        tableC.writeText(
            buildString {
                appendLine("/* Auto-generated. Do not edit. */")
                appendLine("#include <stddef.h>")
                appendLine("#include <string.h>")
                appendLine("#include \"generated_bibles.h\"")
                appendLine()
                appendLine("static const struct EmbeddedTar EMBEDDED_TARS[] = {")
                entries.forEach { (name, startSym, endSym) ->
                    appendLine("  { \"$name\", $startSym, (size_t)($endSym - $startSym) },")
                }
                appendLine("};")
                appendLine()
                appendLine("size_t embedded_tar_count(void) { return sizeof(EMBEDDED_TARS) / sizeof(EMBEDDED_TARS[0]); }")
                appendLine("const struct EmbeddedTar* embedded_tar_at(size_t index) {")
                appendLine("  return index < embedded_tar_count() ? &EMBEDDED_TARS[index] : NULL;")
                appendLine("}")
                appendLine("const struct EmbeddedTar* find_embedded_tar(const char* name) {")
                appendLine("  if (!name) return NULL;")
                appendLine("  for (size_t i = 0; i < embedded_tar_count(); ++i) {")
                appendLine("    if (strcmp(name, EMBEDDED_TARS[i].name) == 0) return &EMBEDDED_TARS[i];")
                appendLine("  }")
                appendLine("  return NULL;")
                appendLine("}")
            }
        )
        logger.lifecycle("Wrote ${header.absolutePath} and ${tableC.absolutePath}")
    }
}

@CacheableTask
abstract class CompileCToObjectsTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cInputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val objectOutputDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeDirectory: DirectoryProperty

    @get:Input
    abstract val clangExecutable: Property<String>

    @get:Input
    abstract val targetTriple: Property<String>

    @get:Input
    abstract val kotlinVersion: Property<String>

    @TaskAction
    fun compile() {
        val cDir = cInputDir.get().asFile
        val objDir = objectOutputDir.get().asFile.also { it.mkdirs() }
        val clang = clangExecutable.get()
        val incDir = includeDirectory.get().asFile

        val sources = cDir.listFiles { f -> f.isFile && f.extension == "c" }
            ?.sortedBy { it.name }
            .orEmpty()

        if (sources.isEmpty()) {
            logger.warn("No C sources to compile in $cDir")
            return
        }

        val target = targetTriple.get()
        sources.parallelStream().forEach { c ->
            val out = File(objDir, c.nameWithoutExtension + ".o")
            val execResult = execOperations.exec {
                commandLine(
                    clang,
                    "-c",
                    "-O2",
                    "-target",
                    target,
                    "-I",
                    incDir.absolutePath,
                    c.absolutePath,
                    "-o",
                    out.absolutePath
                )
            }
            execResult.assertNormalExitValue()
        }
    }
}

@CacheableTask
abstract class BuildEmbeddedArchiveTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val objectInputDir: DirectoryProperty

    @get:OutputFile
    abstract val archiveOutputFile: RegularFileProperty

    @get:Input
    abstract val arExecutable: Property<String>

    @TaskAction
    fun archive() {
        val objDir = objectInputDir.get().asFile
        val ar = arExecutable.get()
        val lib = archiveOutputFile.get().asFile
        lib.parentFile?.mkdirs()

        val objs = objDir.listFiles { f -> f.isFile && f.extension == "o" }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
            .orEmpty()

        if (objs.isEmpty()) {
            logger.warn("No object files to archive in $objDir")
        }

        val execResult = execOperations.exec {
            commandLine(ar, "rcs", lib.absolutePath, *objs.toTypedArray())
        }
        execResult.assertNormalExitValue()
        logger.lifecycle("from .o files, built Archive file: ${lib.absolutePath}")
    }
}

@CacheableTask
abstract class GenerateTarBindingsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headerFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tarInputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val baseOutput = outputDirectory.get().asFile
        val ktDir = File(baseOutput, "org/gnit/bible/cli").also { it.mkdirs() }
        val ktFile = File(ktDir, "GeneratedTarBindings.kt")

        val translations = tarInputDir.get().asFile
            .listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.name.endsWith(".tar") }
            ?.map { it.name.removeSuffix(".tar") }
            ?.sorted()
            ?.toList()
            ?: emptyList()

        val body = buildString {
            appendLine("/* Auto-generated. Do not edit. */")
            appendLine("package org.gnit.bible.cli")
            appendLine()
            appendLine("import bibles.embedded_tar_count")
            appendLine("import bibles.embedded_tar_at")
            appendLine("import bibles.find_embedded_tar")
            appendLine("import kotlinx.cinterop.ExperimentalForeignApi")
            appendLine("import kotlinx.cinterop.pointed")
            appendLine("import kotlinx.cinterop.readBytes")
            appendLine()
            appendLine("private val embeddedTranslations: Set<String> = setOf(${translations.joinToString { "\"$it\"" }})")
            appendLine()
            appendLine("@OptIn(ExperimentalForeignApi::class)")
            appendLine("private fun loadTarBytes(name: String): ByteArray? {")
            appendLine("    val entry = find_embedded_tar(name) ?: return null")
            appendLine("    val len = entry.pointed.len.toLong()")
            appendLine("    if (len <= 0L || len > Int.MAX_VALUE) return null")
            appendLine("    val ptr = entry.pointed.data ?: return null")
            appendLine("    return ptr.readBytes(len.toInt())")
            appendLine("}")
            appendLine()
            appendLine("internal actual fun generatedReaderFor(translation: String): TarBytesReader? {")
            appendLine("    if (!embeddedTranslations.contains(translation)) return null")
            appendLine("    val bytes = loadTarBytes(translation) ?: return null")
            appendLine("    return TarBytesReader(bytes, translation)")
            appendLine("}")
        }

        Files.writeString(
            ktFile.toPath(),
            body,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        logger.lifecycle("Wrote ${'$'}{ktFile.absolutePath}")
    }
}

// Always use version-gated clang/llvm on all platforms to avoid PCH/ABI mismatches
val clangPath = providers.provider { findTool("clang", kotlinNativeLlvmBundleProvider.get().llvmMajorVersion) }
val llvmArPath = providers.provider { findTool("llvm-ar", kotlinNativeLlvmBundleProvider.get().llvmMajorVersion) }
val llvmObjcopyPath: Provider<String> = providers.provider { findTool("llvm-objcopy", kotlinNativeLlvmBundleProvider.get().llvmMajorVersion) }

// --- Embed/cinterop wiring control ---
// IDE sync and Kotlin/Native commonize/metadata tasks may request cinterop/link task models.
// If those tasks unconditionally depend on embedBblpacks, then Gradle sync ends up executing the
// whole embedding pipeline (tarBblpacks -> generateCFromTar -> compileC... -> lib + header).
//
// Behavior:
// - Normal builds: embed wired by default.
// - IDE sync: embed NOT wired by default.
// - Override any time with: -Pbblpacks.embed=true / -Pbblpacks.embed=false
val bblpacksEmbedRequested: Provider<Boolean> = providers.gradleProperty("bblpacks.embed")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(true)

// IntelliJ / Android Studio mark sync in different ways depending on version.
// We treat *any* of these as a sync context.
val isIdeaSyncActive: Provider<Boolean> = listOf(
    "idea.sync.active",
    // Seen in Android Studio / AGP tooling.
    "android.injected.invoked.from.ide",
    // Fallback (IDE is running). Not perfect, but helps.
    "idea.active"
).map { key ->
    providers.systemProperty(key).map { it.equals("true", ignoreCase = true) }.orElse(false)
}.reduce { acc, next ->
    acc.zip(next) { a, b -> a || b }
}

val bblpacksEmbedEnabled: Provider<Boolean> = bblpacksEmbedRequested.zip(isIdeaSyncActive) { requested, isSync ->
    requested && !isSync
}

// 1) TAR bblpacks preserving path "bblpacks/<t>/..."
val tarBblpacks = tasks.register<TarBblpacksTask>("tarBblpacks") {
    bblpacksDir.set(bblpacksDirProvider)
    resourcesRootDir.set(resourcesRoot)

    // Track only files that are actually included in the TAR.
    bblpacksInputFiles.from(
        bblpacksDirProvider.asFileTree.matching {
            include("**/*.txt")
            include("**/index/**")
            exclude("**/index/write.lock")
            // be defensive: ignore other well-known transient files
            exclude("**/*.lock")
            exclude("**/.DS_Store")
        }
    )

    outputDirectory.set(tarOutDir)
    tarExecutable.set(providers.environmentVariable("TAR").orElse("tar"))
}

// 2) Generate C arrays and combined header
val generateCFromTar = tasks.register<GenerateCFromTarTask>("generateCFromTar") {
    dependsOn(tarBblpacks)
    tarInputDir.set(tarOutDir)
    cOutputDir.set(cOutDir)
    includeOutputDir.set(includeOutDir)
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

    val bundle = kotlinNativeLlvmBundleProvider.get()
    val depsDirFile = konanDataDir().resolve("dependencies")
    val targetDir = File(depsDirFile, bundle.dependencyName)
    val tarExe = System.getenv("TAR") ?: "tar"

    inputs.property("bundleName", bundle.dependencyName)
    inputs.property("archiveUrl", bundle.archiveUrl)
    inputs.property("archiveExt", bundle.archiveExt)
    inputs.property("kotlinVersion", kotlinVersionProvider.get())
    outputs.dir(targetDir)

    outputs.upToDateWhen {
        targetDir.isDirectory &&
            llvmToolCandidates(targetDir, "clang").any { it.canExecute() } &&
            llvmToolCandidates(targetDir, "llvm-ar").any { it.canExecute() }
    }

    doLast {
        val clangCandidates = llvmToolCandidates(targetDir, "clang")
        val llvmArCandidates = llvmToolCandidates(targetDir, "llvm-ar")
        val resolvedClang = clangCandidates.firstOrNull { it.canExecute() }
        val resolvedAr = llvmArCandidates.firstOrNull { it.canExecute() }

        val needsDownload = resolvedClang == null || resolvedAr == null
        if (!needsDownload) {
            logger.lifecycle("Reusing existing Kotlin/Native LLVM bundle at ${targetDir.absolutePath}")
            return@doLast
        }

        if (gradle.startParameter.isOffline) {
            throw GradleException("Offline mode cannot download Kotlin/Native LLVM bundle ${bundle.dependencyName}")
        }

        depsDirFile.mkdirs()
        val tmpDir = layout.buildDirectory.dir("tmp/${bundle.dependencyName}").get().asFile.also { it.mkdirs() }
        val archiveFile = tmpDir.resolve("${bundle.dependencyName}.${bundle.archiveExt}")

        logger.lifecycle("Downloading Kotlin/Native LLVM bundle ${bundle.dependencyName} (Kotlin ${kotlinVersionProvider.get()}) from ${bundle.archiveUrl}")
        curlDownload(bundle.archiveUrl, archiveFile, retries = 3)
        if (!archiveFile.exists() || archiveFile.length() == 0L) {
            throw GradleException("Download produced an empty archive for ${bundle.archiveUrl}")
        }

        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }

        when (bundle.archiveExt) {
            "zip" -> project.copy { from(zipTree(archiveFile)); into(depsDirFile) }
            "tar.gz" -> {
                val result = providers.exec {
                    commandLine(tarExe, "-xzf", archiveFile.absolutePath, "-C", depsDirFile.absolutePath)
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

        val finalClang = clangCandidates.firstOrNull { it.exists() }
            ?: throw GradleException("clang binary not found in ${targetDir.absolutePath}")
        val finalAr = llvmArCandidates.firstOrNull { it.exists() }
            ?: throw GradleException("llvm-ar binary not found in ${targetDir.absolutePath}")

        finalClang.ensureExecutable()
        finalAr.ensureExecutable()

        logToolVersionOutput(finalClang)
        logToolVersionOutput(finalAr)
        ensureDependencyMarkedExtracted(depsDirFile, bundle.dependencyName, logger)
    }
}

// 3) Compile C → .o
data class NativeVariant(val id: String, val targetTriple: String, val objcopyOutputFormat: String = "elf64-x86-64", val objcopyBinaryArch: String = "x86-64")

private fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

val nativeVariants = listOf(
    NativeVariant("macosArm64", "arm64-apple-macos11", objcopyOutputFormat = "mach-o64-arm64", objcopyBinaryArch = "arm64"),
    NativeVariant("macosX64", "x86_64-apple-macos10.15", objcopyOutputFormat = "mach-o64-x86-64", objcopyBinaryArch = "x86-64"),
    NativeVariant("linuxX64", "x86_64-unknown-linux-gnu", objcopyOutputFormat = "elf64-x86-64", objcopyBinaryArch = "x86-64")
)

private fun NativeVariant.capId() = id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

val archiveTasksByVariant = nativeVariants.associate { variant ->
    val objDir = embedBuild.map { it.dir("obj/${variant.id}") }
    val libFile = embedBuild.map { it.file("${variant.id}/libbibles.a") }

    val objcopyTask = tasks.register<ObjcopyTarToObjectsTask>("objcopyTarToObjects${variant.capId()}") {
        dependsOn(ensureKonanToolchain)
        dependsOn(downloadKonanLlvm)
        dependsOn(tarBblpacks)
        tarInputDir.set(tarOutDir)
        objectOutputDir.set(objDir)
        objcopyExecutable.set(llvmObjcopyPath)
        outputFormat.set(variant.objcopyOutputFormat)
        binaryArch.set(variant.objcopyBinaryArch)
    }

    val compileTask = tasks.register<CompileCToObjectsTask>("compileCToObjects${variant.capId()}") {
        dependsOn(ensureKonanToolchain)
        dependsOn(downloadKonanLlvm)
        dependsOn(generateCFromTar)
        cInputDir.set(cOutDir)
        includeDirectory.set(includeOutDir)
        objectOutputDir.set(objDir)
        clangExecutable.set(clangPath)
        targetTriple.set(variant.targetTriple)
        kotlinVersion.set(kotlinVersionProvider)
    }

    val archiveTask = tasks.register<BuildEmbeddedArchiveTask>("buildEmbeddedArchive${variant.capId()}") {
        dependsOn(objcopyTask)
        dependsOn(compileTask)
        objectInputDir.set(objDir)
        archiveOutputFile.set(libFile)
        arExecutable.set(llvmArPath)
    }

    variant.id to archiveTask
}

// 5) Generate Kotlin binding + resource reader from generated header
val generateTarBindingsKt = tasks.register<GenerateTarBindingsTask>("generateTarBindingsKt") {
    headerFile.set(includeOutDir.map { it.file("generated_bibles.h") })
    dependsOn(generateCFromTar)
    tarInputDir.set(tarOutDir)
    outputDirectory.set(generatedKtDir)
}

// 6) Aggregate task
tasks.register("embedBblpacks") {
    dependsOn(generateTarBindingsKt)
    dependsOn(archiveTasksByVariant.values)
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    tasks.matching { it.name.startsWith("cinteropBibles") }.configureEach {
        if (bblpacksEmbedEnabled.get()) {
            dependsOn(tasks.named("embedBblpacks"))
            doFirst {
                logger.lifecycle("cinterop depends on header dir: ${includeOutDir.get().asFile.absolutePath}")
            }
        } else {
            logger.info("Not wiring $name -> embedBblpacks (IDE sync detected or bblpacks.embed=false)")
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
        if (bblpacksEmbedEnabled.get()) {
            dependsOn(tasks.named("embedBblpacks"))
        } else {
            logger.info("Not wiring $name -> embedBblpacks (IDE sync detected or bblpacks.embed=false)")
        }
    }
}
