import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    macosX64() // intel mac
    macosArm64() // m1/2/3/4 mac
    linuxX64()
    // windows, good to have, and build later, but for now commenting out
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
                implementation(libs.kmpio)
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

        macosX64Main.get().dependsOn(nativeMain)
        macosX64Test.get().dependsOn(nativeTest)

        macosArm64Main.get().dependsOn(nativeMain)
        macosArm64Test.get().dependsOn(nativeTest)

        linuxX64Main.get().dependsOn(nativeMain)
        linuxX64Test.get().dependsOn(nativeTest)

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
