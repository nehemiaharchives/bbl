# Codex CLI Implementation Prompt: Add Windows WinGet Local Manifest Package Pipeline for `bbl-kmp`

Use this prompt from the root of the `bbl-kmp` repository.

---

## Goal

Implement a first-class Windows WinGet local-manifest package pipeline for `bbl`, parallel to the existing Windows CLI fixture tests and the Linux/macOS package installer E2E tests.

The required flow is:

```text
GitHub Actions windows-latest / local Windows machine
  -> build Windows x64 Kotlin/Native binaries
  -> create a local WinGet portable ZIP package fixture
  -> generate a multi-file WinGet manifest fixture
  -> stage ZIP + manifest + version.txt via a Gradle task
  -> upload/download fixture in CI
  -> copy WinGet fixture into bbl_install/files
  -> run Test Kitchen windows-local suite
  -> enable LocalManifestFiles
  -> validate manifest with winget validate --manifest <manifest-dir>
  -> install with winget install --manifest <manifest-dir>
  -> run E2E verification with InSpec
```

This is a **local WinGet manifest / local package fixture** task. It is **not** a public `microsoft/winget-pkgs` submission task yet.

The implementation must be safe for repeated local and CI runs. It must clean up any existing `bbl` WinGet portable install before installing, and it must avoid polluting the developer machine more than the existing `windows-local` Kitchen tests already do.

---

## Source docs to keep open while implementing

Read these before editing:

```text
https://github.com/microsoft/winget-create
https://github.com/microsoft/winget-create/blob/main/doc/new.md
https://github.com/microsoft/winget-pkgs/blob/master/doc/README.md
https://github.com/microsoft/winget-pkgs/blob/master/doc/Authoring.md
https://github.com/microsoft/winget-pkgs/tree/master/doc
https://github.com/microsoft/winget-pkgs/blob/master/doc/manifest/schema/1.10.0/version.md
https://github.com/microsoft/winget-pkgs/blob/master/doc/manifest/schema/1.10.0/defaultLocale.md
https://github.com/microsoft/winget-pkgs/blob/master/doc/manifest/schema/1.10.0/installer.md
https://learn.microsoft.com/en-us/windows/package-manager/winget/install
https://learn.microsoft.com/en-us/windows/package-manager/winget/validate
https://learn.microsoft.com/en-us/windows/package-manager/winget/hash
```

Key facts from the docs:

1. Winget manifests are YAML metadata files used by Windows Package Manager to install and upgrade software.
2. Public community submissions should be multi-file manifests: version, defaultLocale, and installer.
3. Local manifest testing requires `winget settings --enable LocalManifestFiles` from an administrative shell.
4. Local manifest install is done with `winget install --manifest <path>`.
5. `winget validate --manifest <path>` validates a manifest.
6. `winget hash --file <file>` computes the installer SHA256.
7. For archive installers such as ZIP, `NestedInstallerType` and `NestedInstallerFiles` are required.
8. `PortableCommandAlias` is valid only when `NestedInstallerType` is `portable`.
9. WingetCreate is useful for authoring manifests, but its `new` flow is interactive. Do not make CI depend on an interactive wizard.
10. If WingetCreate cannot generate the exact local fixture non-interactively, generate deterministic YAML in Gradle and use Winget/WingetCreate only for validation and developer regeneration helpers.

Do not blindly force WingetCreate into CI if the installed version only supports interactive manifest creation for the required metadata. CI must be deterministic.

---

## Current repo facts to preserve

Before editing, inspect the current files:

```powershell
git status --short
Get-Content build.gradle.kts -TotalCount 1300
Get-Content bbl_install\kitchen.yml
Get-Content bbl_install\attributes\default.rb
Get-Content bbl_install\attributes\windows.rb
Get-Content bbl_install\recipes\install_windows.rb
Get-Content bbl_install\support\cleanup-windows-local.ps1
Get-Content .github\workflows\ci.yml -TotalCount 1200
Get-ChildItem bbl_install\recipes | Sort-Object Name
Get-ChildItem bbl_install\test\integration -Recurse | Sort-Object FullName
```

Important current facts:

1. `build.gradle.kts` already models Windows as `BblInstallPlatform("windows", "Windows", "mingwX64", "MingwX64", ".exe")`.
2. Windows CLI fixture staging already copies `bbl.exe` and helper executables from `mingwX64/debugExecutable`.
3. `bblInstallBinaries` includes:
   - `bbl`
   - `bbl-search-common`
   - `bbl-search-extra`
   - `bbl-search-kuromoji`
   - `bbl-search-morfologik`
   - `bbl-search-nori`
   - `bbl-search-smartcn`
4. `cli-core` has `includePacks = true`, so Windows CLI core fixtures include pack ZIPs.
5. `stageBblInstallWindowsFixtures` and `stageBblInstallWindowsCliAllFixture` already prepare Windows cookbook fixture files.
6. `bbl_install/kitchen.yml` already has a `windows-local` platform using the local driver, exec transport, and Chef Infra.
7. Existing Windows E2E rows in `.github/workflows/ci.yml` use `windows-latest`, download `bbl-install-windows-*-files`, flatten them into `bbl_install/files`, then run Kitchen against `windows-local`.
8. `bbl_install/attributes/windows.rb` currently puts manual Windows installs under:
   - `%LOCALAPPDATA%\Programs\bbl` for `bbl.exe`
   - `%USERPROFILE%\.bbl\bin` for helpers
   - `%USERPROFILE%\.bbl\packs` for packs
9. `bbl_install/support/cleanup-windows-local.ps1` already removes `%LOCALAPPDATA%\Programs\bbl`, `%USERPROFILE%\.bbl`, and related PATH entries after `windows-local` Kitchen destroy.

Preserve existing Windows tests. Add WinGet as a sibling installer suite.

---

## Design decision: local portable ZIP + multi-file manifest

Use a WinGet portable ZIP package fixture.

Use this package identifier:

```text
Gnit.Bbl
```

Use this package name:

```text
bbl
```

Use this moniker:

```text
bbl
```

Use this manifest schema version:

```text
1.10.0
```

Use this fixture directory:

```text
build/bblInstallFixtures/windows/winget
```

Stage these files:

```text
build/bblInstallFixtures/windows/winget/bbl-winget.zip
build/bblInstallFixtures/windows/winget/manifests/g/Gnit/Bbl/<version>/Gnit.Bbl.yaml
build/bblInstallFixtures/windows/winget/manifests/g/Gnit/Bbl/<version>/Gnit.Bbl.locale.en-US.yaml
build/bblInstallFixtures/windows/winget/manifests/g/Gnit/Bbl/<version>/Gnit.Bbl.installer.yaml
build/bblInstallFixtures/windows/winget/version.txt
```

Use this Kitchen suite name:

```text
winget_install_windows
```

Expected Kitchen instance name:

```text
winget-install-windows-windows-local
```

Confirm the final name with:

```powershell
cd bbl_install
bundle exec kitchen list
```

Use a local HTTP URL placeholder in the generated installer manifest:

```text
__BBL_WINGET_INSTALLER_URL__
```

Reason: community manifests use URLs, and `wingetcreate new` also expects installer URLs. Do not assume `file:///...` InstallerUrl behavior. In the Kitchen recipe, copy the staged manifest to a temp working directory, replace the placeholder with a local loopback HTTP URL serving `bbl-winget.zip`, then run `winget validate` and `winget install --manifest`.

Recommended local URL shape:

```text
http://127.0.0.1:<free-port>/bbl-winget.zip
```

This keeps CI deterministic and avoids publishing anything.

---

## Step 1: Add Gradle helpers for WinGet manifest generation

Edit `build.gradle.kts`.

Add imports if needed:

```kotlin
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
```

You already have `java.security.MessageDigest` and ZIP imports.

Add helper functions near `asYamlString()`:

```kotlin
fun String.asWingetYamlString(): String = "'${replace("'", "''")}'"

fun File.sha256UpperHex(): String = sha256Hex().uppercase()
```

If `sha256Hex()` already exists, reuse it.

---

## Step 2: Add WinGet fixture Gradle task

Add this task near the other Windows fixture/package tasks, after the generic Windows CLI fixture tasks are registered and before aggregate `stageBblInstallWindowsFixtures` is finalized if possible.

Task name:

```text
stageBblInstallWindowsWingetFixture
```

The task must depend on:

```text
stageBblInstallWindowsCliCoreFixture
stageBblInstallWindowsCliSearchCommonFixture
stageBblInstallWindowsCliSearchExtraFixture
stageBblInstallWindowsCliSearchKuromojiFixture
stageBblInstallWindowsCliSearchMorfologikFixture
stageBblInstallWindowsCliSearchNoriFixture
stageBblInstallWindowsCliSearchSmartcnFixture
stageBblInstallVersionFixture
```

Suggested implementation:

```kotlin
val bblWingetPackageIdentifier = "Gnit.Bbl"
val bblWingetPublisher = "GNIT"
val bblWingetPackageName = "bbl"
val bblWingetLocale = "en-US"
val bblWingetManifestVersion = "1.10.0"

val windowsWingetFixtureDirectory = layout.buildDirectory.dir("bblInstallFixtures/windows/winget")
val windowsWingetArchiveFile = windowsWingetFixtureDirectory.map { it.file("bbl-winget.zip") }

tasks.register("stageBblInstallWindowsWingetFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Windows WinGet local manifest + portable ZIP fixture for Kitchen tests."
    notCompatibleWithConfigurationCache("Creates local WinGet manifests and ZIP from script-scoped providers.")

    dependsOn(
        "stageBblInstallWindowsCliCoreFixture",
        "stageBblInstallWindowsCliSearchCommonFixture",
        "stageBblInstallWindowsCliSearchExtraFixture",
        "stageBblInstallWindowsCliSearchKuromojiFixture",
        "stageBblInstallWindowsCliSearchMorfologikFixture",
        "stageBblInstallWindowsCliSearchNoriFixture",
        "stageBblInstallWindowsCliSearchSmartcnFixture",
        stageBblInstallVersionFixture,
    )

    val stagedCliCoreDir = layout.buildDirectory.dir("bblInstallFixtures/windows/cli-core")
    val stagedHelperDirs = listOf(
        "cli-search-common" to "bbl-search-common.exe",
        "cli-search-extra" to "bbl-search-extra.exe",
        "cli-search-kuromoji" to "bbl-search-kuromoji.exe",
        "cli-search-morfologik" to "bbl-search-morfologik.exe",
        "cli-search-nori" to "bbl-search-nori.exe",
        "cli-search-smartcn" to "bbl-search-smartcn.exe",
    )

    inputs.dir(stagedCliCoreDir)
    inputs.property("bblVersion", bblVersionProvider)
    outputs.dir(windowsWingetFixtureDirectory)

    doLast {
        val version = bblVersionProvider.get()
        val outputDir = windowsWingetFixtureDirectory.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val packageRoot = temporaryDir.resolve("winget-package")
        packageRoot.deleteRecursively()
        packageRoot.mkdirs()

        val cliCore = stagedCliCoreDir.get().asFile
        val bblExe = cliCore.resolve("bbl.exe")
        require(bblExe.isFile) { "Missing staged bbl.exe: ${bblExe.absolutePath}" }
        bblExe.copyTo(packageRoot.resolve("bbl.exe"), overwrite = true)

        stagedHelperDirs.forEach { (fixtureId, fileName) ->
            val source = layout.buildDirectory.file("bblInstallFixtures/windows/$fixtureId/$fileName").get().asFile
            require(source.isFile) { "Missing staged helper: ${source.absolutePath}" }
            source.copyTo(packageRoot.resolve(fileName), overwrite = true)
        }

        val webusPack = cliCore.resolve("webus.zip")
        require(webusPack.isFile) { "Missing staged webus.zip: ${webusPack.absolutePath}" }
        webusPack.copyTo(packageRoot.resolve("webus.zip"), overwrite = true)

        val archive = windowsWingetArchiveFile.get().asFile
        archive.parentFile.mkdirs()
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            packageRoot.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.relativeTo(packageRoot).invariantSeparatorsPath }
                .forEach { file ->
                    val entry = ZipEntry(file.relativeTo(packageRoot).invariantSeparatorsPath)
                    entry.time = 0L
                    zip.putNextEntry(entry)
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }

        val sha256 = archive.sha256UpperHex()
        val manifestDir = outputDir.resolve("manifests/g/Gnit/Bbl/$version")
        manifestDir.mkdirs()

        manifestDir.resolve("Gnit.Bbl.yaml").writeText(
            """
            # Created for local Kitchen/CI testing. Do not submit this file unchanged to microsoft/winget-pkgs.
            PackageIdentifier: Gnit.Bbl
            PackageVersion: ${version.asWingetYamlString()}
            DefaultLocale: en-US
            ManifestType: version
            ManifestVersion: $bblWingetManifestVersion
            """.trimIndent() + "\n"
        )

        manifestDir.resolve("Gnit.Bbl.locale.en-US.yaml").writeText(
            """
            # Created for local Kitchen/CI testing. Do not submit this file unchanged to microsoft/winget-pkgs.
            PackageIdentifier: Gnit.Bbl
            PackageVersion: ${version.asWingetYamlString()}
            PackageLocale: en-US
            Publisher: GNIT
            PublisherUrl: https://github.com/nehemiaharchives
            PublisherSupportUrl: https://github.com/nehemiaharchives/bbl-kmp/issues
            Author: GNIT
            PackageName: bbl
            PackageUrl: https://github.com/nehemiaharchives/bbl-kmp
            License: Apache-2.0
            LicenseUrl: https://github.com/nehemiaharchives/bbl-kmp/blob/master/LICENSE
            ShortDescription: Read and search the Holy Bible from the command line.
            Description: Offline command-line Bible reader and search tool.
            Moniker: bbl
            Tags:
              - bible
              - cli
              - command-line
              - search
            ManifestType: defaultLocale
            ManifestVersion: $bblWingetManifestVersion
            """.trimIndent() + "\n"
        )

        manifestDir.resolve("Gnit.Bbl.installer.yaml").writeText(
            """
            # Created for local Kitchen/CI testing. Do not submit this file unchanged to microsoft/winget-pkgs.
            PackageIdentifier: Gnit.Bbl
            PackageVersion: ${version.asWingetYamlString()}
            MinimumOSVersion: 10.0.17763.0
            InstallerType: zip
            NestedInstallerType: portable
            ArchiveBinariesDependOnPath: true
            Installers:
              - Architecture: x64
                InstallerUrl: __BBL_WINGET_INSTALLER_URL__
                InstallerSha256: $sha256
                NestedInstallerFiles:
                  - RelativeFilePath: bbl.exe
                    PortableCommandAlias: bbl
                  - RelativeFilePath: bbl-search-common.exe
                  - RelativeFilePath: bbl-search-extra.exe
                  - RelativeFilePath: bbl-search-kuromoji.exe
                  - RelativeFilePath: bbl-search-morfologik.exe
                  - RelativeFilePath: bbl-search-nori.exe
                  - RelativeFilePath: bbl-search-smartcn.exe
            ManifestType: installer
            ManifestVersion: $bblWingetManifestVersion
            """.trimIndent() + "\n"
        )

        bblInstallCommonFixtureDirectory.get().asFile.resolve("version.txt")
            .copyTo(outputDir.resolve("version.txt"), overwrite = true)
    }
}
```

Notes:

1. Keep the manifest deterministic.
2. Keep the ZIP deterministic enough for stable hash generation within a single build.
3. Use `InstallerUrl: __BBL_WINGET_INSTALLER_URL__` as a placeholder. The Kitchen recipe must patch it.
4. Include all helper executables in the ZIP. This is safer for runtime than only packaging `bbl.exe`.
5. Include `webus.zip` so `bbl john 3:16` and search tests can run offline.
6. If WinGet rejects `PortableCommandAlias: bbl`, try `PortableCommandAlias: bbl.exe`. Use the one that makes `bbl --version` work from a fresh shell.

---

## Step 3: Add WinGet fixture to aggregate Windows fixture tasks

Edit the aggregate tasks so the WinGet fixture is built when staging all Windows fixtures.

```kotlin
tasks.register<Sync>("stageBblInstallWindowsFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "windows" })
    dependsOn("stageBblInstallWindowsWingetFixture")
}

tasks.register<Sync>("stageBblInstallWindowsCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Windows CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "windows" })
    dependsOn("stageBblInstallWindowsWingetFixture")
}
```

If those tasks already exist exactly once, edit them in place and only add the dependency. Do not duplicate task registrations.

---

## Step 4: Optional WingetCreate helper task

Add a helper task for developers, but do **not** make CI depend on interactive WingetCreate.

Task name:

```text
verifyWingetCreateAvailable
```

Suggested task:

```kotlin
tasks.register<Exec>("verifyWingetCreateAvailable") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verify wingetcreate is available for manual WinGet manifest authoring."
    onlyIf("Windows-only WingetCreate check") {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    }
    commandLine("powershell", "-NoProfile", "-Command", "wingetcreate --help")
}
```

If WingetCreate has a non-interactive command in the installed version that can generate the exact manifest from local HTTP URL + metadata, add a separate optional task:

```text
generateWingetManifestWithWingetCreate
```

But keep `stageBblInstallWindowsWingetFixture` deterministic and self-contained. The docs show `wingetcreate new` starts a wizard-like manifest creation flow, so CI should not rely on it unless `wingetcreate --help` proves a fully non-interactive path exists.

---

## Step 5: Add Kitchen suite

Edit `bbl_install/kitchen.yml`.

Add a Windows suite:

```yaml
  - name: winget_install_windows
    run_list:
      - recipe[bbl_install::winget_install_windows]
    verifier:
      inspec_tests:
        - test/integration/winget_install_windows
    includes:
      - windows-local
```

Expected instance name:

```text
winget-install-windows-windows-local
```

Confirm with:

```powershell
cd bbl_install
bundle exec kitchen list
```

Do not add Linux/macOS platforms to this suite.

---

## Step 6: Add WinGet install recipe

Create:

```text
bbl_install/recipes/winget_install_windows.rb
```

The recipe must:

1. Locate `bbl_install/files/bbl-winget.zip`.
2. Locate `bbl_install/files/winget-manifests` or `bbl_install/files/manifests` depending on how CI flattening is implemented.
3. Copy the manifest to a temp working directory.
4. Replace `__BBL_WINGET_INSTALLER_URL__` with a loopback HTTP URL.
5. Start a local HTTP server serving the ZIP.
6. Enable `LocalManifestFiles`.
7. Run `winget validate --manifest <manifest-dir>`.
8. Uninstall existing `Gnit.Bbl` if present.
9. Install with `winget install --manifest <manifest-dir> --disable-interactivity --accept-package-agreements --accept-source-agreements --force`.
10. Run smoke commands.
11. Save attributes/paths for InSpec if needed.

Suggested implementation shape:

```ruby
require 'fileutils'
require 'json'
require 'tmpdir'

raise 'winget_install_windows recipe must run on Windows' unless platform_family?('windows')

cookbook_files = ::File.expand_path('../files', __dir__)
zip_source = ::File.join(cookbook_files, 'bbl-winget.zip')
manifest_source = ::File.join(cookbook_files, 'manifests')
version_source = ::File.join(cookbook_files, 'version.txt')

raise "Missing #{zip_source}" unless ::File.file?(zip_source)
raise "Missing #{manifest_source}" unless ::File.directory?(manifest_source)
raise "Missing #{version_source}" unless ::File.file?(version_source)

work_root = ::File.join(ENV['TEMP'] || ENV['TMP'] || ::Dir.tmpdir, 'bbl-winget-install')
zip_work = ::File.join(work_root, 'bbl-winget.zip')
manifest_work = ::File.join(work_root, 'manifests')
server_script = ::File.join(work_root, 'serve-winget-fixture.ps1')
server_pid_file = ::File.join(work_root, 'server.pid')
port_file = ::File.join(work_root, 'server-port.txt')
attrs_file = ::File.join(work_root, 'bbl-winget-test-attributes.json')

ruby_block 'prepare bbl winget fixture working directory' do
  block do
    ::FileUtils.rm_rf(work_root)
    ::FileUtils.mkdir_p(work_root)
    ::FileUtils.cp(zip_source, zip_work)
    ::FileUtils.cp_r(manifest_source, manifest_work)
  end
end

ruby_block 'write bbl winget local http server script' do
  block do
    ::File.write(server_script, <<~'PS1')
      param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$PortFile
      )

      $ErrorActionPreference = 'Stop'
      $listener = [System.Net.HttpListener]::new()
      $port = 17851
      while ($true) {
        try {
          $prefix = "http://127.0.0.1:$port/"
          $listener.Prefixes.Clear()
          $listener.Prefixes.Add($prefix)
          $listener.Start()
          Set-Content -LiteralPath $PortFile -Value $port -Encoding ascii
          break
        } catch {
          $port++
          if ($port -gt 17950) { throw }
        }
      }

      try {
        while ($listener.IsListening) {
          $context = $listener.GetContext()
          try {
            $name = [System.IO.Path]::GetFileName($context.Request.Url.AbsolutePath)
            if ([string]::IsNullOrWhiteSpace($name)) { $name = 'bbl-winget.zip' }
            $path = Join-Path $Root $name
            if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
              $context.Response.StatusCode = 404
              $bytes = [System.Text.Encoding]::UTF8.GetBytes("not found")
            } else {
              $context.Response.StatusCode = 200
              $context.Response.ContentType = 'application/zip'
              $bytes = [System.IO.File]::ReadAllBytes($path)
            }
            $context.Response.ContentLength64 = $bytes.Length
            $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
          } finally {
            $context.Response.OutputStream.Close()
          }
        }
      } finally {
        $listener.Stop()
      }
    PS1
  end
end

powershell_script 'start local bbl winget fixture http server' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    Remove-Item -LiteralPath '#{port_file.tr('/', '\\')}' -Force -ErrorAction SilentlyContinue
    $p = Start-Process powershell -WindowStyle Hidden -PassThru -ArgumentList @(
      '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', '#{server_script.tr('/', '\\')}',
      '-Root', '#{work_root.tr('/', '\\')}', '-PortFile', '#{port_file.tr('/', '\\')}'
    )
    Set-Content -LiteralPath '#{server_pid_file.tr('/', '\\')}' -Value $p.Id -Encoding ascii
    for ($i = 0; $i -lt 100; $i++) {
      if (Test-Path -LiteralPath '#{port_file.tr('/', '\\')}') { exit 0 }
      Start-Sleep -Milliseconds 100
    }
    throw 'Local winget fixture HTTP server did not write port file.'
  PS
end

ruby_block 'patch bbl winget manifest installer url' do
  block do
    port = ::File.read(port_file).strip
    url = "http://127.0.0.1:#{port}/bbl-winget.zip"
    installer_manifest = ::Dir.glob(::File.join(manifest_work, '**', 'Gnit.Bbl.installer.yaml')).single
    text = ::File.read(installer_manifest)
    raise 'Missing __BBL_WINGET_INSTALLER_URL__ placeholder' unless text.include?('__BBL_WINGET_INSTALLER_URL__')
    ::File.write(installer_manifest, text.gsub('__BBL_WINGET_INSTALLER_URL__', url))
    ::File.write(attrs_file, JSON.pretty_generate({
      'manifest_dir' => manifest_work,
      'installer_zip' => zip_work,
      'installer_url' => url,
      'version' => ::File.read(version_source).strip,
      'package_identifier' => 'Gnit.Bbl'
    }))
  end
end

powershell_script 'enable local winget manifests' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    winget --version
    winget settings --enable LocalManifestFiles
  PS
end

powershell_script 'validate bbl winget manifest' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    winget validate --manifest '#{manifest_work.tr('/', '\\')}' --disable-interactivity
  PS
end

powershell_script 'remove existing bbl winget install' do
  code <<~PS
    $ErrorActionPreference = 'Continue'
    winget uninstall --id Gnit.Bbl --exact --disable-interactivity --silent --force
    $ErrorActionPreference = 'Stop'
  PS
end

powershell_script 'install bbl using local winget manifest' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    winget install --manifest '#{manifest_work.tr('/', '\\')}' --disable-interactivity --accept-package-agreements --accept-source-agreements --force --verbose-logs
  PS
end

powershell_script 'smoke test bbl winget install' do
  code <<~PS
    $ErrorActionPreference = 'Stop'
    bbl --version
    bbl john 3:16
    bbl search God limit 1
    winget list --id Gnit.Bbl --exact --disable-interactivity
  PS
end
```

If `winget validate` rejects loopback HTTP or local archive malware scanning, use the exact error to adjust. Do **not** skip validation. The install command has an option `--ignore-local-archive-malware-scan`; use it only if the client demands it for local archive installation in CI, and document why in the final response.

If `winget settings --enable LocalManifestFiles` fails because the shell is not elevated, document that limitation and adjust CI/local instructions. But do not silently bypass local manifest testing.

---

## Step 7: Stop local HTTP server during cleanup

Update:

```text
bbl_install/support/cleanup-windows-local.ps1
```

Add cleanup for the WinGet local test:

```powershell
$wingetWorkRoot = Join-Path ($env:TEMP ?? $env:TMP ?? [System.IO.Path]::GetTempPath()) 'bbl-winget-install'
$serverPidFile = Join-Path $wingetWorkRoot 'server.pid'

if (Test-Path -LiteralPath $serverPidFile) {
    $pidText = Get-Content -LiteralPath $serverPidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pidText -match '^\d+$') {
        Stop-Process -Id ([int]$pidText) -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped bbl winget fixture HTTP server process $pidText"
    }
}

winget uninstall --id Gnit.Bbl --exact --disable-interactivity --silent --force 2>$null | Out-Host

if (Test-Path -LiteralPath $wingetWorkRoot) {
    Remove-Item -LiteralPath $wingetWorkRoot -Recurse -Force
    Write-Host "Removed $wingetWorkRoot"
}
```

Keep existing cleanup behavior intact.

---

## Step 8: Add InSpec tests for WinGet install

Create:

```text
bbl_install/test/integration/winget_install_windows/default_test.rb
```

Suggested test:

```ruby
control 'bbl-winget-manifest-and-package' do
  impact 1.0
  title 'bbl WinGet local manifest and package fixture are present'

  describe file(File.join(ENV['TEMP'] || ENV['TMP'], 'bbl-winget-install', 'bbl-winget.zip')) do
    it { should exist }
    it { should be_file }
  end

  describe file(File.join(ENV['TEMP'] || ENV['TMP'], 'bbl-winget-install', 'manifests')) do
    it { should exist }
    it { should be_directory }
  end

  describe command('powershell -NoProfile -Command "Get-ChildItem -Recurse $env:TEMP\\bbl-winget-install\\manifests | Select-Object -ExpandProperty FullName"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Gnit\.Bbl\.yaml/) }
    its('stdout') { should match(/Gnit\.Bbl\.locale\.en-US\.yaml/) }
    its('stdout') { should match(/Gnit\.Bbl\.installer\.yaml/) }
  end
end

control 'bbl-winget-installed' do
  impact 1.0
  title 'bbl is installed by WinGet from local manifest'

  describe command('winget list --id Gnit.Bbl --exact --disable-interactivity') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/Gnit\.Bbl/) }
  end

  describe command('winget validate --manifest "%TEMP%\\bbl-winget-install\\manifests" --disable-interactivity') do
    its('exit_status') { should eq 0 }
  end
end

control 'bbl-winget-runtime' do
  impact 1.0
  title 'bbl installed by WinGet runs from PATH and can read/search WEBUS'

  describe command('powershell -NoProfile -Command "bbl --version"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
  end

  describe command('powershell -NoProfile -Command "bbl list translations"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/WEBUS/i) }
  end

  describe command('powershell -NoProfile -Command "bbl john 3:16"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end

  describe command('powershell -NoProfile -Command "bbl search God limit 1"') do
    its('exit_status') { should eq 0 }
    its('stdout') { should match(/God|god/) }
  end
end

control 'bbl-winget-no-manual-install-contamination' do
  impact 1.0
  title 'WinGet install does not accidentally rely on old manual install locations'

  describe command('powershell -NoProfile -Command "if (Test-Path $env:LOCALAPPDATA\\Programs\\bbl\\bbl.exe) { exit 1 } else { exit 0 }"') do
    its('exit_status') { should eq 0 }
  end
end
```

If WinGet portable install legitimately places files under a configurable portable package root, inspect with:

```powershell
winget list --id Gnit.Bbl --exact
where.exe bbl
Get-Command bbl | Format-List *
```

Then adjust the InSpec tests to assert the real WinGet-managed location. Do not assert `%LOCALAPPDATA%\Programs\bbl` for this suite unless you intentionally configure WinGet’s portable root to that path.

---

## Step 9: Update GitHub Actions package/build matrix

Edit `.github/workflows/ci.yml`.

Add a Package row near the existing Windows CLI-core row:

```yaml
          - platform: mingw
            module_name: installer-winget
            os: windows-latest
            timeout: 75
            task: :cli:core:mingwX64Test
            needs_konan_cache: true
            fixture_task: stageBblInstallWindowsWingetFixture "-Pbblpacks.embed=false"
            fixture_artifact_name: bbl-install-windows-winget-files
            fixture_path: build/bblInstallFixtures/windows/winget
            job_type: Package
```

This package fixture must be built on Windows because it depends on `mingwX64` binaries and Windows path/runtime behavior.

Do not add `needs_nfpm`. WinGet is not nFPM.

---

## Step 10: Update GitHub Actions E2E matrix

In the E2E matrix, add:

```yaml
          - suite: winget_install_windows
            platform: mingw
            os: windows-latest
            fixture_pattern: bbl-install-windows-winget-files
            kitchen_instance: winget-install-windows-windows-local
```

Use the actual Kitchen instance name if `bundle exec kitchen list` differs.

---

## Step 11: Update CI fixture preparation for Windows

The current Windows preparation step flattens all files from `bbl_install\files\windows` into `bbl_install\files`. That is OK for CLI fixtures, but it will destroy the manifest directory structure if used blindly.

Modify the Windows preparation script so `winget_install_windows` is handled first:

```powershell
if ('${{ matrix.suite }}' -eq 'winget_install_windows') {
    $fixtureDir = 'bbl_install\files\windows\winget'

    if (-not (Test-Path "$fixtureDir\bbl-winget.zip")) { throw "Missing $fixtureDir\bbl-winget.zip" }
    if (-not (Test-Path "$fixtureDir\manifests")) { throw "Missing $fixtureDir\manifests" }
    if (-not (Test-Path "$fixtureDir\version.txt")) { throw "Missing $fixtureDir\version.txt" }

    Copy-Item "$fixtureDir\bbl-winget.zip" bbl_install\files\bbl-winget.zip -Force
    Copy-Item "$fixtureDir\version.txt" bbl_install\files\version.txt -Force
    Copy-Item "$fixtureDir\manifests" bbl_install\files\manifests -Recurse -Force

    Remove-Item bbl_install\files\common -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item bbl_install\files\linux -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item bbl_install\files\macosArm64 -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item bbl_install\files\macosX64 -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item bbl_install\files\windows -Recurse -Force -ErrorAction SilentlyContinue

    if (-not (Test-Path bbl_install\files\bbl-winget.zip)) { throw 'Missing bbl_install\files\bbl-winget.zip' }
    if (-not (Test-Path bbl_install\files\manifests)) { throw 'Missing bbl_install\files\manifests' }
    if (-not (Test-Path bbl_install\files\version.txt)) { throw 'Missing bbl_install\files\version.txt' }
    Get-ChildItem bbl_install\files -Recurse
    exit 0
}
```

Keep the existing generic Windows fixture flattening for all other Windows suites.

---

## Step 12: Install/check Winget and WingetCreate in CI

For the package fixture generation job, `wingetcreate` is not required if Gradle generates deterministic YAML. But since this task is explicitly about WinGet/WingetCreate support, add a Windows-only diagnostic/install step that does not block non-WinGet jobs.

Add after checkout/setup steps:

```yaml
      - name: Check WinGet tools
        if: ${{ matrix.platform == 'mingw' && matrix.module_name == 'installer-winget' }}
        shell: pwsh
        run: |
          $ErrorActionPreference = 'Stop'
          winget --version
          winget install --id Microsoft.WingetCreate --exact --source winget --disable-interactivity --accept-package-agreements --accept-source-agreements --silent
          wingetcreate --help
```

If `Microsoft.WingetCreate` is not the correct package ID in the current winget source, run:

```powershell
winget search wingetcreate
```

and use the exact ID from the current source. Do not guess.

If installing WingetCreate on GitHub Actions is flaky, make it a best-effort diagnostic for now:

```powershell
wingetcreate --help || Write-Warning 'wingetcreate is not available; Gradle-generated deterministic manifest will still be tested with winget validate/install.'
```

Do not let WingetCreate flakiness block the deterministic local-manifest E2E unless you intentionally make WingetCreate part of the accepted build contract.

---

## Step 13: Local verification commands

From Windows PowerShell:

```powershell
./gradlew :cli:core:mingwX64Test
./gradlew stageBblInstallWindowsWingetFixture -Pbblpacks.embed=false
```

Inspect staged files:

```powershell
Get-ChildItem build\bblInstallFixtures\windows\winget -Recurse
Expand-Archive build\bblInstallFixtures\windows\winget\bbl-winget.zip -DestinationPath $env:TEMP\bbl-winget-inspect -Force
Get-ChildItem $env:TEMP\bbl-winget-inspect
```

Prepare Kitchen files manually:

```powershell
Remove-Item bbl_install\files\* -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item build\bblInstallFixtures\windows\winget\bbl-winget.zip bbl_install\files\bbl-winget.zip -Force
Copy-Item build\bblInstallFixtures\windows\winget\version.txt bbl_install\files\version.txt -Force
Copy-Item build\bblInstallFixtures\windows\winget\manifests bbl_install\files\manifests -Recurse -Force
```

Run Kitchen:

```powershell
cd bbl_install
bundle exec kitchen list
bundle exec kitchen test winget-install-windows-windows-local
```

If you want converge/verify separately:

```powershell
bundle exec kitchen converge winget-install-windows-windows-local
bundle exec kitchen verify winget-install-windows-windows-local
bundle exec kitchen destroy winget-install-windows-windows-local
```

Useful diagnostics after converge:

```powershell
winget list --id Gnit.Bbl --exact --disable-interactivity
winget validate --manifest "$env:TEMP\bbl-winget-install\manifests" --disable-interactivity
where.exe bbl
Get-Command bbl | Format-List *
bbl --version
bbl john 3:16
bbl search God limit 1
```

---

## Step 14: CI verification

Run the CI-equivalent Gradle command locally on Windows:

```powershell
./gradlew stageBblInstallWindowsWingetFixture -Pbblpacks.embed=false
```

Validate YAML syntax at least by running:

```powershell
ruby -e "require 'yaml'; YAML.load_file('.github/workflows/ci.yml'); puts 'ci.yml OK'"
```

Then run:

```powershell
git diff --check
git status --short
```

---

## Acceptance criteria

The implementation is complete only when this passes on Windows:

```powershell
./gradlew stageBblInstallWindowsWingetFixture -Pbblpacks.embed=false
```

And this passes:

```powershell
cd bbl_install
bundle exec kitchen test winget-install-windows-windows-local
```

The Kitchen/InSpec test must confirm:

```text
bbl-winget.zip exists
multi-file manifest exists
winget validate --manifest <manifest-dir> succeeds
winget install --manifest <manifest-dir> succeeds
winget list --id Gnit.Bbl --exact succeeds
bbl --version succeeds from PATH
bbl list translations reports WEBUS
bbl john 3:16 succeeds
bbl search God limit 1 succeeds
cleanup stops the local HTTP server
cleanup uninstalls Gnit.Bbl or removes the WinGet portable install cleanly
```

The CI matrix must have:

```text
Package fixture job for installer-winget on windows-latest
E2E Kitchen job for winget_install_windows on windows-latest
bbl-install-windows-winget-files artifact uploaded/downloaded
bbl-winget.zip copied into bbl_install/files
manifest directory copied into bbl_install/files/manifests without flattening
```

---

## Non-goals

Do not do these in this task:

- Do not submit to `microsoft/winget-pkgs`.
- Do not create a pull request to the public WinGet community repository.
- Do not require a GitHub PAT.
- Do not sign anything.
- Do not publish a release asset.
- Do not use a GitHub release URL unless the fixture is explicitly changed from local test to release publishing.
- Do not skip `winget validate`.
- Do not skip real `winget install --manifest`.
- Do not replace the existing Windows manual install tests.
- Do not change existing Linux/macOS package tests.
- Do not rely on the interactive WingetCreate wizard in CI.
- Do not leave a background HTTP server running after Kitchen destroy.
- Do not assume `file:///` InstallerUrl support unless proven by local and CI tests.

---

## Final response expected from Codex

When finished, summarize:

1. Files changed.
2. New Gradle tasks.
3. New Kitchen suite and instance name.
4. Whether WingetCreate was used directly or only kept as a helper because its `new` flow is interactive.
5. Exact local manifest path and package ZIP path.
6. Exact `winget validate` and `winget install --manifest` commands run.
7. Whether `LocalManifestFiles` had to be enabled and whether elevation was required.
8. Local commands run and results.
9. Any skipped test with the exact reason.
10. Whether CI now covers:
    - WinGet package fixture generation
    - WinGet local manifest validation
    - WinGet local install/runtime E2E
