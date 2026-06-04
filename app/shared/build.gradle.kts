import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    androidLibrary {
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
