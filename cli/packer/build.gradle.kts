import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

apply(from = rootProject.file("gradle/bblpacks.gradle.kts"))

// Keep embed pipeline out of IDE sync task graphs.
val isIdeaSyncActive: Provider<Boolean> = listOf(
    "idea.sync.active",
    "android.injected.invoked.from.ide",
    "idea.active"
).map { key ->
    providers.systemProperty(key).map { it.equals("true", ignoreCase = true) }.orElse(false)
}.reduce { acc, next ->
    acc.zip(next) { a, b -> a || b }
}

val bblpacksEmbedRequested: Provider<Boolean> = providers.gradleProperty("bblpacks.embed")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(true)

val bblpacksEmbedEnabled: Provider<Boolean> = bblpacksEmbedRequested.zip(isIdeaSyncActive) { requested, isSync ->
    requested && !isSync
}

@Suppress("UNCHECKED_CAST")
val bblpacksCompilerOptsProvider = project.extensions.extraProperties.get("bblpacksCompilerOpts") as Provider<List<String>>

kotlin {
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    // windows, good to have, and build later, but for now commenting out
    jvm() // primarily for testing purposes,
    // in case windows native implementation has too much problems
    jvmToolchain(24)

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.clikt)
                implementation(libs.okio)
                implementation(libs.kmpio)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlin.logging)
                implementation(libs.lucene.kmp.core)
                implementation(libs.lucene.kmp.analysis.common)
                implementation(libs.lucene.kmp.analysis.morfologik)
                implementation(libs.lucene.kmp.analysis.smartcn)
                implementation(libs.lucene.kmp.analysis.nori)
                implementation(libs.lucene.kmp.analysis.kuromoji)
                implementation(libs.lucene.kmp.analysis.extra)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.testFramework)
                implementation(libs.kotlin.test)
                implementation(libs.ktor.clientMock)
                implementation(libs.okio.fakefs)
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        macosX64Main.get().dependsOn(nativeMain)
        macosX64Test.get().dependsOn(nativeTest)

        macosArm64Main.get().dependsOn(nativeMain)
        macosArm64Test.get().dependsOn(nativeTest)

        linuxX64Main.get().dependsOn(nativeMain)
        linuxX64Test.get().dependsOn(nativeTest)

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

        jvmMain.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonMain/composeResources").asFile
        )

        jvmTest.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonTest/composeResources").asFile
        )
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
                creating {
                    defFile(project.file("src/nativeMain/cinterop/bibles.def"))
                    val inc = project.layout.buildDirectory.dir("embedded/include").get().asFile
                    includeDirs(inc)
                    compilerOpts(*bblpacksCompilerOptsProvider.get().toTypedArray())
                    //linkerOps is not supported by Kotlin/Native
                    //linkerOpts("-L${embedDir.absolutePath}", "-lbibles")
                }
            }
        }

        binaries {
            all {
                val embedDir = project.layout.buildDirectory.dir("embedded/$variantId").get().asFile
                linkerOpts("-L${embedDir.absolutePath}", "-lbibles")
                linkTaskProvider.configure {
                    if (bblpacksEmbedEnabled.get()) {
                        dependsOn("embedBblpacks")
                    }
                }
            }
            executable {
                entryPoint = "org.gnit.bible.cli.main"
                baseName = "bbl"
            }
        }
    }
}

tasks.matching { it.name.startsWith("link") && it.name.contains("LinuxX64", ignoreCase = true) }.configureEach {
    if (bblpacksEmbedEnabled.get()) {
        dependsOn("embedBblpacks")
    }
}

// Ensure Kotlin compilation sees the file (independent of cinterop timing)
// (syncGeneratedTarBindings tasks already depend on generateTarBindingsKt -> tarBblpacks,
// so we must also gate these for IDE sync).
listOf(
    "compileKotlinLinuxX64" to ":cli:syncGeneratedTarBindingsLinuxX64",
    "compileKotlinMacosX64" to ":cli:syncGeneratedTarBindingsMacosX64",
    "compileKotlinMacosArm64" to ":cli:syncGeneratedTarBindingsMacosArm64"
).forEach { (tn, dep) ->
    tasks.matching { it.name == tn }.configureEach {
        if (bblpacksEmbedEnabled.get()) {
            dependsOn(dep)
        }
    }
}

// Remove the previous shared dir wiring; we now wire per-target dirs above.

afterEvaluate {
    if (tasks.findByName("compileNativeMainKotlinMetadata") != null) {
        tasks.named("compileNativeMainKotlinMetadata") {
            if (bblpacksEmbedEnabled.get()) {
                dependsOn(tasks.named("generateTarBindingsKt"))
            }
        }
    }
}

tasks.withType<CInteropProcess>().configureEach {
    if (name.contains("Bibles")) {
        val cacheDirPath = temporaryDir.resolve("clang-modules").absolutePath
        val sharedKonanCache = System.getenv("TMPDIR")?.let { File(it, "konan-module-cache") }

        settings.compilerOpts.removeAll { it.startsWith("-fmodules-cache-path=") }
        settings.compilerOpts.add("-fmodules-cache-path=$cacheDirPath")

        if (bblpacksEmbedEnabled.get()) {
            dependsOn("embedBblpacks")
        }
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
            logger.info("DEBUG cinterop task: $name")
            logger.info("DEBUG lib exists: ${lib.exists()} size=${lib.length()} path=${lib.absolutePath}")
            logger.info("DEBUG embedded dir files:" + layout.buildDirectory.dir("embedded").get().asFile.listFiles()?.map { it.name })
            logger.info("DEBUG expecting libraryPaths entry pointing to:" + layout.buildDirectory.dir("embedded").get().asFile.absolutePath)
            logger.info("DEBUG compilerOpts module cache path: $cacheDirPath")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions{
        optIn.addAll(
            "kotlin.ExperimentalStdlibApi",
        )
        //suppressWarnings = true
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )
    }
}
