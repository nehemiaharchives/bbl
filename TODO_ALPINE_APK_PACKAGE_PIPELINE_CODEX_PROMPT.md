# Codex CLI Implementation Prompt: Add Alpine Linux APK Package Pipeline for `bbl-kmp`

Use this prompt from the root of the `bbl-kmp` repository.

---

## Goal

Implement a first-class Alpine Linux package pipeline for `bbl`, parallel to the existing `.deb`, `.rpm`, and Arch Linux pacman package pipelines.

The required flow is:

```text
Ubuntu build machine / GitHub Actions ubuntu-latest
  -> build Linux x64 native binaries
  -> generate Alpine Linux APK package using nFPM
  -> stage package into Gradle fixture directory
  -> upload/download fixture in CI
  -> copy bbl.apk into bbl_install/files
  -> run Test Kitchen against alpine:latest
  -> install with apk add --allow-untrusted
  -> run E2E verification with InSpec
```

Do **not** require Alpine Linux to build the package. Build the `.apk` on Ubuntu with nFPM. The Alpine container is only for install/runtime verification.

Important: this task means **Alpine APK packaging and runtime verification**. It does **not** automatically mean the Kotlin/Native binary is musl-native. The current Linux x64 Kotlin/Native binary may be glibc-linked. The Alpine E2E test must reveal that honestly. If the binary requires glibc compatibility, declare/install the required Alpine compatibility packages such as `gcompat`, `libgcc`, and `libstdc++`.

This task is **not** an Alpine repository publishing task. No repository index, no signing, no `apk add bbl` from a remote repo. Use local package installation:

```bash
apk add --allow-untrusted /tmp/bbl.apk
```

---

## Current repo facts to preserve

Before editing, inspect the current files:

```bash
git status --short
sed -n '1,1160p' build.gradle.kts
sed -n '1,320p' bbl_install/kitchen.yml
sed -n '1,140p' bbl_install/attributes/default.rb
find bbl_install/attributes -maxdepth 1 -type f -print | sort
find bbl_install/recipes -maxdepth 1 -type f -print | sort
find bbl_install/test/integration -maxdepth 2 -type f -print | sort
sed -n '1,860p' .github/workflows/ci.yml
```

The repo already has these relevant pieces:

1. `build.gradle.kts` builds `.deb` with nFPM in `buildLinuxDeb`.
2. `build.gradle.kts` builds `.rpm` with nFPM in `buildLinuxRpm`.
3. `build.gradle.kts` builds Arch Linux pacman package with nFPM in `buildLinuxArchlinux`.
4. `stageBblInstallLinuxDebFixture` stages `build/bblInstallFixtures/linux/deb/bbl.deb`.
5. `stageBblInstallLinuxRpmFixture` stages `build/bblInstallFixtures/linux/rpm/bbl.rpm`.
6. `stageBblInstallLinuxArchlinuxFixture` stages `build/bblInstallFixtures/linux/archlinux/bbl.pkg.tar.zst`.
7. `stageBblInstallLinuxFixtures` and `stageBblInstallLinuxCliAllFixture` already depend on the Linux package fixture tasks. Add Alpine there too.
8. `bbl_install/kitchen.yml` already has Ubuntu, Fedora, and Arch package suites.
9. `bbl_install/attributes/fedora.rb` and `bbl_install/attributes/archlinux.rb` override the default Ubuntu-ish Linux paths for non-Ubuntu package tests.
10. `.github/workflows/ci.yml` already has package matrix rows for Deb, RPM, and Arch.
11. `.github/workflows/ci.yml` already has E2E matrix rows for Deb, RPM, and Arch.
12. `.github/workflows/ci.yml` already has Linux package fixture preparation branches for Deb, RPM, and Arch.

Preserve the existing `.deb`, `.rpm`, and Arch Linux behavior. Add Alpine as a sibling.

---

## Design decisions

Use the Docker Official Alpine image requested by the maintainer:

```yaml
image: alpine:latest
```

Use this Kitchen platform name:

```text
alpine-latest
```

Use this Kitchen suite name:

```text
apk_install_alpine
```

Expected Kitchen instance name:

```text
apk-install-alpine-alpine-latest
```

Confirm the final name with:

```bash
cd bbl_install
bundle exec kitchen list
```

Use an Alpine-specific E2E test user:

```text
user:  alpine
group: alpine
home:  /home/alpine
```

Do not silently reuse `/home/ubuntu`, `/home/fedora`, or `/home/arch`.

For this first implementation, mirror the existing Linux package fixture layout:

```text
/usr/bin/bbl
/home/alpine/.bbl/bin/bbl-search-common
/home/alpine/.bbl/packs/webus.zip
```

Do not include all search helper binaries unless the `.deb`, `.rpm`, and Arch package pipelines are intentionally updated to match, with tests.

---

## Critical Alpine runtime warning

Alpine uses musl libc. The current `linuxX64` Kotlin/Native binary may expect the glibc dynamic loader.

Do not fake this test by only checking file existence.

Inside the Alpine Kitchen container, explicitly diagnose failures with:

```bash
file /usr/bin/bbl || true
ldd /usr/bin/bbl || true
readelf -l /usr/bin/bbl | grep -i interpreter || true
/usr/bin/bbl --version || true
```

If the binary fails with a misleading error such as:

```text
/usr/bin/bbl: not found
```

even though the file exists, suspect a missing ELF interpreter such as:

```text
/lib64/ld-linux-x86-64.so.2
```

For this task, the acceptable solution is to make the APK depend on Alpine compatibility/runtime packages and prove the installed package works in Alpine:

```yaml
depends:
  - gcompat
  - libgcc
  - libstdc++
```

Do not claim the package is musl-native unless the binary is actually rebuilt for a musl target and verified with `ldd`/`readelf`.

### Current blocker: lucene-kmp native caller validation on Alpine

This pipeline is currently blocked by [lucene-kmp issue #262](https://github.com/nehemiaharchives/lucene-kmp/issues/262).

The APK can be generated with nFPM, installed by Chef on `alpine:latest`, and run with `gcompat`, `libgcc`, and `libstdc++`. The Alpine InSpec run reached 41 successful assertions out of 42. The remaining required search assertion fails because lucene-kmp 10.2.0-alpha13 validates native `VectorizationProvider` callers by matching symbolic Kotlin stack frames. Under Alpine `gcompat`, those internal frames are reported as addresses rather than fully qualified caller names, so a legitimate `Lucene101PostingsReader` call is rejected:

```text
kotlin.UnsupportedOperationException: VectorizationProvider is internal and can only be used by known Lucene classes.
```

Do not weaken or skip `bbl search God limit 1` to work around this failure. Resume the Alpine pipeline only after issue #262 is fixed in lucene-kmp, released, and the `bbl` lucene-kmp dependency is updated to that fixed release. Then rerun the complete Gradle fixture and Kitchen acceptance commands in this prompt.

---

## Step 1: Add Alpine-specific Gradle package properties

Edit `build.gradle.kts`.

Near the existing Deb/RPM/Arch properties, add:

```kotlin
val bblAlpineInstallUser = providers.gradleProperty("bblAlpineInstallUser")
    .orElse("alpine")
val bblAlpineInstallGroup = providers.gradleProperty("bblAlpineInstallGroup")
    .orElse(bblAlpineInstallUser)
val bblAlpineInstallHome = providers.gradleProperty("bblAlpineInstallHome")
    .orElse("/home/alpine")
val linuxAlpineOutputFile = bblVersionProvider.flatMap { version ->
    linuxDebOutputDirectory.map { it.file("bbl-$version-linux-x86_64.apk") }
}
```

Keep using the existing `linuxDebOutputDirectory` if that is already the shared `build/distributions` output directory for Deb/RPM/Arch. Do not rename it in this task.

---

## Step 2: Add `buildLinuxAlpine` Gradle task

Add this task near `buildLinuxDeb`, `buildLinuxRpm`, and `buildLinuxArchlinux`.

```kotlin
val buildLinuxAlpine = tasks.register<Exec>("buildLinuxAlpine") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Build the Linux x86_64 Alpine Linux APK package for bbl using nFPM."
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
    val nfpmConfig = layout.buildDirectory.file("nfpm/apk-x86_64/nfpm.yaml")
    val postInstallScript = layout.buildDirectory.file("nfpm/apk-x86_64/postinstall.sh")

    inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
    inputs.property("bblVersion", bblVersionProvider)
    inputs.property("bblAlpineInstallUser", bblAlpineInstallUser)
    inputs.property("bblAlpineInstallGroup", bblAlpineInstallGroup)
    inputs.property("bblAlpineInstallHome", bblAlpineInstallHome)
    outputs.file(linuxAlpineOutputFile)

    doFirst {
        val installUser = bblAlpineInstallUser.get()
        val installGroup = bblAlpineInstallGroup.get()
        val installHome = bblAlpineInstallHome.get()

        require(installHome.startsWith("/")) {
            "bblAlpineInstallHome must be an absolute path: $installHome"
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

        require(bbl.setExecutable(true, false)) {
            "Unable to make ${bbl.absolutePath} executable"
        }
        require(searchCommon.setExecutable(true, false)) {
            "Unable to make ${searchCommon.absolutePath} executable"
        }

        val configFile = nfpmConfig.get().asFile
        configFile.parentFile.mkdirs()

        val postInstallFile = postInstallScript.get().asFile
        postInstallFile.writeText(
            """
            #!/bin/sh
            set -e
            if [ -d '${installHome}/.bbl' ]; then
              chown -R '${installUser}:${installGroup}' '${installHome}/.bbl' || true
            fi
            """.trimIndent() + "\n"
        )
        postInstallFile.setExecutable(true, false)

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
            depends:
              - gcompat
              - libgcc
              - libstdc++
            apk:
              arch: x86_64
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
            "--packager", "apk",
            "--target", linuxAlpineOutputFile.get().asFile.absolutePath,
        )
    }
}
```

If nFPM rejects `apk.arch: x86_64`, inspect the generated error. The nFPM config reference says packager-specific `{format}.arch` can override the top-level architecture. Prefer keeping `apk.arch: x86_64`; remove it only if the currently installed nFPM version proves it is invalid.

If runtime proves that fewer dependencies are needed, do not remove them until an Alpine E2E run still passes. For now, `gcompat`, `libgcc`, and `libstdc++` are pragmatic because the Kotlin/Native Linux binary may not be musl-native.

---

## Step 3: Add Alpine fixture staging task

Add this near the other Linux package stage tasks:

```kotlin
tasks.register<Sync>("stageBblInstallLinuxAlpineFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Linux x86_64 Alpine Linux APK package fixture for Kitchen tests."
    dependsOn(buildLinuxAlpine, stageBblInstallVersionFixture)
    into(layout.buildDirectory.dir("bblInstallFixtures/linux/alpine"))
    from(linuxAlpineOutputFile) { rename { "bbl.apk" } }
    from(bblInstallCommonFixtureDirectory) { include("version.txt") }
}
```

Then update aggregate Linux fixture tasks:

```kotlin
tasks.register<Sync>("stageBblInstallLinuxFixtures") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
    dependsOn("stageBblInstallLinuxDebFixture")
    dependsOn("stageBblInstallLinuxRpmFixture")
    dependsOn("stageBblInstallLinuxArchlinuxFixture")
    dependsOn("stageBblInstallLinuxAlpineFixture")
}

tasks.register<Sync>("stageBblInstallLinuxCliAllFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage all Linux CLI fixture files for bbl_install Kitchen tests."
    prepareBblInstallCookbookFiles(bblInstallPlatforms.single { it.id == "linux" })
    dependsOn("stageBblInstallLinuxDebFixture")
    dependsOn("stageBblInstallLinuxRpmFixture")
    dependsOn("stageBblInstallLinuxArchlinuxFixture")
    dependsOn("stageBblInstallLinuxAlpineFixture")
}
```

If the existing task bodies already differ, edit minimally and just add the Alpine dependency.

---

## Step 4: Create `bbl_install/attributes/alpine.rb`

Create:

```text
bbl_install/attributes/alpine.rb
```

Content:

```ruby
if platform_family?('alpine')
  default['bbl_install']['install_user'] = 'alpine'
  default['bbl_install']['install_group'] = 'alpine'
  default['bbl_install']['system_user'] = 'root'
  default['bbl_install']['system_group'] = 'root'
  default['bbl_install']['manage_install_user'] = true
  default['bbl_install']['test_command_user'] = 'alpine'
  default['bbl_install']['test_command_home'] = '/home/alpine'
  default['bbl_install']['home_dir'] = '/home/alpine'
  default['bbl_install']['install_root'] = '/home/alpine/.bbl'
  default['bbl_install']['bin_dir'] = '/home/alpine/.bbl/bin'
  default['bbl_install']['helper_bin_dir'] = '/home/alpine/.bbl/bin'
  default['bbl_install']['pack_dir'] = '/home/alpine/.bbl/packs'
  default['bbl_install']['completion_dir'] = '/home/alpine/.bbl/completions'
  default['bbl_install']['version_file_path'] = '/home/alpine/.bbl/version.txt'
  default['bbl_install']['bbl_binary_path'] = '/usr/bin/bbl'
end
```

This file must mirror `fedora.rb` and `archlinux.rb`, but with Alpine-specific paths.

---

## Step 5: Add Alpine platform and APK suite to Kitchen

Edit:

```text
bbl_install/kitchen.yml
```

Add this platform under `platforms:`:

```yaml
  - name: alpine-latest
    driver:
      image: alpine:latest
      privileged: true
      intermediate_instructions:
        - RUN apk add --no-cache bash ca-certificates curl gcompat libgcc libstdc++ shadow
```

Do not add `pid_one_command`; Alpine does not use systemd in this container test.

Why these intermediate packages:

```text
bash/curl/ca-certificates: useful for Chef/Dokken bootstrap and diagnostics
gcompat/libgcc/libstdc++: likely runtime compatibility for glibc-linked Kotlin/Native Linux binary
shadow: gives standard user/group/su tooling, avoiding BusyBox edge cases in Kitchen tests
```

Add the suite:

```yaml
  - name: apk_install_alpine
    run_list:
      - recipe[bbl_install::apk_install_alpine]
    verifier:
      inspec_tests:
        - test/integration/apk_install_alpine
    includes:
      - alpine-latest
```

Expected Kitchen instance name:

```text
apk-install-alpine-alpine-latest
```

Confirm with:

```bash
cd bbl_install
bundle exec kitchen list
```

---

## Step 6: Add Alpine APK install recipe

Create:

```text
bbl_install/recipes/apk_install_alpine.rb
```

Suggested implementation:

```ruby
apk_path = '/tmp/bbl.apk'
install_user = node['bbl_install']['install_user'] || 'alpine'
install_group = node['bbl_install']['install_group'] || install_user
install_home = node['bbl_install']['home_dir'] || "/home/#{install_user}"

group install_group do
  only_if { install_group && !install_group.empty? }
end

user install_user do
  gid install_group
  home install_home
  shell '/bin/sh'
  manage_home true
  only_if { install_user && !install_user.empty? }
end

cookbook_file apk_path do
  source 'bbl.apk'
  mode '0644'
end

execute 'remove existing bbl apk package and stale files' do
  command <<~SH
    set -e
    apk del bbl >/dev/null 2>&1 || true
    rm -f /usr/bin/bbl
    rm -f #{install_home}/.bbl/bin/bbl-search-common
    rm -f #{install_home}/.bbl/webus.zip
    rm -f #{install_home}/.bbl/packs/webus.zip
  SH
end

execute 'install bbl Alpine APK package' do
  command <<~SH
    set -e
    apk update
    apk add --no-cache --allow-untrusted #{apk_path}
  SH
end

execute 'diagnose installed bbl binary on Alpine' do
  command <<~SH
    set -e
    file /usr/bin/bbl || true
    ldd /usr/bin/bbl || true
    readelf -l /usr/bin/bbl | grep -i interpreter || true
    /usr/bin/bbl --version
  SH
end
```

Keep this recipe narrow. It should test the local APK package, not perform a manual install.

Do not install `/usr/bin/bbl` manually after `apk add`. If `apk add` fails, fix the APK metadata, the dependency list, or the package contents.

### If Chef provisioning fails on Alpine

Official Alpine is musl-based, so Chef/Dokken bootstrap may be the hardest part of this task.

First try the Chef recipe approach above with the `intermediate_instructions` packages.

If Kitchen fails before the recipe runs because Chef Infra cannot bootstrap on `alpine:latest`, do not waste time forcing Chef. Use a suite-local shell provisioner only for `apk_install_alpine`.

If switching to a shell provisioner, still keep `bbl_install/attributes/alpine.rb` for consistency with the package platform model.

A shell fallback is acceptable only if:

1. It still installs `bbl.apk` using `apk add --allow-untrusted`.
2. It still runs the same InSpec profile.
3. It does not change existing Ubuntu/Fedora/Arch suites.
4. It documents clearly in the final Codex summary that Chef was not usable on Alpine.

---

## Step 7: Add InSpec tests for Alpine APK install

Create:

```text
bbl_install/test/integration/apk_install_alpine/default_test.rb
```

The test should verify at least:

1. The `bbl` APK package is installed.
2. `/usr/bin/bbl` exists, is executable, and is owned by `root`.
3. `/home/alpine/.bbl/bin/bbl-search-common` exists and is executable.
4. `/home/alpine/.bbl/packs/webus.zip` exists.
5. Running `bbl --version` as `alpine` succeeds.
6. Running `bbl list translations` as `alpine` succeeds and reports WEBUS installed.
7. Running `bbl john 3:16` as `alpine` succeeds and prints expected Bible text.
8. Running `bbl search God limit 1` as `alpine` succeeds.
9. APK metadata includes sane package information.
10. APK declared dependencies include the Alpine runtime compatibility packages required for this binary.
11. Package file list does **not** accidentally contain `/home/ubuntu`, `/home/fedora`, `/home/arch`, `/usr/local`, or the wrong old path `/home/alpine/.bbl/webus.zip`.

Suggested test:

```ruby
describe command('apk info -e bbl') do
  its('exit_status') { should eq 0 }
end

describe command('apk info -a bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl/) }
  its('stdout') { should match(/Apache-2\.0/) }
end

describe command('apk info -R bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/gcompat/) }
  its('stdout') { should match(/libgcc/) }
  its('stdout') { should match(/libstdc\+\+/) }
end

describe file('/usr/bin/bbl') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'root' }
  its('group') { should eq 'root' }
end

describe file('/home/alpine/.bbl/bin/bbl-search-common') do
  it { should exist }
  it { should be_file }
  it { should be_executable }
  its('owner') { should eq 'alpine' }
  its('group') { should eq 'alpine' }
end

describe file('/home/alpine/.bbl/packs/webus.zip') do
  it { should exist }
  it { should be_file }
  its('owner') { should eq 'alpine' }
  its('group') { should eq 'alpine' }
end

describe command('apk info -L bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(%r{(^|\n)usr/bin/bbl(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)home/alpine/.bbl/bin/bbl-search-common(\n|$)}) }
  its('stdout') { should match(%r{(^|\n)home/alpine/.bbl/packs/webus.zip(\n|$)}) }
  its('stdout') { should_not include '/home/ubuntu' }
  its('stdout') { should_not include '/home/fedora' }
  its('stdout') { should_not include '/home/arch' }
  its('stdout') { should_not include '/usr/local' }
  its('stdout') { should_not include "home/alpine/.bbl/webus.zip\n" }
end

describe command('file /usr/bin/bbl') do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/ELF/) }
end

describe command('ldd /usr/bin/bbl || true') do
  its('stdout') { should_not match(/not found/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl --version'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/bbl version v?\d+\.\d+(?:\.\d+)?/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl list translations'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/WEBUS.*Installed/i) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl john 3:16'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end

describe command(%q{su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl search God limit 1'}) do
  its('exit_status') { should eq 0 }
  its('stdout') { should match(/God|god/) }
end
```

If Alpine `apk info -L bbl` output format differs, adjust the regex after checking actual output. Do not remove the file-list assertions.

If `su -s /bin/sh alpine -c ...` behaves differently in the container, install/use `shadow` in the Kitchen image as shown above, or use InSpec command resource user support if already working in this repo.

---

## Step 8: Update GitHub Actions package build matrix

Edit `.github/workflows/ci.yml`.

In the package/test matrix, add a package row parallel to `installer-deb`, `installer-rpm`, and `installer-archlinux`:

```yaml
          - platform: alpine
            module_name: installer-apk
            os: ubuntu-latest
            timeout: 45
            task: :cli:core:linuxX64Test
            needs_konan_cache: true
            needs_nfpm: true
            fixture_task: stageBblInstallLinuxAlpineFixture "-Pbblpacks.embed=false" "-PbblAlpineInstallUser=alpine" "-PbblAlpineInstallGroup=alpine" "-PbblAlpineInstallHome=/home/alpine"
            fixture_artifact_name: bbl-install-linux-alpine-files
            fixture_path: build/bblInstallFixtures/linux/alpine
            job_type: Package
```

This still builds the Alpine APK package on Ubuntu. That is intentional.

If the matrix schema has changed, adapt to the current schema but preserve these values:

```text
platform: alpine
module_name: installer-apk
needs_nfpm: true
fixture_task: stageBblInstallLinuxAlpineFixture
fixture_artifact_name: bbl-install-linux-alpine-files
fixture_path: build/bblInstallFixtures/linux/alpine
```

---

## Step 9: Update GitHub Actions E2E matrix

In the `e2e-cli` job matrix, add:

```yaml
          - suite: apk_install_alpine
            platform: alpine
            os: ubuntu-latest
            fixture_pattern: bbl-install-linux-alpine-files
            kitchen_instance: apk-install-alpine-alpine-latest
```

If `bundle exec kitchen list` produces a different instance name, use the actual name.

---

## Step 10: Update CI fixture preparation for Alpine

In `.github/workflows/ci.yml`, find the Linux cookbook preparation step. It probably currently has a guard similar to:

```yaml
if: ${{ matrix.platform == 'ubuntu' || matrix.platform == 'fedora' || matrix.platform == 'archlinux' }}
```

Add Alpine:

```yaml
if: ${{ matrix.platform == 'ubuntu' || matrix.platform == 'fedora' || matrix.platform == 'archlinux' || matrix.platform == 'alpine' }}
```

Inside the bash script, add an Alpine branch before the Arch/RPM/Deb branches:

```bash
if [ "${{ matrix.suite }}" = "apk_install_alpine" ]; then
  fixture_dir=bbl_install/files/linux/alpine

  if [ -f bbl_install/files/bbl.apk ]; then
    :
  elif [ -f "$fixture_dir/bbl.apk" ]; then
    cp "$fixture_dir/bbl.apk" bbl_install/files/bbl.apk
  else
    echo "Missing bbl.apk" >&2
    find bbl_install/files -maxdepth 5 -type f | sort
    exit 1
  fi

  if [ -f "$fixture_dir/version.txt" ]; then
    cp "$fixture_dir/version.txt" bbl_install/files/version.txt
  elif [ -f bbl_install/files/common/version.txt ]; then
    cp bbl_install/files/common/version.txt bbl_install/files/version.txt
  fi

  test -f bbl_install/files/bbl.apk
  test -f bbl_install/files/version.txt

  # Do not require apk tooling on the Ubuntu runner here.
  # Deeper package metadata/content checks happen inside Alpine Kitchen using apk.
  du -sh bbl_install/files
  exit 0
fi
```

Keep the existing Deb/RPM/Arch branches intact.

The Chef recipe expects:

```text
bbl_install/files/bbl.apk
```

---

## Step 11: Update Docker image pull logic for Alpine

Add a CI step near the existing Arch image pull step:

```yaml
      - name: Pull Alpine Docker image
        if: ${{ matrix.platform == 'alpine' }}
        run: docker pull alpine:latest
```

Leave the existing Ubuntu/Fedora/Arch logic alone unless the current workflow has a central image-cache function that is obviously intended to be extended.

---

## Step 12: Optional local package inspection

On Ubuntu, do not require Alpine `apk` tooling.

You may inspect the APK as an archive if the local tools can read it:

```bash
tar -tf build/bblInstallFixtures/linux/alpine/bbl.apk | sort | sed -n '1,160p'
```

Expected package payload should include metadata plus:

```text
usr/bin/bbl
home/alpine/.bbl/bin/bbl-search-common
home/alpine/.bbl/packs/webus.zip
```

Do not make this local archive inspection a required CI gate unless the required tools are already present on the Ubuntu runner.

---

## Step 13: Local verification commands

After implementation, run these locally on Ubuntu.

Verify nFPM exists:

```bash
nfpm --version
```

Build and stage the Alpine package:

```bash
./gradlew :cli:core:linuxX64Test
./gradlew stageBblInstallLinuxAlpineFixture \
  -Pbblpacks.embed=false \
  -PbblAlpineInstallUser=alpine \
  -PbblAlpineInstallGroup=alpine \
  -PbblAlpineInstallHome=/home/alpine
```

Prepare Kitchen files manually:

```bash
mkdir -p bbl_install/files
cp build/bblInstallFixtures/linux/alpine/bbl.apk bbl_install/files/bbl.apk
cp build/bblInstallFixtures/linux/alpine/version.txt bbl_install/files/version.txt
```

Run Kitchen:

```bash
cd bbl_install
bundle exec kitchen list
bundle exec kitchen test apk-install-alpine-alpine-latest
```

If you want converge/verify separately:

```bash
bundle exec kitchen converge apk-install-alpine-alpine-latest
bundle exec kitchen verify apk-install-alpine-alpine-latest
bundle exec kitchen destroy apk-install-alpine-alpine-latest
```

Useful manual diagnostics inside Kitchen:

```bash
bundle exec kitchen login apk-install-alpine-alpine-latest
apk info -e bbl
apk info -a bbl
apk info -R bbl
apk info -L bbl
file /usr/bin/bbl
ldd /usr/bin/bbl || true
readelf -l /usr/bin/bbl | grep -i interpreter || true
su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl john 3:16'
su -s /bin/sh alpine -c 'HOME=/home/alpine /usr/bin/bbl search God limit 1'
exit
```

---

## Step 14: CI verification

Run the relevant CI-equivalent Gradle command locally:

```bash
./gradlew stageBblInstallLinuxAlpineFixture \
  -Pbblpacks.embed=false \
  -PbblAlpineInstallUser=alpine \
  -PbblAlpineInstallGroup=alpine \
  -PbblAlpineInstallHome=/home/alpine
```

Then verify the GitHub Actions YAML is syntactically valid. If Ruby is available:

```bash
ruby -e "require 'yaml'; YAML.load_file('.github/workflows/ci.yml'); puts 'ci.yml OK'"
```

Then run:

```bash
git diff --check
git status --short
```

---

## Acceptance criteria

The implementation is complete only when this passes:

```bash
./gradlew stageBblInstallLinuxAlpineFixture \
  -Pbblpacks.embed=false \
  -PbblAlpineInstallUser=alpine \
  -PbblAlpineInstallGroup=alpine \
  -PbblAlpineInstallHome=/home/alpine
```

And this passes:

```bash
cd bbl_install
bundle exec kitchen test apk-install-alpine-alpine-latest
```

The Alpine Kitchen test must confirm:

```text
apk info -e bbl succeeds
apk info -a bbl shows sane package metadata
apk info -R bbl includes required runtime compatibility dependencies
/usr/bin/bbl exists and is executable
/home/alpine/.bbl/bin/bbl-search-common exists and is executable
/home/alpine/.bbl/packs/webus.zip exists
bbl --version succeeds as alpine
bbl list translations shows WEBUS Installed as alpine
bbl john 3:16 succeeds as alpine
bbl search God limit 1 succeeds as alpine
ldd /usr/bin/bbl does not report missing libraries
apk info -L bbl does not contain /home/ubuntu
apk info -L bbl does not contain /home/fedora
apk info -L bbl does not contain /home/arch
apk info -L bbl does not contain /usr/local
apk info -L bbl does not contain /home/alpine/.bbl/webus.zip
```

And CI must have:

```text
Package fixture job for installer-apk
E2E Kitchen job for apk_install_alpine
Alpine Docker image pulled before Kitchen runs
bbl-install-linux-alpine-files artifact uploaded/downloaded
bbl.apk copied into bbl_install/files before Kitchen converge
```

---

## Non-goals

Do not do these in this task:

- Do not publish to an Alpine package repository.
- Do not create `APKINDEX.tar.gz`.
- Do not sign the APK package.
- Do not add private APK keys.
- Do not add Wolfi, Chainguard, postmarketOS, or edge-specific Alpine variants.
- Do not replace the existing `.deb`, `.rpm`, or Arch Linux package pipelines.
- Do not change the existing `.deb`/`.rpm`/Arch package layout unless required to keep tests passing.
- Do not move package assets to `/usr/lib`, `/usr/share`, or `/usr/local` in this task.
- Do not add all search helper binaries to the APK unless you also intentionally update Deb/RPM/Arch to match, with tests.
- Do not bypass Kitchen by only testing nFPM output.
- Do not claim musl-native support unless the binary is actually built for musl and verified as such.
- Do not skip the runtime command tests on Alpine.

---

## Final response expected from Codex

When finished, summarize:

1. Files changed.
2. New Gradle tasks.
3. New Kitchen platform/suite/instance name.
4. Whether Chef recipe worked on Alpine or whether a shell provisioner fallback was required.
5. Whether the installed binary required Alpine compatibility packages.
6. Local commands run and their results.
7. Any skipped test with the exact reason.
8. Whether the CI matrix now covers:
   - Alpine APK package fixture generation
   - Alpine Kitchen install/runtime E2E
