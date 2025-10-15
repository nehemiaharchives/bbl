import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {

    // mobile app
    // bbl-kmp-android
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // bbl-kmp-ios
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // bbl-kmp-cli
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    //mingwX64()

    // desktop app (and windows jvm cli if windows native development has too much problem)
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.gnit.bible.test"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
