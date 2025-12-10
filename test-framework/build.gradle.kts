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

        val commonMain by getting
        val nativeMain by creating { dependsOn(commonMain) }

        val iosMain by creating { dependsOn(nativeMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val iosX64Main by getting { dependsOn(iosMain) }

        val posixMain by creating { dependsOn(nativeMain) }
        val macosX64Main by getting { dependsOn(posixMain) }
        val macosArm64Main by getting { dependsOn(posixMain) }
        val linuxX64Main by getting { dependsOn(posixMain) }

        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.clientMock)
            implementation(libs.multiplatform.settings)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.testExt.junit)
        }
    }
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
