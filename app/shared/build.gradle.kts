@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import groovy.json.JsonSlurper
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

data class GradleBblAppEdition(
    val id: String,
    val displayName: String,
    val embeddedCodes: List<String>,
    val kind: String,
)

val bblAppEditionCatalog = JsonSlurper().parse(rootProject.layout.projectDirectory.file("resources/bbl-app-editions.json").asFile) as Map<*, *>
val bblAppTranslationCodes = (bblAppEditionCatalog["translations"] as List<*>).map { rawTranslation ->
    (rawTranslation as Map<*, *>)["code"] as String
}
val allBblAppEditions = (bblAppEditionCatalog["editions"] as List<*>).map { rawEdition ->
    val edition = rawEdition as Map<*, *>
    GradleBblAppEdition(
        id = edition["id"] as String,
        displayName = edition["displayName"] as String,
        embeddedCodes = (edition["embeddedCodes"] as List<*>).map { it as String },
        kind = edition["kind"] as String
    )
}
val bblAppEditionsById = allBblAppEditions.associateBy { it.id }

fun String.csvCodes(): List<String> =
    split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }

val selectedBblAppEdition: GradleBblAppEdition = run {
    val explicitCodes = providers.gradleProperty("bbl.app.embeddedCodes").orNull?.csvCodes()
    if (!explicitCodes.isNullOrEmpty()) {
        val unknownCodes = explicitCodes.filterNot { it in bblAppTranslationCodes }
        require(unknownCodes.isEmpty()) { "Unknown bbl.app.embeddedCodes value(s): ${unknownCodes.joinToString(", ")}" }
        GradleBblAppEdition(
            id = explicitCodes.joinToString("-"),
            displayName = explicitCodes.joinToString(" + ") { it.uppercase(Locale.ROOT) },
            embeddedCodes = explicitCodes.distinct(),
            kind = "custom"
        )
    } else {
        val editionId = providers.gradleProperty("bbl.app.edition").orElse("webus").get()
        bblAppEditionsById[editionId]
            ?: error("Unknown bbl.app.edition '$editionId'. Run ./gradlew -q printBblAppEditionIds to list valid edition IDs.")
    }
}

@CacheableTask
abstract class GenerateBblAppEmbeddedPackRegistryTask : DefaultTask() {
    @get:Input
    abstract val editionId: Property<String>

    @get:Input
    abstract val editionDisplayName: Property<String>

    @get:Input
    abstract val embeddedCodes: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.gnit.bible.app

            object EmbeddedPackRegistry {
                const val editionId: String = "${editionId.get()}"
                const val editionDisplayName: String = "${editionDisplayName.get()}"
                val embeddedCodes: Set<String> = setOf(${embeddedCodes.get().joinToString { "\"$it\"" }})
            }
            """.trimIndent() + "\n"
        )
    }
}

@CacheableTask
abstract class SyncBblAppEmbeddedResourcesTask : DefaultTask() {
    @get:Input
    abstract val editionId: Property<String>

    @get:Input
    abstract val embeddedCodes: ListProperty<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baseResourcesDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bblTextsDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun sync() {
        val output = outputDirectory.get().asFile.toPath()
        output.toFile().deleteRecursively()
        copyDirectory(
            source = baseResourcesDirectory.get().asFile.toPath(),
            destination = output,
            exclude = { relativePath ->
                relativePath.nameCount >= 2 &&
                    relativePath.getName(0).toString() == "files" &&
                    relativePath.getName(1).toString() == "bblpacks"
            }
        )

        val bblTexts = bblTextsDirectory.get().asFile.toPath()
        embeddedCodes.get().forEach { code ->
            val textDirectory = bblTexts.resolve(code)
            require(Files.isDirectory(textDirectory)) {
                "Missing BBL text directory for Android app edition '${editionId.get()}': $textDirectory"
            }
            copyDirectory(
                source = textDirectory,
                destination = output.resolve("files").resolve("bblpacks").resolve(code)
            )
        }
    }

    private fun copyDirectory(
        source: Path,
        destination: Path,
        exclude: (Path) -> Boolean = { false }
    ) {
        Files.walk(source).use { paths ->
            paths.forEach { sourcePath ->
                val relativePath = source.relativize(sourcePath)
                if (relativePath.nameCount == 0 || exclude(relativePath)) return@forEach
                val targetPath = destination.resolve(relativePath)
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.createDirectories(targetPath.parent)
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}

compose.resources {
    packageOfResClass = "org.gnit.bible.app"
    generateResClass = auto
    customDirectory("commonMain", layout.buildDirectory.dir("generated/bblAppEdition/${selectedBblAppEdition.id}/composeResources"))
    customDirectory("commonTest", layout.buildDirectory.dir("generated/composeTestPackResources"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries {
            /*
             * Temporary Kotlin/Native cache workaround for CI:
             * Xcode 16.4 iOS simulator test linking fails in cached Compose/Skiko artifacts
             * with UIViewLayoutRegion/UIUtilities symbols. Once that CI issue is resolved,
             * remove this all { disableNativeCache(...) } block to re-enable native caches.
             */
            all {
                disableNativeCache(
                    DisableCacheInKotlinVersion.`2_4_0`,
                    "Work around CI iOS simulator link failure in cached Compose/Skiko Kotlin/Native artifacts."
                )
            }

            framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    jvm()

    android {
        namespace = "org.gnit.bible.app.shared"
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
        val syncComposeTestPacks = tasks.register<Sync>("syncComposeTestPacks") {
            from(rootProject.layout.projectDirectory.dir("resources/bblpacks"))
            into(layout.buildDirectory.dir("generated/composeTestPackResources/files/bblpackzips"))
            include("*.zip")
        }

        tasks.named("copyNonXmlValueResourcesForCommonTest") {
            dependsOn(syncComposeTestPacks)
        }

        val syncAndroidDeviceTestPacks = tasks.register<Sync>("syncAndroidDeviceTestPacks") {
            from(rootProject.layout.projectDirectory.dir("resources/bblpacks"))
            into(layout.buildDirectory.dir("androidDeviceTestAssets/bblpacks"))
            include(
                "abtag.zip", "ayt.zip", "irvben.zip", "irvguj.zip", "irvhin.zip",
                "irvmar.zip", "irvtam.zip", "irvtel.zip", "irvurd.zip", "kttv.zip",
                "npiulb.zip", "th1971.zip"
            )
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.slf4j.android)
        }
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/bblAppEdition/${selectedBblAppEdition.id}/kotlin"))
            dependencies {
                api(projects.core)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.compose.material.iconsCore)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.logging)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.locale)
                implementation(libs.okio)
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
                implementation(libs.ktor.clientMock)
                implementation(libs.okio.fakefs)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        iosMain {
            dependsOn(commonMain.get())
            iosArm64Main.get().dependsOn(this)
            iosSimulatorArm64Main.get().dependsOn(this)
        }
        iosTest {
            dependsOn(commonTest)
            iosArm64Test.get().dependsOn(this)
            iosSimulatorArm64Test.get().dependsOn(this)
        }
    }
}

val generateBblAppEmbeddedPackRegistry = tasks.register<GenerateBblAppEmbeddedPackRegistryTask>("generateBblAppEmbeddedPackRegistry") {
    editionId.set(selectedBblAppEdition.id)
    editionDisplayName.set(selectedBblAppEdition.displayName)
    embeddedCodes.set(selectedBblAppEdition.embeddedCodes)
    outputFile.set(
        layout.buildDirectory.file(
            "generated/bblAppEdition/${selectedBblAppEdition.id}/kotlin/org/gnit/bible/app/EmbeddedPackRegistry.kt"
        )
    )
}

val syncBblAppEmbeddedResources = tasks.register<SyncBblAppEmbeddedResourcesTask>("syncBblAppEmbeddedResources") {
    editionId.set(selectedBblAppEdition.id)
    embeddedCodes.set(selectedBblAppEdition.embeddedCodes)
    baseResourcesDirectory.set(layout.projectDirectory.dir("src/commonMain/composeResources"))
    bblTextsDirectory.set(rootProject.layout.projectDirectory.dir("resources/bbltexts"))
    outputDirectory.set(layout.buildDirectory.dir("generated/bblAppEdition/${selectedBblAppEdition.id}/composeResources"))
}

tasks.matching {
    it.name.contains("compile", ignoreCase = true) ||
        it.name.contains("process", ignoreCase = true) ||
        it.name.contains("convert", ignoreCase = true) ||
        it.name.contains("prepare", ignoreCase = true) ||
        it.name.contains("generateCompose", ignoreCase = true) ||
        it.name.contains("copyNonXmlValueResources", ignoreCase = true)
}.configureEach {
    dependsOn(generateBblAppEmbeddedPackRegistry)
    dependsOn(syncBblAppEmbeddedResources)
}

val cleanBblAppGeneratedAndroidAssets = tasks.register<Delete>("cleanBblAppGeneratedAndroidAssets") {
    delete(layout.buildDirectory.dir("generated/assets"))
    delete(layout.buildDirectory.dir("intermediates/assets"))
}

tasks.matching {
    it.name.contains("copy", ignoreCase = true) &&
        it.name.contains("ComposeResourcesTo", ignoreCase = true) &&
        it.name.contains("Assets", ignoreCase = true)
}.configureEach {
    dependsOn(cleanBblAppGeneratedAndroidAssets)
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(24))
        }
    )
}

val iosAppXcodeProject = rootProject.layout.projectDirectory.dir("app/iosApp/iosApp.xcodeproj")

gradle.taskGraph.whenReady {
    tasks.findByName("convertPbxprojToJson")?.let { task ->
        val pbxprojFile = task.javaClass.methods
            .first { method -> method.name == "getPbxprojFile" && method.parameterCount == 0 }
            .invoke(task) as org.gradle.api.file.RegularFileProperty
        pbxprojFile.set(iosAppXcodeProject.file("project.pbxproj"))
    }

    tasks.findByName("checkXcodeProjectConfiguration")?.let { task ->
        val xcodeProjectPath = task.javaClass.methods
            .first { method -> method.name == "getXcodeProjectPath" && method.parameterCount == 0 }
            .invoke(task) as org.gradle.api.file.DirectoryProperty
        xcodeProjectPath.set(iosAppXcodeProject)
    }
}
