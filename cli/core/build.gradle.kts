import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    @Suppress("DEPRECATION")
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    mingwX64() // windows native
    jvm() // primarily for testing purposes,
    // in case windows native implementation has too much problems
    jvmToolchain(24)

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.cli.search.common)
                implementation(libs.clikt)
                implementation(libs.okio)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlin.logging)
                implementation(libs.lucene.kmp.core)
                implementation(libs.lucene.kmp.analysis.common)
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

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        val posixMain by creating { dependsOn(nativeMain) }
        val posixTest by creating { dependsOn(nativeTest) }

        macosX64Main.get().dependsOn(posixMain)
        macosX64Test.get().dependsOn(posixTest)

        macosArm64Main.get().dependsOn(posixMain)
        macosArm64Test.get().dependsOn(posixTest)

        linuxX64Main.get().dependsOn(posixMain)
        linuxX64Test.get().dependsOn(posixTest)

        mingwX64Main.get().dependsOn(nativeMain)
        mingwX64Test.get().dependsOn(nativeTest)

        // Keep JVM resource wiring (used by some JVM tests/tools).
        jvmMain.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonMain/composeResources").asFile
        )

        jvmTest.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonTest/composeResources").asFile
        )
    }

    targets.withType<KotlinNativeTarget>().all {
        binaries {
            executable {
                entryPoint = "org.gnit.bible.cli.cliMain"
                baseName = "bbl"
            }
        }
    }
}
