import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync

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

val bblCliVersionProvider = providers.fileContents(
    bblVersionFile
).asText.map { source ->
    listOf(
        Regex("""const val bblCliVersion = "([^"]+)""""),
        Regex("""const val cliVersion = "([^"]+)"""")
    ).firstNotNullOfOrNull { regex ->
        regex.find(source)?.groupValues?.get(1)
    }
        ?: error("Unable to read bblCliVersion from BblVersion.kt")
}

val bblArtifactCompatibilityVersionProvider = bblCliVersionProvider

val bblPacksDirectory = layout.projectDirectory.dir("resources/bblpacks")

tasks.register("verifyBblPackVersions") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verify bblpack zip manifests match bblArtifactCompatibilityVersion."

    inputs.file(bblVersionFile)
    inputs.property("bblArtifactCompatibilityVersion", bblArtifactCompatibilityVersionProvider)
    inputs.files(fileTree(bblPacksDirectory) {
        include("*.zip")
    })

    val expectedVersionProvider = bblArtifactCompatibilityVersionProvider
    val packDirectory = bblPacksDirectory.asFile
    doLast {
        val expectedVersion = expectedVersionProvider.get()
        val zipFiles = packDirectory.listFiles { file -> file.isFile && file.extension == "zip" }
            ?.sortedBy { it.name }
            .orEmpty()

        require(zipFiles.isNotEmpty()) {
            "No bblpack zips found in ${packDirectory.absolutePath}"
        }

        val versionRegex = Regex(""""bblArtifactCompatibilityVersion"\s*:\s*"([^"]+)"""")
        val failures = mutableListOf<String>()

        zipFiles.forEach { zipFile ->
            java.util.zip.ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.entries().asSequence()
                    .firstOrNull { it.name.endsWith(".0.manifest.json") }
                if (manifestEntry == null) {
                    failures.add("${zipFile.name}: missing .0.manifest.json")
                    return@use
                }

                val manifestJson = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                val actualVersion = versionRegex.find(manifestJson)?.groupValues?.get(1)
                if (actualVersion != expectedVersion) {
                    failures.add("${zipFile.name}: bblArtifactCompatibilityVersion ${actualVersion ?: "<missing>"} != $expectedVersion")
                }
            }
        }

        if (failures.isNotEmpty()) {
            throw GradleException(
                "Bblpack versions are not compatible with bbl artifact compatibility version $expectedVersion:\n" +
                    failures.joinToString(separator = "\n") { " - $it" } +
                    "\nRegenerate packs with bbl pack before staging fixtures or publishing."
            )
        }
    }
}

val bblInstallVersionFixtureFile = layout.buildDirectory.file("bblInstallFixtures/common/version.txt")
val bblInstallArtifactCompatibilityVersionFixtureFile = layout.buildDirectory.file("bblInstallFixtures/common/artifact_compatibility_version.txt")
val bblInstallCookbookVersionFile = layout.projectDirectory.file("bbl_install/files/version.txt")
val bblInstallCookbookArtifactCompatibilityVersionFile = layout.projectDirectory.file("bbl_install/files/artifact_compatibility_version.txt")

val stageBblInstallVersionFixture = tasks.register("stageBblInstallVersionFixture") {
    notCompatibleWithConfigurationCache("Writes staged cookbook version files using script-scoped providers.")
    inputs.property("bblCliVersion", bblCliVersionProvider)
    inputs.property("bblArtifactCompatibilityVersion", bblArtifactCompatibilityVersionProvider)
    outputs.files(bblInstallVersionFixtureFile, bblInstallArtifactCompatibilityVersionFixtureFile)
    outputs.files(bblInstallCookbookVersionFile, bblInstallCookbookArtifactCompatibilityVersionFile)

    doLast {
        bblInstallVersionFixtureFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("${bblCliVersionProvider.get()}\n")
        }
        bblInstallArtifactCompatibilityVersionFixtureFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("${bblArtifactCompatibilityVersionProvider.get()}\n")
        }
        bblInstallCookbookVersionFile.asFile.apply {
            parentFile.mkdirs()
            writeText("${bblCliVersionProvider.get()}\n")
        }
        bblInstallCookbookArtifactCompatibilityVersionFile.asFile.apply {
            parentFile.mkdirs()
            writeText("${bblArtifactCompatibilityVersionProvider.get()}\n")
        }
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

            dependsOn("${binary.projectPath}:linkReleaseExecutable${platform.linkTaskSuffix}")
            dependsOn(stageBblInstallVersionFixture)

            into(layout.buildDirectory.dir("bblInstallFixtures/${platform.id}/${binary.id}"))
            from(project(binary.projectPath).layout.buildDirectory.dir("bin/${platform.nativeTargetName}/releaseExecutable")) {
                include(executableFileName)
                rename(Regex.escape(executableFileName), binary.binaryName + if (platform.id == "windows") ".exe" else "")
            }

            if (binary.includePacks) {
                dependsOn("verifyBblPackVersions")
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
