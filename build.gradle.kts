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
    BblInstallPlatform("macos", "Macos", "macosArm64", "MacosArm64", ".kexe"),
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

val stageBblInstallFixtureTasks = bblInstallPlatforms.flatMap { platform ->
    bblInstallBinaries.map { binary ->
        val taskName = "stageBblInstall${platform.taskNamePart}${binary.taskNamePart}Fixture"
        val executableFileName = binary.binaryName + platform.executableSuffix
        tasks.register<Copy>(taskName) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Stage ${binary.id} ${platform.id} fixture files for bbl_install Kitchen tests."

            dependsOn("${binary.projectPath}:linkReleaseExecutable${platform.linkTaskSuffix}")

            into(layout.buildDirectory.dir("bblInstallFixtures/${platform.id}/${binary.id}"))
            from(project(binary.projectPath).layout.buildDirectory.dir("bin/${platform.nativeTargetName}/releaseExecutable")) {
                include(executableFileName)
                rename(Regex.escape(executableFileName), binary.binaryName + if (platform.id == "windows") ".exe" else "")
            }

            if (binary.includePacks) {
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

val stageBblInstallWindowsFixtures = tasks.register("stageBblInstallWindowsFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { it.name.contains("Windows") })
}

val stageBblInstallMacosFixtures = tasks.register("stageBblInstallMacosFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { it.name.contains("Macos") })
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
