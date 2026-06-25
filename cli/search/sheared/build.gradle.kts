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
    mingwX64()
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
                implementation(libs.kotlin.logging)
                implementation(libs.lucene.kmp.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.testFramework)
                implementation(libs.kotlin.test)
                implementation(libs.lucene.kmp.core)
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

        mingwX64Main.get().dependsOn(nativeMain)
        mingwX64Test.get().dependsOn(nativeTest)
    }
}
