@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    packageOfResClass = "org.gnit.bible.app"
    generateResClass = auto
    customDirectory("commonTest", layout.buildDirectory.dir("generated/composeTestPackResources"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries {
            /*
             * Temporary Kotlin/Native cache workaround for CI:
             * Xcode 16.4 iOS simulator test linking fails in cached Compose/Skiko artifacts
             * with UIViewLayoutRegion/UIUtilities symbols. Once that CI issue is resolved,
             * remove this all { disableNativeCache(...) } block to re-enable native caches.
             */
            all {
                disableNativeCache(
                    DisableCacheInKotlinVersion.`2_4_0`,
                    "Work around CI iOS simulator link failure in cached Compose/Skiko Kotlin/Native artifacts."
                )
            }

            framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    jvm()

    android {
        namespace = "org.gnit.bible.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        val syncComposeTestPacks = tasks.register<Sync>("syncComposeTestPacks") {
            from(rootProject.layout.projectDirectory.dir("resources/bblpacks"))
            into(layout.buildDirectory.dir("generated/composeTestPackResources/files/bblpackzips"))
            include("*.zip")
        }

        tasks.named("copyNonXmlValueResourcesForCommonTest") {
            dependsOn(syncComposeTestPacks)
        }

        val syncAndroidDeviceTestPacks = tasks.register<Sync>("syncAndroidDeviceTestPacks") {
            from(rootProject.layout.projectDirectory.dir("resources/bblpacks"))
            into(layout.buildDirectory.dir("androidDeviceTestAssets/bblpacks"))
            include(
                "abtag.zip", "ayt.zip", "irvben.zip", "irvguj.zip", "irvhin.zip",
                "irvmar.zip", "irvtam.zip", "irvtel.zip", "irvurd.zip", "kttv.zip",
                "npiulb.zip", "th1971.zip"
            )
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.slf4j.android)
        }
        commonMain {
            dependencies {
                api(projects.core)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.compose.material.iconsCore)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.logging)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.locale)
                implementation(libs.okio)
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
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        iosMain {
            dependsOn(commonMain.get())
            iosArm64Main.get().dependsOn(this)
            iosSimulatorArm64Main.get().dependsOn(this)
        }
        iosTest {
            dependsOn(commonTest)
            iosArm64Test.get().dependsOn(this)
            iosSimulatorArm64Test.get().dependsOn(this)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(24))
        }
    )
}

val iosAppXcodeProject = rootProject.layout.projectDirectory.dir("app/iosApp/iosApp.xcodeproj")

gradle.taskGraph.whenReady {
    tasks.findByName("convertPbxprojToJson")?.let { task ->
        val pbxprojFile = task.javaClass.methods
            .first { method -> method.name == "getPbxprojFile" && method.parameterCount == 0 }
            .invoke(task) as org.gradle.api.file.RegularFileProperty
        pbxprojFile.set(iosAppXcodeProject.file("project.pbxproj"))
    }

    tasks.findByName("checkXcodeProjectConfiguration")?.let { task ->
        val xcodeProjectPath = task.javaClass.methods
            .first { method -> method.name == "getXcodeProjectPath" && method.parameterCount == 0 }
            .invoke(task) as org.gradle.api.file.DirectoryProperty
        xcodeProjectPath.set(iosAppXcodeProject)
    }
}
