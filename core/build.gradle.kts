import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    @Suppress("DEPRECATION")
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()
    jvm()

    androidLibrary {
        namespace = "org.gnit.bible.core"
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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.logging)
                implementation(libs.okio)
                implementation(libs.multiplatform.settings)
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.lucene.kmp.core)
                implementation(libs.lucene.kmp.queryparser)
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
                implementation(libs.okio.fakefs)
                implementation(libs.ktor.clientMock)
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        val posixMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.clientCurl)
                implementation(libs.okio)
            }
        }
        val posixTest by creating { dependsOn(nativeTest) }

        macosX64Main.get().dependsOn(posixMain)
        macosX64Test.get().dependsOn(posixTest)
        macosArm64Main.get().dependsOn(posixMain)
        macosArm64Test.get().dependsOn(posixTest)
        linuxX64Main.get().dependsOn(posixMain)
        linuxX64Test.get().dependsOn(posixTest)
        mingwX64Main.get().dependsOn(nativeMain)
        mingwX64Test.get().dependsOn(nativeTest)

        jvmMain.get().dependsOn(commonMain)
        jvmTest.get().dependsOn(commonTest)

        val iosMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.clientDarwin)
            }
        }
        val iosTest by creating { dependsOn(nativeTest) }
        iosArm64Main.get().dependsOn(iosMain)
        iosArm64Test.get().dependsOn(iosTest)
        iosSimulatorArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Test.get().dependsOn(iosTest)
        iosX64Main.get().dependsOn(iosMain)
        iosX64Test.get().dependsOn(iosTest)
    }
}
