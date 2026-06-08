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
        Regex("""val version = "([^"]+)"""")
    ).firstNotNullOfOrNull { regex ->
        regex.find(source)?.groupValues?.get(1)
    }
        ?: error("Unable to read version from BblVersion.kt")
}

val bblInstallVersionFixtureFile = layout.buildDirectory.file("bblInstallFixtures/common/version.txt")
val bblInstallCookbookVersionFile = layout.projectDirectory.file("bbl_install/files/version.txt")

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
