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
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.clikt)
                implementation(libs.okio)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlin.logging)
                implementation(libs.lucene.kmp.core)
                implementation(libs.lucene.kmp.analysis.morfologik)
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

        mingwX64Main.get().dependsOn(nativeMain)
        mingwX64Test.get().dependsOn(nativeTest)

        targets.withType<KotlinNativeTarget>().all {
            binaries {
                executable {
                    entryPoint = "org.gnit.bible.cli.main"
                    baseName = "bbl-search-morfologik"
                }
            }
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
