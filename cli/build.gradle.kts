import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
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
    }

    targets.withType<KotlinNativeTarget>().all {

        compilations.getByName("main") {
            cinterops {
                val bibles by creating {
                    defFile(project.file("src/nativeMain/cinterop/bibles.def"))
                    val embedDir = project.layout.buildDirectory.dir("embedded").get().asFile
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
                val embedDir = project.layout.buildDirectory.dir("embedded").get().asFile
                linkerOpts("-L${embedDir.absolutePath}", "-lbibles")
                linkTaskProvider.get().dependsOn("embedBblpacks")
            }
            executable {
                entryPoint = "org.gnit.bible.cli.main"
            }
        }
    }
}

tasks.matching { it.name.startsWith("link") && it.name.contains("LinuxX64", ignoreCase = true) }.configureEach { dependsOn("embedBblpacks") }

// Ensure generated code directory is on the nativeMain source set
kotlin.sourceSets.getByName("nativeMain") {
    kotlin.srcDir(layout.buildDirectory.dir("generated/cli"))
}

// Ensure Kotlin compilation sees the file (independent of cinterop timing)
listOf(
    "compileKotlinLinuxX64",
    "compileKotlinMacosX64",
    "compileKotlinMacosArm64"
).forEach { tn ->
    tasks.matching { it.name == tn }.configureEach {
        dependsOn(":cli:generateTarBindingsKt")
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

            val lib = layout.buildDirectory.file("embedded/libbibles.a").get().asFile
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
