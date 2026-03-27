import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    //alias(libs.plugins.androidLibrary)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {

    android {
        namespace = "org.gnit.bible.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    // bbl-kmp-ios
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // bbl-kmp-cli
    @Suppress("DEPRECATION")
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    //mingwX64()

    // desktop app (and windows jvm cli if windows native development has too much problem)
    jvm()
    
    sourceSets {
        val commonMain by getting
        val commonTest by getting

        // New aggregated native source set so we can place shared native code in src/nativeMain
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        // Optional aggregated test source set if needed later (src/nativeTest)
        val nativeTest by creating {
            dependsOn(commonTest)
        }

        // Make every native target's main source set depend on nativeMain
        val iosMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.clientDarwin)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val iosX64Main by getting { dependsOn(iosMain) }

        val posixMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.clientCurl)
                implementation(libs.okio)
            }
        }
        val macosX64Main by getting { dependsOn(posixMain) }
        val macosArm64Main by getting { dependsOn(posixMain) }
        val linuxX64Main by getting { dependsOn(posixMain) }
        //val mingwX64Main by getting { dependsOn(nativeMain) }

        // (Tests) hook them to nativeTest if/when created
        val iosTest by creating { dependsOn(nativeTest) }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
        val iosX64Test by getting { dependsOn(iosTest) }

        val posixTest by creating {
            dependsOn(nativeTest)
            dependencies {
                implementation(libs.okio.fakefs)
            }
        }
        val macosX64Test by getting { dependsOn(posixTest) }
        val macosArm64Test by getting { dependsOn(posixTest) }
        val linuxX64Test by getting { dependsOn(posixTest) }
        //val mingwX64Test by getting { dependsOn(nativeTest) }

        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.lucene.kmp.core)
            implementation(libs.lucene.kmp.queryparser)
            implementation(libs.lucene.kmp.analysis.common)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientCio)
            implementation(libs.okio)
            implementation(libs.kmpio)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlin.logging)
        }
        commonTest.dependencies {
            implementation(projects.testFramework)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.clientMock)
            implementation(libs.okio.fakefs)
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.clientOkhttp)
            }
        }

        /*val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.testExt.junit)
            }
        }*/
    }
}

/*android {
    namespace = "org.gnit.bible.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    *//*compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }*//*
    *//*defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }*//*
    *//*testOptions {
        unitTests {
            isIncludeAndroidResources = true

            all {
                it.filter {
                    excludeTestsMatching("org.gnit.bible.DownloaderTest")
                }
            }
        }
    }*//*
}*/
