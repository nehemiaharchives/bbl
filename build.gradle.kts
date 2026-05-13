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

val bblInstallLinuxFilesPath = "bbl_install_linux/files"
val bblInstallWindowsFilesPath = "bbl_install_windows/files"

val stageBblInstallLinuxFixtures = tasks.register<Copy>("stageBblInstallLinuxFixtures") {
    dependsOn(":cli:linkReleaseExecutableLinuxX64")

    into(layout.projectDirectory.dir(bblInstallLinuxFilesPath))
    from(project(":cli:core").layout.buildDirectory.dir("bin/linuxX64/releaseExecutable")) {
        include("bbl.kexe")
        rename("bbl\\.kexe", "bbl")
    }

    listOf(
        ":cli:search:common" to "bbl-search-common",
        ":cli:search:extra" to "bbl-search-extra",
        ":cli:search:kuromoji" to "bbl-search-kuromoji",
        ":cli:search:morfologik" to "bbl-search-morfologik",
        ":cli:search:nori" to "bbl-search-nori",
        ":cli:search:smartcn" to "bbl-search-smartcn",
    ).forEach { (projectPath, binaryName) ->
        from(project(projectPath).layout.buildDirectory.dir("bin/linuxX64/releaseExecutable")) {
            include("$binaryName.kexe")
            rename("$binaryName\\.kexe", binaryName)
        }
    }

    from(project(":server").layout.projectDirectory.dir("src/main/resources/files/bblpacks")) {
        include("*.zip")
    }
}

val stageBblInstallWindowsFixtures = tasks.register<Copy>("stageBblInstallWindowsFixtures") {
    dependsOn(":cli:linkReleaseExecutableMingwX64")

    into(layout.projectDirectory.dir(bblInstallWindowsFilesPath))

    from(project(":cli:core").layout.buildDirectory.dir("bin/mingwX64/releaseExecutable")) {
        include("bbl.exe")
    }

    listOf(
        ":cli:search:common" to "bbl-search-common",
        ":cli:search:extra" to "bbl-search-extra",
        ":cli:search:kuromoji" to "bbl-search-kuromoji",
        ":cli:search:morfologik" to "bbl-search-morfologik",
        ":cli:search:nori" to "bbl-search-nori",
        ":cli:search:smartcn" to "bbl-search-smartcn",
    ).forEach { (projectPath, binaryName) ->
        from(project(projectPath).layout.buildDirectory.dir("bin/mingwX64/releaseExecutable")) {
            include("$binaryName.exe")
        }
    }

    from(project(":server").layout.projectDirectory.dir("src/main/resources/files/bblpacks")) {
        include("*.zip")
    }
}

val stageBblInstallFixtures = tasks.register("stageBblInstallFixtures") {
    dependsOn(stageBblInstallLinuxFixtures, stageBblInstallWindowsFixtures)
}
