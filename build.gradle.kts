import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinParcelize) apply false
}

subprojects {
    tasks.withType<KotlinNativeTest>().configureEach {
        environment("SIMCTL_CHILD_BBL_KMP_ROOT", rootProject.projectDir.absolutePath)
    }

    if (path.startsWith(":cli")) {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinNativeTarget>().configureEach {
                    if (name != "mingwX64") {
                        binaries.getTest("", NativeBuildType.DEBUG).apply {
                            @Suppress("DEPRECATION")
                            @OptIn(KotlinNativeCacheApi::class)
                            disableNativeCache(
                                DisableCacheInKotlinVersion.`2_3_20`,
                                "Clikt native caches produce duplicate symbols in cli test binaries"
                            )
                        }
                        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Executable>().configureEach {
                            @Suppress("DEPRECATION")
                            @OptIn(KotlinNativeCacheApi::class)
                            disableNativeCache(
                                DisableCacheInKotlinVersion.`2_3_20`,
                                "Clikt native caches produce duplicate symbols in cli executable binaries"
                            )
                        }
                    }
                }
            }
        }
    }

        tasks.matching { it.name in setOf("compileKotlinJvm", "compileTestKotlinJvm") }
        .configureEach {
            group = LifecycleBasePlugin.BUILD_GROUP // "build"
        }
}

data class BblInstallPlatform(
    val id: String,
    val taskNamePart: String,
    val nativeTargetName: String,
    val linkTaskSuffix: String,
    val executableSuffix: String,
)

data class BblInstallBinary(
    val id: String,
    val taskNamePart: String,
    val projectPath: String,
    val binaryName: String,
    val includePacks: Boolean = false,
)

val bblInstallPlatforms = listOf(
    BblInstallPlatform("linux", "Linux", "linuxX64", "LinuxX64", ".kexe"),
    BblInstallPlatform("macosArm64", "MacosArm64", "macosArm64", "MacosArm64", ".kexe"),
    BblInstallPlatform("macosX64", "MacosX64", "macosX64", "MacosX64", ".kexe"),
    BblInstallPlatform("windows", "Windows", "mingwX64", "MingwX64", ".exe"),
)

val bblInstallBinaries = listOf(
    BblInstallBinary("cli-core", "CliCore", ":cli:core", "bbl", includePacks = true),
    BblInstallBinary("cli-search-common", "CliSearchCommon", ":cli:search:common", "bbl-search-common"),
    BblInstallBinary("cli-search-extra", "CliSearchExtra", ":cli:search:extra", "bbl-search-extra"),
    BblInstallBinary("cli-search-kuromoji", "CliSearchKuromoji", ":cli:search:kuromoji", "bbl-search-kuromoji"),
    BblInstallBinary("cli-search-morfologik", "CliSearchMorfologik", ":cli:search:morfologik", "bbl-search-morfologik"),
    BblInstallBinary("cli-search-nori", "CliSearchNori", ":cli:search:nori", "bbl-search-nori"),
    BblInstallBinary("cli-search-smartcn", "CliSearchSmartcn", ":cli:search:smartcn", "bbl-search-smartcn"),
)

val bblCliVersionProvider = providers.fileContents(
    layout.projectDirectory.file("shared/src/commonMain/kotlin/org/gnit/bible/cli/BblVersion.kt")
).asText.map { source ->
    Regex("""const val bblCliVersion = "([^"]+)"""")
        .find(source)
        ?.groupValues
        ?.get(1)
        ?: error("Unable to read bblCliVersion from BblVersion.kt")
}

val bblArtifactCompatibilityVersionProvider = providers.fileContents(
    layout.projectDirectory.file("shared/src/commonMain/kotlin/org/gnit/bible/cli/BblVersion.kt")
).asText.map { source ->
    Regex("""const val bblArtifactCompatibilityVersion = "([^"]+)"""")
        .find(source)
        ?.groupValues
        ?.get(1)
        ?: error("Unable to read bblArtifactCompatibilityVersion from BblVersion.kt")
}

val verifyServerBblPackVersions = tasks.register("verifyServerBblPackVersions") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verify server bblpack zip manifests match bblArtifactCompatibilityVersion."

    inputs.file(layout.projectDirectory.file("shared/src/commonMain/kotlin/org/gnit/bible/cli/BblVersion.kt"))
    inputs.property("bblArtifactCompatibilityVersion", bblArtifactCompatibilityVersionProvider)
    inputs.files(fileTree(layout.projectDirectory.dir("server/src/main/resources/files/bblpacks")) {
        include("*.zip")
    })

    doLast {
        val expectedVersion = bblArtifactCompatibilityVersionProvider.get()
        val packDir = layout.projectDirectory.dir("server/src/main/resources/files/bblpacks").asFile
        val zipFiles = packDir.listFiles { file -> file.isFile && file.extension == "zip" }
            ?.sortedBy { it.name }
            .orEmpty()

        require(zipFiles.isNotEmpty()) {
            "No server bblpack zips found in ${packDir.absolutePath}"
        }

        val versionRegex = Regex(""""bblArtifactCompatibilityVersion"\s*:\s*"([^"]+)"""")
        val failures = mutableListOf<String>()

        zipFiles.forEach { zipFile ->
            java.util.zip.ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.entries().asSequence()
                    .firstOrNull { it.name.endsWith(".0.manifest.json") }
                if (manifestEntry == null) {
                    failures.add("${zipFile.name}: missing .0.manifest.json")
                    return@use
                }

                val manifestJson = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                val actualVersion = versionRegex.find(manifestJson)?.groupValues?.get(1)
                if (actualVersion != expectedVersion) {
                    failures.add("${zipFile.name}: bblArtifactCompatibilityVersion ${actualVersion ?: "<missing>"} != $expectedVersion")
                }
            }
        }

        if (failures.isNotEmpty()) {
            throw GradleException(
                "Server bblpack versions are not compatible with bbl artifact compatibility version $expectedVersion:\n" +
                    failures.joinToString(separator = "\n") { " - $it" } +
                    "\nRegenerate packs with bbl pack before staging fixtures or publishing."
            )
        }
    }
}

val bblInstallCommonFixtureDirectory = layout.buildDirectory.dir("bblInstallFixtures/common")
val bblInstallVersionFixtureFile = bblInstallCommonFixtureDirectory.map { it.file("version.txt") }
val bblInstallArtifactCompatibilityVersionFixtureFile =
    bblInstallCommonFixtureDirectory.map { it.file("artifact_compatibility_version.txt") }
val stageBblInstallVersionFixture = tasks.register("stageBblInstallVersionFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage expected bbl version files for bbl_install Kitchen tests and local cookbook runs."

    inputs.property("bblCliVersion", bblCliVersionProvider)
    inputs.property("bblArtifactCompatibilityVersion", bblArtifactCompatibilityVersionProvider)
    outputs.files(bblInstallVersionFixtureFile, bblInstallArtifactCompatibilityVersionFixtureFile)

    doLast {
        val versionFile = bblInstallVersionFixtureFile.get().asFile
        versionFile.parentFile.mkdirs()
        versionFile.writeText("${bblCliVersionProvider.get()}\n")

        val artifactCompatibilityVersionFile = bblInstallArtifactCompatibilityVersionFixtureFile.get().asFile
        artifactCompatibilityVersionFile.parentFile.mkdirs()
        artifactCompatibilityVersionFile.writeText("${bblArtifactCompatibilityVersionProvider.get()}\n")

        val cookbookVersionFile = layout.projectDirectory.file("bbl_install/files/version.txt").asFile
        cookbookVersionFile.parentFile.mkdirs()
        cookbookVersionFile.writeText(versionFile.readText())

        val cookbookArtifactCompatibilityVersionFile =
            layout.projectDirectory.file("bbl_install/files/artifact_compatibility_version.txt").asFile
        cookbookArtifactCompatibilityVersionFile.parentFile.mkdirs()
        cookbookArtifactCompatibilityVersionFile.writeText(artifactCompatibilityVersionFile.readText())
    }
}

val stageBblInstallFixtureTasks = bblInstallPlatforms.flatMap { platform ->
    bblInstallBinaries.map { binary ->
        val taskName = "stageBblInstall${platform.taskNamePart}${binary.taskNamePart}Fixture"
        val executableFileName = binary.binaryName + platform.executableSuffix
        tasks.register<Copy>(taskName) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Stage ${binary.id} ${platform.id} fixture files for bbl_install Kitchen tests."

            dependsOn("${binary.projectPath}:linkReleaseExecutable${platform.linkTaskSuffix}")
            dependsOn(stageBblInstallVersionFixture)

            into(layout.buildDirectory.dir("bblInstallFixtures/${platform.id}/${binary.id}"))
            from(project(binary.projectPath).layout.buildDirectory.dir("bin/${platform.nativeTargetName}/releaseExecutable")) {
                include(executableFileName)
                rename(Regex.escape(executableFileName), binary.binaryName + if (platform.id == "windows") ".exe" else "")
            }

            if (binary.includePacks) {
                dependsOn(verifyServerBblPackVersions)
                from(project(":server").layout.projectDirectory.dir("src/main/resources/files/bblpacks")) {
                    include("*.zip")
                }
            }
        }
    }
}

val stageBblInstallLinuxFixtures = tasks.register("stageBblInstallLinuxFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { it.name.contains("Linux") })
}

fun Copy.prepareBblInstallCookbookFiles(platform: BblInstallPlatform) {
    val platformFixtureTasks = stageBblInstallFixtureTasks.filter { it.name.contains(platform.taskNamePart) }
    dependsOn(platformFixtureTasks)
    dependsOn(stageBblInstallVersionFixture)

    into(layout.projectDirectory.dir("bbl_install/files"))
    from(platformFixtureTasks.map { layout.buildDirectory.dir("bblInstallFixtures/${platform.id}") })
    from(bblInstallCommonFixtureDirectory)
    include("**/*")
    eachFile {
        relativePath = RelativePath(true, name)
    }
    includeEmptyDirs = false
    outputs.upToDateWhen { false }

    doFirst {
        delete(fileTree(layout.projectDirectory.dir("bbl_install/files")) {
            exclude("README.md")
        })
    }
}

// before test kitchen, run this task in local dev linux
tasks.register<Copy>("stageBblInstallLinuxCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
}

val stageBblInstallWindowsFixtures = tasks.register("stageBblInstallWindowsFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { it.name.contains("Windows") })
}

// before test kitchen, run this task in local dev windows
tasks.register<Copy>("stageBblInstallWindowsCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "windows" })
}

val stageBblInstallMacosFixtures = tasks.register("stageBblInstallMacosFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { it.name.contains("Macos") })
}

// before test kitchen, run this task in local dev macos (arm64)
tasks.register<Copy>("stageBblInstallMacosArm64CliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS Arm64 CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "macosArm64" })
}

// before test kitchen, run this task in local dev macos (x64)
tasks.register<Copy>("stageBblInstallMacosX64CliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS X64 CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "macosX64" })
}

tasks.register("stageBblInstallFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks)
}

tasks.register("stageBblInstallCommonFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage Linux and Windows fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { !it.name.contains("Macos") })
}
