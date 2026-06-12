import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}

data class BblInstallPlatform(
    val id: String,
    val taskNamePart: String,
    val nativeTargetName: String,
    val linkTaskSuffix: String,
    val executableSuffix: String,
)

data class BblInstallBinary(
    val id: String,
    val taskNamePart: String,
    val projectPath: String,
    val binaryName: String,
    val includePacks: Boolean = false,
)

val bblInstallPlatforms = listOf(
    BblInstallPlatform("linux", "Linux", "linuxX64", "LinuxX64", ".kexe"),
    BblInstallPlatform("macosArm64", "MacosArm64", "macosArm64", "MacosArm64", ".kexe"),
    BblInstallPlatform("macosX64", "MacosX64", "macosX64", "MacosX64", ".kexe"),
    BblInstallPlatform("windows", "Windows", "mingwX64", "MingwX64", ".exe"),
)

val bblInstallBinaries = listOf(
    BblInstallBinary("cli-core", "CliCore", ":cli:core", "bbl", includePacks = true),
    BblInstallBinary("cli-search-common", "CliSearchCommon", ":cli:search:common", "bbl-search-common"),
    BblInstallBinary("cli-search-extra", "CliSearchExtra", ":cli:search:extra", "bbl-search-extra"),
    BblInstallBinary("cli-search-kuromoji", "CliSearchKuromoji", ":cli:search:kuromoji", "bbl-search-kuromoji"),
    BblInstallBinary("cli-search-morfologik", "CliSearchMorfologik", ":cli:search:morfologik", "bbl-search-morfologik"),
    BblInstallBinary("cli-search-nori", "CliSearchNori", ":cli:search:nori", "bbl-search-nori"),
    BblInstallBinary("cli-search-smartcn", "CliSearchSmartcn", ":cli:search:smartcn", "bbl-search-smartcn"),
)

val bblVersionFile = layout.projectDirectory.file("core/src/commonMain/kotlin/org/gnit/bible/BblVersion.kt")

val bblVersionProvider = providers.fileContents(
    bblVersionFile
).asText.map { source ->
    listOf(
        Regex("""(?:const\s+)?val\s+VERSION(?:\s*:\s*String)?\s*=\s*"([^"]+)""""),
        Regex("""(?:const\s+)?val\s+version(?:\s*:\s*String)?\s*=\s*"([^"]+)"""")
    ).firstNotNullOfOrNull { regex ->
        regex.find(source)?.groupValues?.get(1)
    }
        ?: error("Unable to read version from BblVersion.kt")
}

val bblInstallVersionFixtureFile = layout.buildDirectory.file("bblInstallFixtures/common/version.txt")
val bblInstallCookbookVersionFile = layout.projectDirectory.file("bbl_install/files/version.txt")
val bblPacksDirectory = layout.projectDirectory.dir("resources/bblpacks")
val bblPackManifestFiles = fileTree(layout.projectDirectory) {
    include("resources/bbltexts/**/*.manifest.json")
    include("app/shared/src/commonMain/composeResources/files/bblpacks/**/*.manifest.json")
    include("core/src/commonTest/resources/**/*.manifest.json")
}

val stageBblInstallVersionFixture = tasks.register("stageBblInstallVersionFixture") {
    notCompatibleWithConfigurationCache("Writes staged cookbook version files using script-scoped providers.")
    inputs.property("bblVersion", bblVersionProvider)
    outputs.files(bblInstallVersionFixtureFile, bblInstallCookbookVersionFile)

    doLast {
        val version = bblVersionProvider.get()
        bblInstallVersionFixtureFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("$version\n")
        }
        bblInstallCookbookVersionFile.asFile.apply {
            parentFile.mkdirs()
            writeText("$version\n")
        }
    }
}

tasks.register("updateBblPackManifestVersions") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Update only the version field in existing bbl pack manifest JSON files without rebuilding indexes."
    notCompatibleWithConfigurationCache("Rewrites existing zip files using script-scoped providers and JDK zip streams.")

    val packFiles = fileTree(bblPacksDirectory) {
        include("*.zip")
    }

    inputs.property("bblVersion", bblVersionProvider)
    inputs.files(packFiles)
    inputs.files(bblPackManifestFiles)
    outputs.files(packFiles, bblPackManifestFiles)

    doLast {
        val version = bblVersionProvider.get()
        val versionFieldRegex = Regex(""""version"\s*:\s*"[^"]*"""")
        var updatedZipCount = 0
        var currentZipCount = 0
        var updatedManifestFileCount = 0
        var currentManifestFileCount = 0

        bblPackManifestFiles.files.sortedBy { it.relativeTo(projectDir).path }.forEach { manifestFile ->
            val manifestJson = manifestFile.readText()
            require(versionFieldRegex.containsMatchIn(manifestJson)) {
                "Manifest ${manifestFile.relativeTo(projectDir)} does not contain a version field"
            }
            val updatedJson = versionFieldRegex.replace(manifestJson, """"version":"$version"""")
            if (updatedJson != manifestJson) {
                manifestFile.writeText(updatedJson)
                updatedManifestFileCount += 1
            } else {
                currentManifestFileCount += 1
            }
        }

        packFiles.files.sortedBy { it.name }.forEach { zipFile ->
            val manifestName = "${zipFile.nameWithoutExtension}.0.manifest.json"
            val tempFile = temporaryDir.resolve("${zipFile.name}.tmp")
            var foundManifest = false
            var changedManifest = false

            ZipInputStream(zipFile.inputStream().buffered()).use { input ->
                ZipOutputStream(tempFile.outputStream().buffered()).use { output ->
                    while (true) {
                        val sourceEntry = input.nextEntry ?: break
                        val entryBytes = input.readBytes()
                        val targetEntry = ZipEntry(sourceEntry.name)
                        targetEntry.comment = sourceEntry.comment
                        targetEntry.setExtra(sourceEntry.extra)
                        targetEntry.time = sourceEntry.time

                        val outputBytes = if (sourceEntry.name == manifestName) {
                            foundManifest = true
                            val manifestJson = entryBytes.toString(Charsets.UTF_8)
                            require(versionFieldRegex.containsMatchIn(manifestJson)) {
                                "Manifest $manifestName in ${zipFile.name} does not contain a version field"
                            }
                            val updatedJson = versionFieldRegex.replace(manifestJson, """"version":"$version"""")
                            changedManifest = updatedJson != manifestJson
                            updatedJson.toByteArray(Charsets.UTF_8)
                        } else {
                            entryBytes
                        }

                        output.putNextEntry(targetEntry)
                        output.write(outputBytes)
                        output.closeEntry()
                        input.closeEntry()
                    }
                }
            }

            require(foundManifest) {
                "Manifest $manifestName not found in ${zipFile.name}"
            }

            if (changedManifest) {
                tempFile.copyTo(zipFile, overwrite = true)
                updatedZipCount += 1
            } else {
                tempFile.delete()
                currentZipCount += 1
            }
        }

        logger.lifecycle(
            "Updated $updatedManifestFileCount loose manifest file(s) and $updatedZipCount pack zip manifest(s) to $version; " +
                "$currentManifestFileCount loose manifest file(s) and $currentZipCount pack zip manifest(s) already current."
        )
    }
}

tasks.register("verifyServerBblPackVersions") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verify loose bbl pack manifests and pack zip manifests match BblVersion.VERSION."
    notCompatibleWithConfigurationCache("Reads existing manifest files and zip entries using script-scoped providers.")

    val packFiles = fileTree(bblPacksDirectory) {
        include("*.zip")
    }

    inputs.property("bblVersion", bblVersionProvider)
    inputs.files(packFiles, bblPackManifestFiles)

    doLast {
        val version = bblVersionProvider.get()
        val versionFieldRegex = Regex(""""version"\s*:\s*"([^"]*)"""")
        val mismatches = mutableListOf<String>()

        bblPackManifestFiles.files.sortedBy { it.relativeTo(projectDir).path }.forEach { manifestFile ->
            val actual = versionFieldRegex.find(manifestFile.readText())?.groupValues?.get(1)
            if (actual != version) {
                mismatches += "${manifestFile.relativeTo(projectDir)} has version ${actual ?: "<missing>"}"
            }
        }

        packFiles.files.sortedBy { it.name }.forEach { zipFile ->
            val manifestName = "${zipFile.nameWithoutExtension}.0.manifest.json"
            var foundManifest = false

            ZipInputStream(zipFile.inputStream().buffered()).use { input ->
                while (true) {
                    val sourceEntry = input.nextEntry ?: break
                    if (sourceEntry.name == manifestName) {
                        foundManifest = true
                        val actual = versionFieldRegex.find(input.readBytes().toString(Charsets.UTF_8))
                            ?.groupValues
                            ?.get(1)
                        if (actual != version) {
                            mismatches += "${zipFile.relativeTo(projectDir)}!/$manifestName has version ${actual ?: "<missing>"}"
                        }
                    }
                    input.closeEntry()
                }
            }

            if (!foundManifest) {
                mismatches += "${zipFile.relativeTo(projectDir)} is missing $manifestName"
            }
        }

        if (mismatches.isNotEmpty()) {
            error(
                "Expected bbl pack manifest versions to be $version, but found:\n" +
                    mismatches.joinToString(separator = "\n")
            )
        }

        logger.lifecycle(
            "Verified ${bblPackManifestFiles.files.size} loose manifest file(s) and ${packFiles.files.size} pack zip manifest(s) match $version."
        )
    }
}

val bblInstallCommonFixtureDirectory = layout.buildDirectory.dir("bblInstallFixtures/common")

val stageBblInstallFixtureTasks = bblInstallPlatforms.flatMap { platform ->
    bblInstallBinaries.map { binary ->
        val taskName = "stageBblInstall${platform.taskNamePart}${binary.taskNamePart}Fixture"
        val executableFileName = binary.binaryName + platform.executableSuffix
        tasks.register<Copy>(taskName) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Stage ${binary.id} ${platform.id} fixture files for bbl_install Kitchen tests."

            dependsOn("${binary.projectPath}:linkDebugExecutable${platform.linkTaskSuffix}")
            dependsOn(stageBblInstallVersionFixture)

            into(layout.buildDirectory.dir("bblInstallFixtures/${platform.id}/${binary.id}"))
            from(project(binary.projectPath).layout.buildDirectory.dir("bin/${platform.nativeTargetName}/debugExecutable")) {
                include(executableFileName)
                rename(Regex.escape(executableFileName), binary.binaryName + if (platform.id == "windows") ".exe" else "")
            }

            if (binary.includePacks) {
                from(rootProject.layout.projectDirectory.dir("resources/bblpacks")) {
                    include("*.zip")
                }
            }
        }
    }
}

fun Sync.prepareBblInstallCookbookFiles(platform: BblInstallPlatform) {
    val platformFixtureTasks = stageBblInstallFixtureTasks.filter { it.name.contains(platform.taskNamePart) }
    dependsOn(platformFixtureTasks)
    dependsOn(stageBblInstallVersionFixture)

    into(layout.projectDirectory.dir("bbl_install/files"))
    from(platformFixtureTasks.map { layout.buildDirectory.dir("bblInstallFixtures/${platform.id}") })
    from(bblInstallCommonFixtureDirectory)
    include("**/*")
    eachFile {
        relativePath = RelativePath(true, name)
    }
    includeEmptyDirs = false
    outputs.upToDateWhen { false }
    preserve {
        include("README.md")
    }
}

tasks.register<Sync>("stageBblInstallLinuxFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
}

tasks.register<Sync>("stageBblInstallLinuxCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
}

tasks.register<Sync>("stageBblInstallWindowsFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "windows" })
}

tasks.register<Sync>("stageBblInstallWindowsCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "windows" })
}

tasks.register<Sync>("stageBblInstallMacosArm64CliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS Arm64 CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "macosArm64" })
}

tasks.register<Sync>("stageBblInstallMacosX64CliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS X64 CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "macosX64" })
}

tasks.register("stageBblInstallFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks)
}

tasks.register("stageBblInstallCommonFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage Linux and Windows fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { !it.name.contains("Macos") })
}
