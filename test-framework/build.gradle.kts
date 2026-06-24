import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "org.gnit.bible.test"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    @Suppress("DEPRECATION")
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

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
        val mingwX64Main by getting { dependsOn(nativeMain) }

        val commonTest by getting
        val jvmTest by getting
        val nativeTest by creating { dependsOn(commonTest) }
        val mingwX64Test by getting { dependsOn(nativeTest) }

        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.clientMock)
            implementation(libs.multiplatform.settings)
            implementation(libs.okio)
            implementation(libs.lucene.kmp.core)
            implementation(libs.okio.fakefs)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio)
            implementation(libs.okio.fakefs)
        }

        androidMain.dependencies {
            implementation(libs.junit)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.runner)
            implementation(libs.robolectric)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        optIn.addAll("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}
