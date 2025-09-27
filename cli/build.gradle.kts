import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

apply(from = rootProject.file("gradle/bblpacks.gradle.kts"))

kotlin {
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    // windows, good to have, and build later, but for now commenting out
    jvm() // primarily for testing purposes,
    // in case windows native implementation has too much problems

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.clikt)
                implementation(libs.kotlinx.coroutines)
            }
        }
        val commonTest by getting

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }

        val macosArm64Main by getting { dependsOn(nativeMain) }
        val macosArm64Test by getting { dependsOn(nativeTest) }

        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }

        // Wire unique generated sources per target to avoid a single folder being
        // attached to multiple IDE modules (which causes IntelliJ to
        // attribute the file to linuxX64Main only).
        listOf(
            "linuxX64Main" to layout.buildDirectory.dir("generated/cli-linuxX64Main"),
            "macosX64Main" to layout.buildDirectory.dir("generated/cli-macosX64Main"),
            "macosArm64Main" to layout.buildDirectory.dir("generated/cli-macosArm64Main")
        ).forEach { (ss, dirProvider) ->
            kotlin.sourceSets.named(ss) {
                kotlin.srcDir(dirProvider)
            }
        }
    }

    targets.withType<KotlinNativeTarget>().all {
        val variantId = when (konanTarget) {
            KonanTarget.MACOS_ARM64 -> "macosArm64"
            KonanTarget.MACOS_X64 -> "macosX64"
            KonanTarget.LINUX_X64 -> "linuxX64"
            else -> return@all
        }

        compilations.getByName("main") {
            cinterops {
                val bibles by creating {
                    defFile(project.file("src/nativeMain/cinterop/bibles.def"))
                    val inc = project.layout.buildDirectory.dir("embedded/include").get().asFile
                    includeDirs(inc)
                    compilerOpts("-I${inc.absolutePath}")
                    //linkerOps is not supported by Kotlin/Native
                    //linkerOpts("-L${embedDir.absolutePath}", "-lbibles")
                }
            }
        }

        binaries {
            all {
                val embedDir = project.layout.buildDirectory.dir("embedded/$variantId").get().asFile
                linkerOpts("-L${embedDir.absolutePath}", "-lbibles")
                linkTaskProvider.configure { dependsOn("embedBblpacks") }
            }
            executable {
                entryPoint = "org.gnit.bible.cli.main"
            }
        }
    }
}

tasks.matching { it.name.startsWith("link") && it.name.contains("LinuxX64", ignoreCase = true) }.configureEach { dependsOn("embedBblpacks") }

// Ensure Kotlin compilation sees the file (independent of cinterop timing)
listOf(
    "compileKotlinLinuxX64" to ":cli:syncGeneratedTarBindingsLinuxX64",
    "compileKotlinMacosX64" to ":cli:syncGeneratedTarBindingsMacosX64",
    "compileKotlinMacosArm64" to ":cli:syncGeneratedTarBindingsMacosArm64"
).forEach { (tn, dep) ->
    tasks.matching { it.name == tn }.configureEach {
        dependsOn(dep)
    }
}

// Remove the previous shared dir wiring; we now wire per-target dirs above.

afterEvaluate {
    if (tasks.findByName("compileNativeMainKotlinMetadata") != null) {
        tasks.named("compileNativeMainKotlinMetadata") {
            dependsOn(tasks.named("generateTarBindingsKt"))
        }
    }
}

tasks.withType<CInteropProcess>().configureEach {
    if (name.contains("Bibles")) {
        val cacheDirPath = temporaryDir.resolve("clang-modules").absolutePath
        val sharedKonanCache = System.getenv("TMPDIR")?.let { File(it, "konan-module-cache") }

        settings.compilerOpts.removeAll { it.startsWith("-fmodules-cache-path=") }
        settings.compilerOpts.add("-fmodules-cache-path=$cacheDirPath")

        dependsOn("embedBblpacks")
        doFirst {
            sharedKonanCache?.takeIf { it.exists() }?.deleteRecursively()

            val cacheDir = File(cacheDirPath)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            cacheDir.mkdirs()

            val variantId = when {
                name.contains("LinuxX64", ignoreCase = true) -> "linuxX64"
                name.contains("MacosArm64", ignoreCase = true) -> "macosArm64"
                else -> "macosX64"
            }
            val lib = layout.buildDirectory.file("embedded/$variantId/libbibles.a").get().asFile
            println("DEBUG cinterop task: $name")
            println("DEBUG lib exists: ${lib.exists()} size=${lib.length()} path=${lib.absolutePath}")
            println("DEBUG embedded dir files:" + layout.buildDirectory.dir("embedded").get().asFile.listFiles()?.map { it.name })
            println("DEBUG expecting libraryPaths entry pointing to:" + layout.buildDirectory.dir("embedded").get().asFile.absolutePath)
            println("DEBUG compilerOpts module cache path: $cacheDirPath")
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.register("buildNativeRelease") {
    dependsOn(
        "linkReleaseExecutableMacosX64",
        "linkReleaseExecutableMacosArm64",
        "linkReleaseExecutableLinuxX64",
        "linkReleaseExecutableMingwX64"
    )
}
