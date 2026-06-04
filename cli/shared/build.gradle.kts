import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    @Suppress("DEPRECATION")
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()
    jvm()

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.okio)
                implementation(libs.clikt)
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }

        val posixMain by creating { dependsOn(nativeMain) }

        macosX64Main.get().dependsOn(posixMain)
        macosArm64Main.get().dependsOn(posixMain)
        linuxX64Main.get().dependsOn(posixMain)

        mingwX64Main.get().dependsOn(nativeMain)
    }
}
