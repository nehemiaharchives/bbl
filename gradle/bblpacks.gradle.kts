import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

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

// Central LLVM major version selector used for tool discovery and gating
val llvmVersion: Int = 19

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

fun findMacClang(): String {
    val viaXcrun = runCatching {
        val proc = ProcessBuilder("xcrun", "--find", "clang").redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().use { it.readText() }.trim()
        proc.waitFor()
        output.takeIf { it.isNotBlank() && File(it).canExecute() }
    }.getOrNull()

    val path = viaXcrun
        ?: File("/usr/bin/clang").takeIf { it.canExecute() }?.absolutePath
        ?: throw GradleException("Cannot locate Xcode clang. Ensure Xcode Command Line Tools are installed.")

    toolMajorVersion(path)
    logger.lifecycle("$path (Xcode clang) will be used")
    return path
}

fun findTool(toolName: String, requiredMajorVersion: Int? = null): String {
    System.getenv(toolName.uppercase())?.let { p ->
        val f = File(p); if (f.canExecute()) return f.absolutePath
    }
    val os = OperatingSystem.current()
    val exeName = if (os.isWindows) "$toolName.exe" else toolName

    val needsVersioned = requiredMajorVersion != null
    // From PATH: only accept tool when major version matches requiredMajorVersion (if provided)
    System.getenv("PATH")?.split(File.pathSeparatorChar)?.forEach { dir ->
        val f = File(dir, exeName)
        if (f.canExecute()) {
            if (needsVersioned) {
                val v = toolMajorVersion(f.absolutePath)
                if (v == requiredMajorVersion) {
                    logger.lifecycle("${f.absolutePath} found in PATH will be used")
                    return f.absolutePath
                }
            } else {
                return f.absolutePath
            }
        }
    }

    // Homebrew llvm@<version> explicit paths (Intel and Apple Silicon)
    if (needsVersioned) {
        val brewSuffix = "llvm@${requiredMajorVersion}"
        val brewFallbacks = listOf(
            "/usr/local/opt/${brewSuffix}/bin/$toolName",
            "/opt/homebrew/opt/${brewSuffix}/bin/$toolName"
        )
        for (p in brewFallbacks) {
            val f = File(p)
            if (f.canExecute()) {
                logger.lifecycle("${f.absolutePath} installed via Homebrew will be used")
                return f.absolutePath
            }
        }
    }

    // Kotlin/Native Konan toolchains: enforce llvmVersion for clang/llvm-ar
    val home = System.getProperty("user.home")
    val konanDeps = File(home, ".konan/dependencies")
    val candidates = konanDeps.listFiles { f -> f.isDirectory && f.name.startsWith("llvm-") }?.sortedByDescending { it.name } ?: emptyList()
    for (dir in candidates) {
        val f = File(dir, "bin/$exeName")
        if (f.canExecute()) {
            if (needsVersioned) {
                val v = toolMajorVersion(f.absolutePath)
                if (v == requiredMajorVersion) return f.absolutePath else continue
            } else {
                logger.lifecycle("${f.absolutePath} found in .konan will be used")
                return f.absolutePath
            }
        }
    }

    val reason = requiredMajorVersion?.let { "Require major version $it for $toolName." } ?: ""
    val brewTip = requiredMajorVersion?.let { "run 'brew install llvm@${it}'" } ?: "ensure it is on PATH"
    throw GradleException("Cannot find $toolName. $reason Set env $toolName or $brewTip.")
}

val clangPath = providers.provider {
    if (OperatingSystem.current().isMacOsX) findMacClang() else findTool("clang", llvmVersion)
}
val llvmArPath = providers.provider { findTool("llvm-ar", llvmVersion) }

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

// 3) Compile C → .o
val compileCToObjects = tasks.register("compileCToObjects") {
    dependsOn(generateCFromTar)
    inputs.dir(cOutDir)
    outputs.dir(oOutDir)

    doLast {
        val cDir = cOutDir.get().asFile
        val objDir = oOutDir.get().asFile.also { it.mkdirs() }
        val clang = clangPath.get()

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
        logger.lifecycle("Built ${lib.absolutePath}")
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
