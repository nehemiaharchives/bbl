import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
}

@CacheableTask
abstract class GenerateBblAppEditionCatalogSourceTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val catalogFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val catalog = JsonSlurper().parse(catalogFile.get().asFile) as Map<*, *>
        val editions = catalog["editions"] as List<*>
        val outputFile = outputDirectory.get().asFile.resolve("org/gnit/bible/BblAppEdition.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            buildString {
                appendLine("package org.gnit.bible")
                appendLine()
                appendLine("data class BblAppEdition(")
                appendLine("    val id: String,")
                appendLine("    val displayName: String,")
                appendLine("    val embeddedCodes: Set<String>,")
                appendLine("    val kind: Kind,")
                appendLine(") {")
                appendLine("    enum class Kind { single, pair, regional }")
                appendLine("}")
                appendLine()
                appendLine("object BblAppEditionCatalog {")
                appendLine("    val all: List<BblAppEdition> = listOf(")
                editions.forEach { rawEdition ->
                    val edition = rawEdition as Map<*, *>
                    val id = edition["id"] as String
                    val displayName = edition["displayName"] as String
                    val embeddedCodes = edition["embeddedCodes"] as List<*>
                    val kind = edition["kind"] as String
                    appendLine("        BblAppEdition(")
                    appendLine("            id = ${id.kotlinString()},")
                    appendLine("            displayName = ${displayName.kotlinString()},")
                    appendLine("            embeddedCodes = linkedSetOf(${embeddedCodes.joinToString { (it as String).kotlinString() }}),")
                    appendLine("            kind = BblAppEdition.Kind.$kind")
                    appendLine("        ),")
                }
                appendLine("    )")
                appendLine()
                appendLine("    private val byId: Map<String, BblAppEdition> by lazy { all.associateBy { it.id } }")
                appendLine()
                appendLine("    fun byId(id: String): BblAppEdition =")
                appendLine("        byId[id] ?: error(\"Unknown BBL app edition '\$id'. Known editions: \${all.joinToString { it.id }}\")")
                appendLine("}")
            }
        )
    }

    private fun String.kotlinString(): String =
        buildString {
            append('"')
            this@kotlinString.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
}

val generateBblAppEditionCatalogSource = tasks.register<GenerateBblAppEditionCatalogSourceTask>("generateBblAppEditionCatalogSource") {
    catalogFile.set(rootProject.layout.projectDirectory.file("resources/bbl-app-editions.json"))
    outputDirectory.set(layout.buildDirectory.dir("generated/bblAppEditionCatalog/kotlin"))
}

kotlin {
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

    android {
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
            kotlin.srcDir(generateBblAppEditionCatalogSource.flatMap { it.outputDirectory })
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
        linuxArm64Main.get().dependsOn(posixMain)
        linuxArm64Test.get().dependsOn(posixTest)
        mingwX64Main.get().dependsOn(nativeMain)
        mingwX64Test.get().dependsOn(nativeTest)

        mingwX64Main.get().dependencies {
            implementation(libs.ktor.clientWinHttp)
        }

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
