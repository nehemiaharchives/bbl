import java.security.MessageDigest
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

data class BblHomebrewMacosFixture(
    val platformId: String,
    val taskNamePart: String,
    val archiveSuffix: String,
)

fun File.sha256Hex(): String = inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

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
val linuxDebOutputDirectory = layout.buildDirectory.dir("distributions")
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


// https://github.com/nehemiaharchives/bbl-kmp is for development so we will migrate to following
val bblGitHubRepositoryUrl = "https://github.com/nehemiaharchives/bbl"
val bblAuthorName = "Hokuto Joel Ide"
val bblAuthorEmail = "nehemiaharchive@gmail.com"
val bblDescription = "Read/search Holy Bible in your terminal"

val macosPkgId = "org.gnit.bbl"
val macosPkgOutputDirectory = layout.buildDirectory.dir("distributions")

fun registerMacosPkgTasks(platformId: String, taskNamePart: String, architecture: String) {
    val pkgRoot = layout.buildDirectory.dir("macosPkg/$platformId/root")
    val pkgOutputFile = bblVersionProvider.map { version ->
        macosPkgOutputDirectory.get().file("bbl-$version-macos-$architecture.pkg")
    }
    val buildTask = tasks.register<Exec>("build${taskNamePart}Pkg") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Build the unsigned macOS $architecture .pkg installer for bbl."
        notCompatibleWithConfigurationCache("Builds a pkg using script-scoped providers and filesystem preparation.")
        onlyIf("pkgbuild is available only on macOS") {
            System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
        }
        dependsOn(
            "stageBblInstall${taskNamePart}CliCoreFixture",
            "stageBblInstall${taskNamePart}CliSearchCommonFixture",
        )
        dependsOn(stageBblInstallVersionFixture)

        val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/$platformId/cli-core/bbl")
        val stagedSearchCommon = layout.buildDirectory.file(
            "bblInstallFixtures/$platformId/cli-search-common/bbl-search-common"
        )
        val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
        inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
        inputs.property("bblVersion", bblVersionProvider)
        outputs.file(pkgOutputFile)
        environment("COPYFILE_DISABLE", "1")

        doFirst {
            val source = stagedBbl.get().asFile
            val searchCommon = stagedSearchCommon.get().asFile
            val webusPack = stagedWebusPack.asFile
            require(source.isFile) { "Missing staged bbl binary: ${source.absolutePath}" }
            require(searchCommon.isFile) { "Missing staged bbl-search-common binary: ${searchCommon.absolutePath}" }
            require(webusPack.isFile) { "Missing webus pack: ${webusPack.absolutePath}" }
            val root = pkgRoot.get().asFile
            val libexec = root.resolve("usr/local/libexec/bbl")
            val target = libexec.resolve("bbl")
            val wrapper = root.resolve("usr/local/bin/bbl")
            root.deleteRecursively()
            target.parentFile.mkdirs()
            source.copyTo(target, overwrite = true)
            searchCommon.copyTo(libexec.resolve("bbl-search-common"), overwrite = true)
            webusPack.copyTo(libexec.resolve("webus.zip"), overwrite = true)
            require(target.setExecutable(true, false)) { "Unable to make ${target.absolutePath} executable" }
            require(libexec.resolve("bbl-search-common").setExecutable(true, false)) {
                "Unable to make packaged bbl-search-common executable"
            }
            wrapper.parentFile.mkdirs()
            wrapper.writeText(
                """
                #!/bin/bash
                set -e
                assets=/usr/local/libexec/bbl
                mkdir -p "${'$'}HOME/.bbl/bin" "${'$'}HOME/.bbl/packs"
                if ! cmp -s "${'$'}assets/bbl-search-common" "${'$'}HOME/.bbl/bin/bbl-search-common"; then
                  install -m 0755 "${'$'}assets/bbl-search-common" "${'$'}HOME/.bbl/bin/bbl-search-common"
                fi
                if ! cmp -s "${'$'}assets/webus.zip" "${'$'}HOME/.bbl/packs/webus.zip"; then
                  install -m 0644 "${'$'}assets/webus.zip" "${'$'}HOME/.bbl/packs/webus.zip"
                fi
                exec "${'$'}assets/bbl" "${'$'}@"
                """.trimIndent() + "\n"
            )
            require(wrapper.setExecutable(true, false)) { "Unable to make ${wrapper.absolutePath} executable" }
            val xattrExitCode = ProcessBuilder("xattr", "-cr", root.absolutePath).inheritIO().start().waitFor()
            require(xattrExitCode == 0) { "Unable to clear extended attributes from ${root.absolutePath}" }

            macosPkgOutputDirectory.get().asFile.mkdirs()
            commandLine(
                "pkgbuild", "--root", root.absolutePath,
                "--identifier", macosPkgId, "--version", bblVersionProvider.get(),
                "--install-location", "/", "--filter", ".*\\._.*",
                pkgOutputFile.get().asFile.absolutePath,
            )
        }
    }

    tasks.register<Sync>("stageBblInstall${taskNamePart}PkgFixture") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Stage the macOS $architecture .pkg installer fixture for Kitchen tests."
        dependsOn(buildTask, stageBblInstallVersionFixture)
        into(layout.buildDirectory.dir("bblInstallFixtures/$platformId/pkg"))
        from(pkgOutputFile) { rename { "bbl.pkg" } }
        from(bblInstallCommonFixtureDirectory) { include("version.txt") }
    }
}

registerMacosPkgTasks("macosArm64", "MacosArm64", "arm64")
registerMacosPkgTasks("macosX64", "MacosX64", "x64")

val bblHomebrewMacosFixtures = listOf(
    BblHomebrewMacosFixture("macosArm64", "MacosArm64", "macos-arm64"),
    BblHomebrewMacosFixture("macosX64", "MacosX64", "macos-x64"),
)

bblHomebrewMacosFixtures.forEach { fixture ->
    tasks.register("stageBblInstall${fixture.taskNamePart}HomebrewFixture") {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Stage the ${fixture.platformId} Homebrew formula fixture for Kitchen tests."
        notCompatibleWithConfigurationCache("Creates a tar archive and formula from script-scoped providers.")
        dependsOn(
            "stageBblInstall${fixture.taskNamePart}CliCoreFixture",
            "stageBblInstall${fixture.taskNamePart}CliSearchCommonFixture",
            stageBblInstallVersionFixture,
        )

        val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/${fixture.platformId}/cli-core/bbl")
        val stagedSearchCommon = layout.buildDirectory.file(
            "bblInstallFixtures/${fixture.platformId}/cli-search-common/bbl-search-common"
        )
        val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
        val fixtureDirectory = layout.buildDirectory.dir("bblInstallFixtures/${fixture.platformId}/homebrew")
        inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
        inputs.property("bblVersion", bblVersionProvider)
        outputs.dir(fixtureDirectory)

        doLast {
            val version = bblVersionProvider.get()
            val source = stagedBbl.get().asFile
            val searchCommon = stagedSearchCommon.get().asFile
            val webusPack = stagedWebusPack.asFile
            require(source.isFile) { "Missing staged bbl binary: ${source.absolutePath}" }
            require(searchCommon.isFile) { "Missing staged bbl-search-common binary: ${searchCommon.absolutePath}" }
            require(webusPack.isFile) { "Missing webus pack: ${webusPack.absolutePath}" }

            val output = fixtureDirectory.get().asFile
            val formulaDirectory = output.resolve("Formula")
            val archive = output.resolve("bbl-$version-${fixture.archiveSuffix}.tar.gz")
            output.deleteRecursively()
            formulaDirectory.mkdirs()

            val archiveRoot = temporaryDir.resolve("${fixture.platformId}-homebrew")
            archiveRoot.deleteRecursively()
            archiveRoot.mkdirs()
            source.copyTo(archiveRoot.resolve("bbl"), overwrite = true)
            searchCommon.copyTo(archiveRoot.resolve("bbl-search-common"), overwrite = true)
            webusPack.copyTo(archiveRoot.resolve("webus.zip"), overwrite = true)
            require(archiveRoot.resolve("bbl").setExecutable(true, false)) {
                "Unable to make staged Homebrew bbl executable"
            }
            require(archiveRoot.resolve("bbl-search-common").setExecutable(true, false)) {
                "Unable to make staged Homebrew bbl-search-common executable"
            }
            val process = ProcessBuilder(
                "tar", "-czf", archive.absolutePath,
                "bbl", "bbl-search-common", "webus.zip",
            )
                .directory(archiveRoot)
                .inheritIO()
                .apply { environment()["COPYFILE_DISABLE"] = "1" }
                .start()
            require(process.waitFor() == 0) { "Failed to create ${archive.absolutePath}" }
            archive.copyTo(output.resolve("bbl.tar.gz"), overwrite = true)

            formulaDirectory.resolve("bbl.rb").writeText(
                """
                class Bbl < Formula
                  desc "$$bblDescription"
                  homepage "$bblGitHubRepositoryUrl"
                  version "$version"
                  license "Apache-2.0"

                  url "file://__BBL_HOMEBREW_ARCHIVE__"
                  sha256 "${archive.sha256Hex()}"

                  def install
                    libexec.install "bbl", "bbl-search-common"
                    (prefix/"packs").install "webus.zip"
                    (bin/"bbl").write <<~SH
                      #!/bin/bash
                      set -e
                      mkdir -p "${'$'}HOME/.bbl/bin" "${'$'}HOME/.bbl/packs"
                      if ! cmp -s "#{libexec}/bbl-search-common" "${'$'}HOME/.bbl/bin/bbl-search-common"; then
                        install -m 0755 "#{libexec}/bbl-search-common" "${'$'}HOME/.bbl/bin/bbl-search-common"
                      fi
                      if ! cmp -s "#{prefix}/packs/webus.zip" "${'$'}HOME/.bbl/packs/webus.zip"; then
                        install -m 0644 "#{prefix}/packs/webus.zip" "${'$'}HOME/.bbl/packs/webus.zip"
                      fi
                      exec "#{libexec}/bbl" "${'$'}@"
                    SH
                  end

                  test do
                    assert_match(/God|god/, shell_output("#{bin}/bbl john 3:16"))
                    assert_match(/God|god/, shell_output("#{bin}/bbl search God limit 1"))
                  end
                end
                """.trimIndent() + "\n"
            )
            bblInstallCommonFixtureDirectory.get().asFile.resolve("version.txt")
                .copyTo(output.resolve("version.txt"), overwrite = true)
        }
    }
}

val hostMacosTaskNamePart = if (System.getProperty("os.arch") == "aarch64") "MacosArm64" else "MacosX64"
tasks.register("buildMacosPkg") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build the unsigned macOS .pkg installer for the current host architecture."
    dependsOn("build${hostMacosTaskNamePart}Pkg")
}
tasks.register("stageBblInstallMacosPkgFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the macOS .pkg fixture for the current host architecture."
    dependsOn("stageBblInstall${hostMacosTaskNamePart}PkgFixture")
}

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

val bblDebInstallUser = providers.gradleProperty("bblDebInstallUser")
    .orElse(providers.environmentVariable("USER"))
val bblDebInstallGroup = providers.gradleProperty("bblDebInstallGroup")
    .orElse(bblDebInstallUser)
val bblDebInstallHome = providers.gradleProperty("bblDebInstallHome")
    .orElse(providers.environmentVariable("HOME"))
    .orElse(bblDebInstallUser.map { "/home/$it" })
val linuxDebOutputFile = bblVersionProvider.flatMap { version ->
    linuxDebOutputDirectory.map { it.file("bbl-$version-linux-amd64.deb") }
}
val bblRpmInstallUser = providers.gradleProperty("bblRpmInstallUser")
    .orElse("fedora")
val bblRpmInstallGroup = providers.gradleProperty("bblRpmInstallGroup")
    .orElse(bblRpmInstallUser)
val bblRpmInstallHome = providers.gradleProperty("bblRpmInstallHome")
    .orElse("/home/fedora")
val linuxRpmOutputFile = bblVersionProvider.flatMap { version ->
    linuxDebOutputDirectory.map { it.file("bbl-$version-linux-x86_64.rpm") }
}
val bblArchInstallUser = providers.gradleProperty("bblArchInstallUser")
    .orElse("arch")
val bblArchInstallGroup = providers.gradleProperty("bblArchInstallGroup")
    .orElse(bblArchInstallUser)
val bblArchInstallHome = providers.gradleProperty("bblArchInstallHome")
    .orElse("/home/arch")
val linuxArchlinuxOutputFile = bblVersionProvider.flatMap { version ->
    linuxDebOutputDirectory.map { it.file("bbl-$version-linux-x86_64.pkg.tar.zst") }
}

fun String.asYamlString(): String = "'${replace("'", "''")}'"

val buildLinuxDeb = tasks.register<Exec>("buildLinuxDeb") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build the Linux amd64 .deb installer for bbl using nFPM."
    notCompatibleWithConfigurationCache("Generates an nFPM config from script-scoped providers.")
    onlyIf("Linux-only package task") {
        System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    }
    dependsOn(
        "stageBblInstallLinuxCliCoreFixture",
        "stageBblInstallLinuxCliSearchCommonFixture",
        stageBblInstallVersionFixture,
    )

    val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/linux/cli-core/bbl")
    val stagedSearchCommon = layout.buildDirectory.file(
        "bblInstallFixtures/linux/cli-search-common/bbl-search-common"
    )
    val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
    val nfpmConfig = layout.buildDirectory.file("nfpm/deb-amd64/nfpm.yaml")

    inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
    inputs.property("bblVersion", bblVersionProvider)
    inputs.property("bblDebInstallUser", bblDebInstallUser)
    inputs.property("bblDebInstallGroup", bblDebInstallGroup)
    inputs.property("bblDebInstallHome", bblDebInstallHome)
    outputs.file(linuxDebOutputFile)

    doFirst {
        val installUser = bblDebInstallUser.orNull
            ?: error("bblDebInstallUser is required. Set -PbblDebInstallUser or the USER environment variable.")
        val installGroup = bblDebInstallGroup.get()
        val installHome = bblDebInstallHome.get()
        require(installHome.startsWith("/")) {
            "bblDebInstallHome must be an absolute path: $installHome"
        }

        val checkNfpm = ProcessBuilder("nfpm", "--version").inheritIO().start().waitFor()
        require(checkNfpm == 0) {
            "nFPM is required. Install it from https://nfpm.goreleaser.com/docs/install/"
        }

        val bbl = stagedBbl.get().asFile
        val searchCommon = stagedSearchCommon.get().asFile
        val webusPack = stagedWebusPack.asFile
        require(bbl.isFile) { "Missing staged bbl binary: ${bbl.absolutePath}" }
        require(searchCommon.isFile) {
            "Missing staged bbl-search-common binary: ${searchCommon.absolutePath}"
        }
        require(webusPack.isFile) { "Missing webus pack: ${webusPack.absolutePath}" }
        require(bbl.setExecutable(true, false)) { "Unable to make ${bbl.absolutePath} executable" }
        require(searchCommon.setExecutable(true, false)) {
            "Unable to make ${searchCommon.absolutePath} executable"
        }

        val configFile = nfpmConfig.get().asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            name: bbl
            arch: amd64
            platform: linux
            version: ${bblVersionProvider.get().asYamlString()}
            version_schema: semver
            release: "1"
            section: utils
            priority: optional
            maintainer: "$bblAuthorName <$bblAuthorEmail>"
            homepage: "$bblGitHubRepositoryUrl"
            license: "Apache-2.0"
            description: |-
              $bblDescription
            umask: 0o002
            contents:
              - src: ${bbl.absolutePath.asYamlString()}
                dst: /usr/bin/bbl
                file_info:
                  mode: 0755
                  owner: root
                  group: root
              - dst: ${(installHome + "/.bbl").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${(installHome + "/.bbl/bin").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${(installHome + "/.bbl/packs").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${searchCommon.absolutePath.asYamlString()}
                dst: ${(installHome + "/.bbl/bin/bbl-search-common").asYamlString()}
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${webusPack.absolutePath.asYamlString()}
                dst: ${(installHome + "/.bbl/packs/webus.zip").asYamlString()}
                file_info:
                  mode: 0644
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
            """.trimIndent() + "\n"
        )

        linuxDebOutputDirectory.get().asFile.mkdirs()
        commandLine(
            "nfpm", "package",
            "--config", configFile.absolutePath,
            "--packager", "deb",
            "--target", linuxDebOutputFile.get().asFile.absolutePath,
        )
    }
}

tasks.register<Copy>("stageBblInstallLinuxDebFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Linux amd64 .deb installer fixture for Kitchen tests."
    dependsOn(buildLinuxDeb, stageBblInstallVersionFixture)
    into(layout.buildDirectory.dir("bblInstallFixtures/linux/deb"))
    from(linuxDebOutputFile) { rename { "bbl.deb" } }
    from(bblInstallCommonFixtureDirectory) { include("version.txt") }
}

val buildLinuxRpm = tasks.register<Exec>("buildLinuxRpm") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build the Linux x86_64 .rpm installer for bbl using nFPM."
    notCompatibleWithConfigurationCache("Generates an nFPM config from script-scoped providers.")
    onlyIf("Linux-only package task") {
        System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    }
    dependsOn(
        "stageBblInstallLinuxCliCoreFixture",
        "stageBblInstallLinuxCliSearchCommonFixture",
        stageBblInstallVersionFixture,
    )

    val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/linux/cli-core/bbl")
    val stagedSearchCommon = layout.buildDirectory.file(
        "bblInstallFixtures/linux/cli-search-common/bbl-search-common"
    )
    val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
    val nfpmConfig = layout.buildDirectory.file("nfpm/rpm-x86_64/nfpm.yaml")

    inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
    inputs.property("bblVersion", bblVersionProvider)
    inputs.property("bblRpmInstallUser", bblRpmInstallUser)
    inputs.property("bblRpmInstallGroup", bblRpmInstallGroup)
    inputs.property("bblRpmInstallHome", bblRpmInstallHome)
    outputs.file(linuxRpmOutputFile)

    doFirst {
        val installUser = bblRpmInstallUser.get()
        val installGroup = bblRpmInstallGroup.get()
        val installHome = bblRpmInstallHome.get()
        require(installHome.startsWith("/")) {
            "bblRpmInstallHome must be an absolute path: $installHome"
        }

        val checkNfpm = ProcessBuilder("nfpm", "--version").inheritIO().start().waitFor()
        require(checkNfpm == 0) {
            "nFPM is required. Install it from https://nfpm.goreleaser.com/docs/install/"
        }

        val bbl = stagedBbl.get().asFile
        val searchCommon = stagedSearchCommon.get().asFile
        val webusPack = stagedWebusPack.asFile
        require(bbl.isFile) { "Missing staged bbl binary: ${bbl.absolutePath}" }
        require(searchCommon.isFile) {
            "Missing staged bbl-search-common binary: ${searchCommon.absolutePath}"
        }
        require(webusPack.isFile) { "Missing webus pack: ${webusPack.absolutePath}" }
        require(bbl.setExecutable(true, false)) { "Unable to make ${bbl.absolutePath} executable" }
        require(searchCommon.setExecutable(true, false)) {
            "Unable to make ${searchCommon.absolutePath} executable"
        }

        val configFile = nfpmConfig.get().asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            name: bbl
            arch: amd64
            platform: linux
            version: ${bblVersionProvider.get().asYamlString()}
            version_schema: semver
            release: "1"
            section: utils
            priority: optional
            maintainer: "$bblAuthorName <$bblAuthorEmail>"
            homepage: "$bblGitHubRepositoryUrl"
            license: "Apache-2.0"
            description: |-
              $bblDescription
            umask: 0o002
            rpm:
              group: Applications/System
            contents:
              - src: ${bbl.absolutePath.asYamlString()}
                dst: /usr/bin/bbl
                file_info:
                  mode: 0755
                  owner: root
                  group: root
              - dst: ${(installHome + "/.bbl").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${(installHome + "/.bbl/bin").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${(installHome + "/.bbl/packs").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${searchCommon.absolutePath.asYamlString()}
                dst: ${(installHome + "/.bbl/bin/bbl-search-common").asYamlString()}
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${webusPack.absolutePath.asYamlString()}
                dst: ${(installHome + "/.bbl/packs/webus.zip").asYamlString()}
                file_info:
                  mode: 0644
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
            """.trimIndent() + "\n"
        )

        linuxDebOutputDirectory.get().asFile.mkdirs()
        commandLine(
            "nfpm", "package",
            "--config", configFile.absolutePath,
            "--packager", "rpm",
            "--target", linuxRpmOutputFile.get().asFile.absolutePath,
        )
    }
}

tasks.register<Copy>("stageBblInstallLinuxRpmFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Linux x86_64 .rpm installer fixture for Kitchen tests."
    dependsOn(buildLinuxRpm, stageBblInstallVersionFixture)
    into(layout.buildDirectory.dir("bblInstallFixtures/linux/rpm"))
    from(linuxRpmOutputFile) { rename { "bbl.rpm" } }
    from(bblInstallCommonFixtureDirectory) { include("version.txt") }
}

val buildLinuxArchlinux = tasks.register<Exec>("buildLinuxArchlinux") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build the Linux x86_64 Arch Linux pacman package for bbl using nFPM."
    notCompatibleWithConfigurationCache("Generates an nFPM config from script-scoped providers.")
    onlyIf("Linux-only package task") {
        System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    }
    dependsOn(
        "stageBblInstallLinuxCliCoreFixture",
        "stageBblInstallLinuxCliSearchCommonFixture",
        stageBblInstallVersionFixture,
    )

    val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/linux/cli-core/bbl")
    val stagedSearchCommon = layout.buildDirectory.file(
        "bblInstallFixtures/linux/cli-search-common/bbl-search-common"
    )
    val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
    val nfpmConfig = layout.buildDirectory.file("nfpm/archlinux-x86_64/nfpm.yaml")
    val postInstallScript = layout.buildDirectory.file("nfpm/archlinux-x86_64/postinstall.sh")

    inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
    inputs.property("bblVersion", bblVersionProvider)
    inputs.property("bblArchInstallUser", bblArchInstallUser)
    inputs.property("bblArchInstallGroup", bblArchInstallGroup)
    inputs.property("bblArchInstallHome", bblArchInstallHome)
    outputs.file(linuxArchlinuxOutputFile)

    doFirst {
        val installUser = bblArchInstallUser.get()
        val installGroup = bblArchInstallGroup.get()
        val installHome = bblArchInstallHome.get()
        require(installHome.startsWith("/")) {
            "bblArchInstallHome must be an absolute path: $installHome"
        }

        val checkNfpm = ProcessBuilder("nfpm", "--version").inheritIO().start().waitFor()
        require(checkNfpm == 0) {
            "nFPM is required. Install it from https://nfpm.goreleaser.com/docs/install/"
        }

        val bbl = stagedBbl.get().asFile
        val searchCommon = stagedSearchCommon.get().asFile
        val webusPack = stagedWebusPack.asFile
        require(bbl.isFile) { "Missing staged bbl binary: ${bbl.absolutePath}" }
        require(searchCommon.isFile) {
            "Missing staged bbl-search-common binary: ${searchCommon.absolutePath}"
        }
        require(webusPack.isFile) { "Missing webus pack: ${webusPack.absolutePath}" }
        require(bbl.setExecutable(true, false)) { "Unable to make ${bbl.absolutePath} executable" }
        require(searchCommon.setExecutable(true, false)) {
            "Unable to make ${searchCommon.absolutePath} executable"
        }

        val configFile = nfpmConfig.get().asFile
        configFile.parentFile.mkdirs()
        val postInstallFile = postInstallScript.get().asFile
        postInstallFile.writeText(
            """
            #!/bin/sh
            chown -R ${installUser.asYamlString()}:${installGroup.asYamlString()} ${installHome.asYamlString()}/.bbl
            """.trimIndent() + "\n"
        )
        configFile.writeText(
            """
            name: bbl
            arch: amd64
            platform: linux
            version: ${bblVersionProvider.get().asYamlString()}
            version_schema: semver
            release: "1"
            maintainer: "$bblAuthorName <$bblAuthorEmail>"
            homepage: "$bblGitHubRepositoryUrl"
            license: "Apache-2.0"
            description: |-
              $bblDescription
            umask: 0o002
            archlinux:
              arch: x86_64
              packager: "$bblAuthorName <$bblAuthorEmail>"
            scripts:
              postinstall: ${postInstallFile.absolutePath.asYamlString()}
            contents:
              - src: ${bbl.absolutePath.asYamlString()}
                dst: /usr/bin/bbl
                file_info:
                  mode: 0755
                  owner: root
                  group: root
              - dst: ${("$installHome/.bbl").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${("$installHome/.bbl/bin").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - dst: ${("$installHome/.bbl/packs").asYamlString()}
                type: dir
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${searchCommon.absolutePath.asYamlString()}
                dst: ${("$installHome/.bbl/bin/bbl-search-common").asYamlString()}
                file_info:
                  mode: 0755
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
              - src: ${webusPack.absolutePath.asYamlString()}
                dst: ${("$installHome/.bbl/packs/webus.zip").asYamlString()}
                file_info:
                  mode: 0644
                  owner: ${installUser.asYamlString()}
                  group: ${installGroup.asYamlString()}
            """.trimIndent() + "\n"
        )

        linuxDebOutputDirectory.get().asFile.mkdirs()
        commandLine(
            "nfpm", "package",
            "--config", configFile.absolutePath,
            "--packager", "archlinux",
            "--target", linuxArchlinuxOutputFile.get().asFile.absolutePath,
        )
    }
}

tasks.register<Sync>("stageBblInstallLinuxArchlinuxFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Linux x86_64 Arch Linux pacman package fixture for Kitchen tests."
    dependsOn(buildLinuxArchlinux, stageBblInstallVersionFixture)
    into(layout.buildDirectory.dir("bblInstallFixtures/linux/archlinux"))
    from(linuxArchlinuxOutputFile) { rename { "bbl.pkg.tar.zst" } }
    from(bblInstallCommonFixtureDirectory) { include("version.txt") }
}

val stageBblInstallCompletionFixtureTasks = bblInstallPlatforms
    .map { platform ->
        val cliCoreFixtureTask = stageBblInstallFixtureTasks.single {
            it.name == "stageBblInstall${platform.taskNamePart}CliCoreFixture"
        }
        val completionOutputDirectory = layout.buildDirectory.dir("bblInstallFixtures/${platform.id}/cli-core")
        val executableSuffix = if (platform.id == "windows") ".exe" else ""
        val bblExecutable = completionOutputDirectory.map { it.file("bbl$executableSuffix") }

        tasks.register("stageBblInstall${platform.taskNamePart}CliCoreCompletionFixtures") {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Generate ${platform.id} shell completion fixtures for bbl_install Kitchen tests."

            dependsOn(cliCoreFixtureTask)

            inputs.file(bblExecutable)
            val outputsList = mutableListOf(
                completionOutputDirectory.map { it.file("bbl.bash") },
                completionOutputDirectory.map { it.file("_bbl") },
                completionOutputDirectory.map { it.file("bbl.fish") },
            )
            if (platform.id == "windows") {
                outputsList.add(completionOutputDirectory.map { it.file("_bbl.ps1") })
            }
            outputs.files(outputsList)

            doLast {
                val binaryFile = bblExecutable.get().asFile
                require(binaryFile.exists()) {
                    "Expected staged bbl executable at ${binaryFile.absolutePath}"
                }
                if (platform.id != "windows") {
                    binaryFile.setExecutable(true)
                }

                val shells = mutableListOf(
                    "bash" to "bbl.bash",
                    "zsh" to "_bbl",
                    "fish" to "bbl.fish",
                )
                if (platform.id == "windows") {
                    shells.add("powershell" to "_bbl.ps1")
                }

                shells.forEach { (shell, fileName) ->
                    val outputFile = completionOutputDirectory.get().file(fileName).asFile
                    outputFile.parentFile.mkdirs()
                    val process = ProcessBuilder(binaryFile.absolutePath, "generate-completion", shell)
                        .redirectOutput(outputFile)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()
                    val exitCode = process.waitFor()
                    require(exitCode == 0) {
                        "Failed to generate $shell completion for ${binaryFile.absolutePath} (exit $exitCode)"
                    }
                    require(outputFile.length() > 0L) {
                        "Generated completion file is empty: ${outputFile.absolutePath}"
                    }

                    // bash 3.2 (macOS default) does not support "compgen -F".
                    // Replace the Clikt-generated pattern:
                    //   COMPREPLY=($(compgen -F function_name 2>/dev/null))
                    // with a direct function call so completions work on all bash versions.
                    if (shell == "bash") {
                        val fixed = outputFile.readText().replace(Regex("""COMPREPLY=\(\$\(compgen -F (\w+) 2>/dev/null\)\)""")) {
                            it.groupValues[1]
                        }
                        outputFile.writeText(fixed)
                    }
                }
            }
        }
    }

fun Sync.prepareBblInstallCookbookFiles(platform: BblInstallPlatform) {
    val platformFixtureTasks = stageBblInstallFixtureTasks.filter { it.name.contains(platform.taskNamePart) }
    val platformCompletionFixtureTasks = stageBblInstallCompletionFixtureTasks.filter {
        it.name.contains(platform.taskNamePart)
    }
    dependsOn(platformFixtureTasks)
    dependsOn(platformCompletionFixtureTasks)
    dependsOn(stageBblInstallVersionFixture)

    into(layout.projectDirectory.dir("bbl_install/files"))
    from(layout.buildDirectory.dir("bblInstallFixtures/${platform.id}")) {
        exclude("pkg/**")
        exclude("**/version.txt")
    }
    from(bblInstallCommonFixtureDirectory)
    include("**/*")
    eachFile {
        relativePath = RelativePath(true, name)
    }
    includeEmptyDirs = false
    outputs.upToDateWhen { false }
    preserve {
        include("README.md")
        include("config_posix_test.sh")
        include("history_posix_test.sh")
        include("search_posix_test.sh")
        include("completion_posix_test.sh")
    }
}

tasks.register<Sync>("stageBblInstallLinuxFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
    dependsOn("stageBblInstallLinuxDebFixture")
    dependsOn("stageBblInstallLinuxRpmFixture")
    dependsOn("stageBblInstallLinuxArchlinuxFixture")
}

tasks.register<Sync>("stageBblInstallLinuxCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
    dependsOn("stageBblInstallLinuxDebFixture")
    dependsOn("stageBblInstallLinuxRpmFixture")
    dependsOn("stageBblInstallLinuxArchlinuxFixture")
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
    dependsOn("stageBblInstallMacosArm64PkgFixture")
    from(layout.buildDirectory.file("bblInstallFixtures/macosArm64/pkg/bbl.pkg"))
}

tasks.register<Sync>("stageBblInstallMacosX64CliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all macOS X64 CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "macosX64" })
    dependsOn("stageBblInstallMacosX64PkgFixture")
    from(layout.buildDirectory.file("bblInstallFixtures/macosX64/pkg/bbl.pkg"))
}

tasks.register("stageBblInstallFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks)
    dependsOn(stageBblInstallCompletionFixtureTasks)
}

tasks.register("stageBblInstallCommonFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage Linux and Windows fixture files for bbl_install Kitchen tests."
    dependsOn(stageBblInstallFixtureTasks.filter { !it.name.contains("Macos") })
    dependsOn(stageBblInstallCompletionFixtureTasks.filter { !it.name.contains("Macos") })
}
