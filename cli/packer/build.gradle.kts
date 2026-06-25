import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    @Suppress("DEPRECATION")
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    jvm()

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.core)
                implementation(projects.cli.shared)
                implementation(libs.clikt)
                implementation(libs.okio)
                implementation(libs.kmpio)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlin.logging)
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
        linuxArm64Main.get().dependsOn(posixMain)
        linuxArm64Test.get().dependsOn(posixTest)

    }

    targets.withType<KotlinNativeTarget>().all {
        binaries {
            executable {
                entryPoint = "org.gnit.bible.cli.main"
                baseName = "bbl-packer"
            }
        }
    }
}
