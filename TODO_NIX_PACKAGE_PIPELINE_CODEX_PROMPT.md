# TODO: Add Nix / NixOS Package Pipeline for `bbl`

Date: 2026-06-22
Target repo: `nehemiaharchives/bbl-kmp`
Target branch suggestion: `package/nix-flake`

This file is written as a Codex CLI task prompt. Open Codex from the repo root, paste this file, and let it modify the repo.

```sh
cd /path/to/bbl-kmp
git checkout -b package/nix-flake
codex
```

Paste everything from **Codex task starts here** through the end of this file.

---

## Web-search / repo facts used

Current repo facts:

- `TODO_PACKAGES.md` already lists NixOS as TODO: package `.nix [ ]`, manager `nix`, container `nixos/nix[ ]`.
- `build.gradle.kts` already has the packaging pattern we should copy: Gradle builds/stages `.deb`, `.rpm`, `.pkg.tar.zst`, and `.apk` package fixtures under `build/bblInstallFixtures/linux/<package-kind>`.
- Current Linux package tasks depend on `stageBblInstallLinuxCliCoreFixture`, `stageBblInstallLinuxCliSearchCommonFixture`, and `stageBblInstallVersionFixture`.
- Current Linux package E2E is driven from `.github/workflows/ci.yml`: package fixture jobs upload `bbl-install-linux-*-files`, then `e2e-cli` downloads those fixtures and runs package-install E2E.
- `publish.yml` already stages linked release executables, restores them into `build/bblInstallFixtures/<platform>`, runs package fixture tasks, copies Linux installer packages from build outputs, generates `SHA256SUMS.txt`, and uploads release assets.

External references checked:

- Nix flakes: https://nix.dev/concepts/flakes.html
- `nix flake` reference: https://nix.dev/manual/nix/2.24/command-ref/new-cli/nix3-flake
- `nix profile install` reference: https://nix.dev/manual/nix/2.28/command-ref/new-cli/nix3-profile-install.html
- Nixpkgs `stdenv.mkDerivation` / hooks reference: https://nixos.org/manual/nixpkgs/stable/
- Nixpkgs `autoPatchelfHook`: https://nixos.org/manual/nixpkgs/stable/#autoPatchelfHook
- Nixpkgs `makeWrapper` / `wrapProgram`: https://nixos.org/manual/nixpkgs/stable/#fun-makeWrapper
- Codex CLI README: https://github.com/openai/codex

Important packaging interpretation:

- Do **not** make this a fake `.nix` binary package like `.deb` or `.rpm`. For Nix, the distributable should be a **flake fixture tarball** containing `flake.nix`, `bbl.nix`, and prebuilt Linux `x86_64` assets.
- Do **not** install files into `/usr/bin`, `/usr/local`, or a fixed `/home/<user>` path in the Nix package. Nix package outputs live in `/nix/store/...`; user-home files must be created only by the runtime wrapper.
- The Docker image `nixos/nix:latest` tests the Nix package manager / flake install path. It is not a full NixOS VM/systemd test. Do not claim that this tests NixOS module integration.
- Because `bbl` and `bbl-search-common` are prebuilt ELF binaries produced outside Nix, the derivation should use `autoPatchelfHook`. Start with `stdenv.cc.cc.lib` and `zlib` in `buildInputs`; if Nix reports another missing shared object, inspect the exact missing `.so` and add the correct Nixpkgs library rather than hiding the error.

---

# Codex task starts here

You are working in `nehemiaharchives/bbl-kmp`. Add a Nix / NixOS-compatible package fixture pipeline for the `bbl` CLI.

## Goal

Implement this full path:

1. Gradle stages a Nix flake fixture tarball.
2. CI builds and uploads that fixture artifact.
3. E2E tests install/run the package using the Nix package manager in `nixos/nix:latest`.
4. Publish workflow includes the Nix flake fixture tarball as a GitHub Release asset and covers it in `SHA256SUMS.txt`.
5. `TODO_PACKAGES.md` reflects the new status.

Keep this consistent with the current `.deb`, `.rpm`, Arch, macOS, Homebrew, MSI, and winget fixture style.

## Non-negotiable design decisions

- Package name: `bbl`.
- Nix target system: `x86_64-linux`.
- Fixture directory: `build/bblInstallFixtures/linux/nix`.
- Generic fixture tarball name: `bbl-nix.tar.gz`.
- Release asset name: `bbl-<tag>-linux-x64-nix-flake.tar.gz`.
- Nix package output must expose `bin/bbl`.
- Nix flake must expose:
  - `packages.x86_64-linux.bbl`
  - `packages.x86_64-linux.default`
  - `apps.x86_64-linux.bbl`
  - `apps.x86_64-linux.default`
- Nix package must store immutable package-owned files under `$out/libexec/bbl` and `$out/share/bbl/packs`.
- Runtime wrapper must copy helper assets into the current user's `$HOME/.bbl` on first run, matching the current macOS/Homebrew first-run behavior:
  - `$HOME/.bbl/bin/bbl-search-common`
  - `$HOME/.bbl/packs/webus.zip`
- Do not write fixed-user home paths during package build/install.
- Do not use nFPM for Nix.
- Do not add a NixOS module in this pass. This is only package-manager integration.

## Step 1: Add Gradle task `stageBblInstallLinuxNixFixture`

Edit `build.gradle.kts`.

Add a new Linux Nix fixture task near the existing Linux package tasks.

The task should:

- be grouped under `LifecycleBasePlugin.BUILD_GROUP`;
- depend on:
  - `stageBblInstallLinuxCliCoreFixture`
  - `stageBblInstallLinuxCliSearchCommonFixture`
  - `stageBblInstallVersionFixture`
- read:
  - `build/bblInstallFixtures/linux/cli-core/bbl`
  - `build/bblInstallFixtures/linux/cli-search-common/bbl-search-common`
  - `resources/bblpacks/webus.zip`
  - the `bblVersionProvider`
- write:
  - `build/bblInstallFixtures/linux/nix/version.txt`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/flake.nix`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/bbl.nix`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/README.md`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/assets/bbl`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/assets/bbl-search-common`
  - `build/bblInstallFixtures/linux/nix/bbl-nix/assets/webus.zip`
  - `build/bblInstallFixtures/linux/nix/bbl-nix.tar.gz`

Implementation shape:

```kotlin
val linuxNixFixtureDirectory = layout.buildDirectory.dir("bblInstallFixtures/linux/nix")

val stageBblInstallLinuxNixFixture = tasks.register("stageBblInstallLinuxNixFixture") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Stage the Linux x86_64 Nix flake package fixture for Nix package-manager tests."
    notCompatibleWithConfigurationCache("Writes generated Nix flake files and a tar archive from script-scoped providers.")
    onlyIf("Linux-only package task") {
        System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
    }
    dependsOn(
        "stageBblInstallLinuxCliCoreFixture",
        "stageBblInstallLinuxCliSearchCommonFixture",
        stageBblInstallVersionFixture,
    )

    val stagedBbl = layout.buildDirectory.file("bblInstallFixtures/linux/cli-core/bbl")
    val stagedSearchCommon = layout.buildDirectory.file("bblInstallFixtures/linux/cli-search-common/bbl-search-common")
    val stagedWebusPack = layout.projectDirectory.file("resources/bblpacks/webus.zip")
    val fixtureDirectory = linuxNixFixtureDirectory

    inputs.files(stagedBbl, stagedSearchCommon, stagedWebusPack)
    inputs.property("bblVersion", bblVersionProvider)
    outputs.dir(fixtureDirectory)

    doLast {
        val version = bblVersionProvider.get()
        val bbl = stagedBbl.get().asFile
        val searchCommon = stagedSearchCommon.get().asFile
        val webusPack = stagedWebusPack.asFile
        require(bbl.isFile) { "Missing staged bbl binary: ${'$'}{bbl.absolutePath}" }
        require(searchCommon.isFile) { "Missing staged bbl-search-common binary: ${'$'}{searchCommon.absolutePath}" }
        require(webusPack.isFile) { "Missing webus pack: ${'$'}{webusPack.absolutePath}" }

        val output = fixtureDirectory.get().asFile
        val root = output.resolve("bbl-nix")
        val assets = root.resolve("assets")
        output.deleteRecursively()
        assets.mkdirs()

        bbl.copyTo(assets.resolve("bbl"), overwrite = true)
        searchCommon.copyTo(assets.resolve("bbl-search-common"), overwrite = true)
        webusPack.copyTo(assets.resolve("webus.zip"), overwrite = true)
        require(assets.resolve("bbl").setExecutable(true, false)) { "Unable to make Nix bbl executable" }
        require(assets.resolve("bbl-search-common").setExecutable(true, false)) { "Unable to make Nix bbl-search-common executable" }

        output.resolve("version.txt").writeText("${'$'}version\n")

        root.resolve("flake.nix").writeText(/* see required flake below */)
        root.resolve("bbl.nix").writeText(/* see required derivation below */)
        root.resolve("README.md").writeText(/* short install/use notes */)

        val tarExitCode = ProcessBuilder(
            "tar", "-C", output.absolutePath,
            "-czf", output.resolve("bbl-nix.tar.gz").absolutePath,
            "bbl-nix",
        ).inheritIO().start().waitFor()
        require(tarExitCode == 0) { "Failed to create ${'$'}{output.resolve("bbl-nix.tar.gz").absolutePath}" }
    }
}
```

Adjust names if necessary to match the current file style.

## Step 2: Generate `flake.nix`

The generated `flake.nix` should be explicit and minimal:

```nix
{
  description = "bbl CLI Nix package fixture";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in {
      packages.${system}.bbl = pkgs.callPackage ./bbl.nix { };
      packages.${system}.default = self.packages.${system}.bbl;

      apps.${system}.bbl = {
        type = "app";
        program = "${self.packages.${system}.bbl}/bin/bbl";
      };
      apps.${system}.default = self.apps.${system}.bbl;

      checks.${system}.bbl = self.packages.${system}.bbl;
    };
}
```

## Step 3: Generate `bbl.nix`

Generate a derivation that packages the prebuilt Linux binaries and runtime assets.

Required behavior:

- Use `stdenv.mkDerivation`.
- Use `autoPatchelfHook` because the input binaries are external ELF binaries.
- Start with `buildInputs = [ stdenv.cc.cc.lib zlib ];`.
- If the E2E Nix build fails with missing shared libraries, add exact Nixpkgs dependencies based on the missing `.so` names.
- Keep the wrapper as a shell script because it needs runtime `$HOME` copy behavior.
- Do not hide missing dependency errors with `autoPatchelfIgnoreMissingDeps = [ "*" ];`.

Template:

```nix
{ lib
, stdenv
, autoPatchelfHook
, bash
, coreutils
, zlib
}:

stdenv.mkDerivation {
  pname = "bbl";
  version = "__BBL_VERSION__";

  src = ./assets;

  nativeBuildInputs = [
    autoPatchelfHook
  ];

  buildInputs = [
    stdenv.cc.cc.lib
    zlib
  ];

  dontConfigure = true;
  dontBuild = true;

  installPhase = ''
    runHook preInstall

    install -Dm755 "$src/bbl" "$out/libexec/bbl/bbl"
    install -Dm755 "$src/bbl-search-common" "$out/libexec/bbl/bbl-search-common"
    install -Dm644 "$src/webus.zip" "$out/share/bbl/packs/webus.zip"

    mkdir -p "$out/bin"
    cat > "$out/bin/bbl" <<EOF_WRAPPER
#!${bash}/bin/bash
set -euo pipefail
assets="$out"
mkdir -p "\$HOME/.bbl/bin" "\$HOME/.bbl/packs"
if ! ${coreutils}/bin/cmp -s "\$assets/libexec/bbl/bbl-search-common" "\$HOME/.bbl/bin/bbl-search-common"; then
  ${coreutils}/bin/install -m 0755 "\$assets/libexec/bbl/bbl-search-common" "\$HOME/.bbl/bin/bbl-search-common"
fi
if ! ${coreutils}/bin/cmp -s "\$assets/share/bbl/packs/webus.zip" "\$HOME/.bbl/packs/webus.zip"; then
  ${coreutils}/bin/install -m 0644 "\$assets/share/bbl/packs/webus.zip" "\$HOME/.bbl/packs/webus.zip"
fi
exec "\$assets/libexec/bbl/bbl" "\$@"
EOF_WRAPPER
    chmod 0755 "$out/bin/bbl"

    runHook postInstall
  '';

  meta = with lib; {
    description = "Read/search Holy Bible in your terminal";
    homepage = "https://github.com/nehemiaharchives/bbl";
    license = licenses.asl20;
    mainProgram = "bbl";
    platforms = [ "x86_64-linux" ];
  };
}
```

When generating from Gradle, replace `__BBL_VERSION__` with `bblVersionProvider.get()`.

## Step 4: Add a short generated README inside the fixture

Generate `build/bblInstallFixtures/linux/nix/bbl-nix/README.md` with content like:

```md
# bbl Nix flake fixture

This is a generated Nix flake fixture for the bbl CLI.

Build:

```sh
NIX_CONFIG='experimental-features = nix-command flakes' nix build .#bbl
```

Run:

```sh
NIX_CONFIG='experimental-features = nix-command flakes' nix run .#bbl -- john 3:16
```

Install into a profile:

```sh
NIX_CONFIG='experimental-features = nix-command flakes' nix profile install .#bbl
```

The package stores immutable assets in the Nix store and copies `bbl-search-common` and `webus.zip` into `$HOME/.bbl` on first run.
```

Escape nested Markdown fences correctly inside the Kotlin string.

## Step 5: Local verification commands

After implementing the Gradle task, run these from the repo root:

```sh
./gradlew stageBblInstallLinuxNixFixture \
  -PExecutableType=release \
  -Pbblpacks.embed=false

test -f build/bblInstallFixtures/linux/nix/bbl-nix.tar.gz
tar -tzf build/bblInstallFixtures/linux/nix/bbl-nix.tar.gz | sort | tee /tmp/bbl-nix-tar-list.txt
grep -qx 'bbl-nix/flake.nix' /tmp/bbl-nix-tar-list.txt
grep -qx 'bbl-nix/bbl.nix' /tmp/bbl-nix-tar-list.txt
grep -qx 'bbl-nix/assets/bbl' /tmp/bbl-nix-tar-list.txt
grep -qx 'bbl-nix/assets/bbl-search-common' /tmp/bbl-nix-tar-list.txt
grep -qx 'bbl-nix/assets/webus.zip' /tmp/bbl-nix-tar-list.txt
```

Then test in the Nix Docker image:

```sh
docker pull nixos/nix:latest

docker run --rm \
  -v "$PWD/build/bblInstallFixtures/linux/nix:/work:ro" \
  nixos/nix:latest \
  sh -lc '
    set -eu
    rm -rf /tmp/bbl-nix /tmp/bbl-result /tmp/bbl-home /tmp/bbl-profile /tmp/bbl-profile-home
    mkdir -p /tmp/bbl-nix
    tar -xzf /work/bbl-nix.tar.gz -C /tmp/bbl-nix --strip-components=1
    cd /tmp/bbl-nix
    export NIX_CONFIG="experimental-features = nix-command flakes"
    nix flake show
    nix build .#bbl --out-link /tmp/bbl-result
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl --version
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl john 3:16
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl search God limit 1
    test -x /tmp/bbl-home/.bbl/bin/bbl-search-common
    test -f /tmp/bbl-home/.bbl/packs/webus.zip
    nix profile install .#bbl --profile /tmp/bbl-profile
    HOME=/tmp/bbl-profile-home /tmp/bbl-profile/bin/bbl john 3:16
  '
```

If the Docker Nix build fails with `auto-patchelf` missing libraries, do this:

1. Read the missing `.so` names from the error.
2. Add the correct Nixpkgs packages to `buildInputs` in generated `bbl.nix`.
3. Re-run the Docker command.
4. Do not ignore the missing dependency unless it is genuinely optional and the binary still runs all tests.

## Step 6: Update CI package fixture matrix

Edit `.github/workflows/ci.yml`.

In `jobs.tests.strategy.matrix.include`, add a package job near the other Linux package jobs:

```yaml
          - platform: nixos
            module_name: installer-nix
            os: ubuntu-latest
            timeout: 45
            task: :cli:core:linuxX64Test
            needs_konan_cache: true
            fixture_task: stageBblInstallLinuxNixFixture "-PExecutableType=release" "-Pbblpacks.embed=false"
            fixture_artifact_name: bbl-install-linux-nix-files
            fixture_path: build/bblInstallFixtures/linux/nix
            job_type: Package
```

No `needs_nfpm` is required.

## Step 7: Update CI E2E matrix

In `.github/workflows/ci.yml`, add this row under `jobs.e2e-cli.strategy.matrix.include` near the other package-install rows:

```yaml
          - suite: nix_install_nixos
            platform: nixos
            os: ubuntu-latest
            fixture_pattern: bbl-install-linux-nix-files
            kitchen_instance: nix-install-nixos-nixos-nix-latest
```

The `kitchen_instance` value is only for naming consistency if later migrated to Kitchen. In this pass, run the Nix E2E with direct Docker, not Chef/Test Kitchen, unless you quickly prove Kitchen works cleanly with `nixos/nix:latest`.

Reason: the Nix Docker image is ideal for `nix build`, `nix run`, and `nix profile install`; forcing Chef/Inspec into it is unnecessary and brittle.

## Step 8: Prepare the Nix fixture in the E2E job

Update the Linux prepare step condition to include `matrix.platform == 'nixos'`.

Add a branch before the other package branches:

```bash
          if [ "${{ matrix.suite }}" = "nix_install_nixos" ]; then
            fixture_dir=bbl_install/files/linux/nix

            if [ -f bbl_install/files/bbl-nix.tar.gz ]; then
              :
            elif [ -f "$fixture_dir/bbl-nix.tar.gz" ]; then
              cp "$fixture_dir/bbl-nix.tar.gz" bbl_install/files/bbl-nix.tar.gz
            else
              echo "Missing bbl-nix.tar.gz" >&2
              find bbl_install/files -maxdepth 5 -type f | sort
              exit 1
            fi

            if [ -f "$fixture_dir/version.txt" ]; then
              cp "$fixture_dir/version.txt" bbl_install/files/version.txt
            elif [ -f bbl_install/files/common/version.txt ]; then
              cp bbl_install/files/common/version.txt bbl_install/files/version.txt
            fi

            test -f bbl_install/files/bbl-nix.tar.gz
            test -f bbl_install/files/version.txt
            tar -tzf bbl_install/files/bbl-nix.tar.gz | tee /tmp/bbl-nix-tar-list.txt
            grep -qx 'bbl-nix/flake.nix' /tmp/bbl-nix-tar-list.txt
            grep -qx 'bbl-nix/bbl.nix' /tmp/bbl-nix-tar-list.txt
            grep -qx 'bbl-nix/assets/bbl' /tmp/bbl-nix-tar-list.txt
            grep -qx 'bbl-nix/assets/bbl-search-common' /tmp/bbl-nix-tar-list.txt
            grep -qx 'bbl-nix/assets/webus.zip' /tmp/bbl-nix-tar-list.txt
            du -sh bbl_install/files
            exit 0
          fi
```

Also add a Docker pull step:

```yaml
      - name: Pull Nix Docker image
        if: ${{ matrix.platform == 'nixos' }}
        run: docker pull nixos/nix:latest
```

## Step 9: Add direct Docker Nix E2E step

Add this step in `jobs.e2e-cli.steps` after the fixture-prepare step and Docker pull step, before Ruby setup:

```yaml
      - name: Nix package E2E
        if: ${{ matrix.platform == 'nixos' }}
        shell: bash
        run: |
          set -euo pipefail
          docker run --rm \
            -v "$PWD/bbl_install/files:/work:ro" \
            nixos/nix:latest \
            sh -lc '
              set -eu
              rm -rf /tmp/bbl-nix /tmp/bbl-result /tmp/bbl-home /tmp/bbl-profile /tmp/bbl-profile-home
              mkdir -p /tmp/bbl-nix
              tar -xzf /work/bbl-nix.tar.gz -C /tmp/bbl-nix --strip-components=1
              cd /tmp/bbl-nix
              export NIX_CONFIG="experimental-features = nix-command flakes"
              nix flake show
              nix build .#bbl --out-link /tmp/bbl-result
              HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl --version
              HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl john 3:16
              HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl search God limit 1
              test -x /tmp/bbl-home/.bbl/bin/bbl-search-common
              test -f /tmp/bbl-home/.bbl/packs/webus.zip
              nix profile install .#bbl --profile /tmp/bbl-profile
              HOME=/tmp/bbl-profile-home /tmp/bbl-profile/bin/bbl john 3:16
            '
```

Then prevent unnecessary Ruby/Kitchen setup for the Nix-only row:

- Add `if: ${{ matrix.platform != 'nixos' }}` to the `Set up Ruby` step.
- Existing Kitchen steps already only run for Ubuntu/Fedora/Arch, macOS, or Windows; confirm `nixos` does not accidentally enter them.

## Step 10: Optional Kitchen files only if you decide to keep a Kitchen suite

Prefer the direct Docker E2E above. But if you decide to also add Kitchen metadata for future consistency, make it non-blocking and do not use it in CI until verified.

Possible future `bbl_install/kitchen.yml` additions:

```yaml
  - name: nixos-nix-latest
    driver:
      image: nixos/nix:latest
      privileged: true

  - name: nix_install_nixos
    run_list:
      - recipe[bbl_install::nix_install_nixos]
    verifier:
      inspec_tests:
        - test/integration/nix_install_nixos
    includes:
      - nixos-nix-latest
```

Do not block the main PR on this if Chef/Dokken does not cooperate with `nixos/nix:latest`.

## Step 11: Update publish workflow

Edit `.github/workflows/publish.yml`.

In `package-cli-assets.matrix.include` for `asset: linux-x64`, add the Nix fixture task to `package_tasks`:

```yaml
              stageBblInstallLinuxNixFixture
```

The Linux job already restores staged executables before running package tasks. Confirm that `stageBblInstallLinuxNixFixture` works with the existing excluded fixture tasks:

```yaml
              -x stageBblInstallLinuxCliCoreFixture
              -x stageBblInstallLinuxCliSearchCommonFixture
```

Update `Assemble Unix release assets` Linux branch.

Existing branch copies `.deb`, `.rpm`, and `.pkg.tar.zst` from `build/distributions`. Add a copy from the staged Nix fixture:

```bash
            if [ -f "$staged/nix/bbl-nix.tar.gz" ]; then
              cp "$staged/nix/bbl-nix.tar.gz" "dist/bbl-$tag-linux-x64-nix-flake.tar.gz"
            fi
```

Keep `SHA256SUMS.txt` generation unchanged; it already hashes every file in `dist`.

## Step 12: Update `TODO_PACKAGES.md`

Update the matrix row from TODO to WIP or Supported after CI/E2E is wired.

Recommended row after implementation:

```md
| Supported | Nix / NixOS-compatible | flake tarball [x] | nix | nixos/nix[x] |
```

Add notes:

- Nix packaging is a generated flake fixture, not an nFPM package.
- It targets `x86_64-linux` initially.
- E2E uses `nixos/nix:latest` and validates `nix build`, runtime execution, first-run `$HOME/.bbl` asset copy, and `nix profile install`.
- True NixOS module/system integration is intentionally not included yet.

If the existing table expects `Distro` rather than `Package Type`, use:

```md
| Supported | NixOS / Nix package manager | flake tarball [x] | nix | nixos/nix[x] |
```

## Step 13: Run final verification

Run these locally before committing:

```sh
./gradlew stageBblInstallLinuxNixFixture \
  -PExecutableType=release \
  -Pbblpacks.embed=false

docker pull nixos/nix:latest

docker run --rm \
  -v "$PWD/build/bblInstallFixtures/linux/nix:/work:ro" \
  nixos/nix:latest \
  sh -lc '
    set -eu
    rm -rf /tmp/bbl-nix /tmp/bbl-result /tmp/bbl-home /tmp/bbl-profile /tmp/bbl-profile-home
    mkdir -p /tmp/bbl-nix
    tar -xzf /work/bbl-nix.tar.gz -C /tmp/bbl-nix --strip-components=1
    cd /tmp/bbl-nix
    export NIX_CONFIG="experimental-features = nix-command flakes"
    nix flake show
    nix build .#bbl --out-link /tmp/bbl-result
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl --version
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl john 3:16
    HOME=/tmp/bbl-home /tmp/bbl-result/bin/bbl search God limit 1
    test -x /tmp/bbl-home/.bbl/bin/bbl-search-common
    test -f /tmp/bbl-home/.bbl/packs/webus.zip
    nix profile install .#bbl --profile /tmp/bbl-profile
    HOME=/tmp/bbl-profile-home /tmp/bbl-profile/bin/bbl john 3:16
  '
```

Then run the fastest relevant Gradle/Kitchen checks that still make sense:

```sh
./gradlew :cli:core:linuxX64Test -Pbblpacks.embed=false
```

If you changed YAML heavily, validate by reading the matrix carefully. Do not invent unavailable GitHub Actions.

## Step 14: Commit message

Use this commit message:

```text
Add Nix flake package fixture pipeline
```

## Acceptance criteria

- `./gradlew stageBblInstallLinuxNixFixture -PExecutableType=release -Pbblpacks.embed=false` creates `build/bblInstallFixtures/linux/nix/bbl-nix.tar.gz`.
- The tarball contains `flake.nix`, `bbl.nix`, `README.md`, `assets/bbl`, `assets/bbl-search-common`, and `assets/webus.zip`.
- `docker run nixos/nix:latest ... nix build .#bbl` succeeds.
- `/tmp/bbl-result/bin/bbl --version` succeeds inside the Nix container.
- `/tmp/bbl-result/bin/bbl john 3:16` succeeds inside the Nix container.
- `/tmp/bbl-result/bin/bbl search God limit 1` succeeds inside the Nix container.
- Runtime wrapper creates `$HOME/.bbl/bin/bbl-search-common` and `$HOME/.bbl/packs/webus.zip`.
- `nix profile install .#bbl --profile /tmp/bbl-profile` succeeds.
- CI has a package fixture job for `installer-nix`.
- CI has a Nix E2E row and direct Docker step.
- Publish workflow uploads `bbl-<tag>-linux-x64-nix-flake.tar.gz`.
- Release checksums include the Nix flake tarball automatically.
- `TODO_PACKAGES.md` reflects Nix package-manager support accurately.
