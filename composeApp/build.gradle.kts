import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {

    android {
        namespace = "org.gnit.bible.cmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    
    listOf(
        iosArm64(),

        // uncomment this when you are working on M1/2/3/4 chip on Mac to use simulator
        iosSimulatorArm64(),

        // uncomment this when you are working on intel mac to use simulator
        iosX64(),

    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val iosMain by creating {
            dependsOn(commonMain)
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val iosX64Main by getting { dependsOn(iosMain) }

        val iosTest by creating {
            dependsOn(commonTest)
        }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
        val iosX64Test by getting { dependsOn(iosTest) }

        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
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
            implementation(projects.shared)
        }

        commonTest.dependencies {
            implementation(projects.testFramework)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.clientMock)
        }

        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.slf4j.android)
        }

        val androidHostTest by getting {
            //dependsOn(commonTest)
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.androidx.test.core)
                implementation(libs.robolectric)
            }
        }

        named("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.test.core)
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "org.gnit.bible.cmp"
    generateResClass = auto
}

compose.desktop {
    application {
        mainClass = "org.gnit.bible.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.gnit.bible"
            packageVersion = "1.0.0"
        }
    }
}
