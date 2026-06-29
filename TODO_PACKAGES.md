# Package Distribution TODO

Last updated: 2026-06-29

This document tracks package distribution status for the `bbl` CLI.

## Current Status Snapshot

Legend:

- `[x]` Done in the current repo.
- `[~]` Partly done or implemented only as a CI/local fixture.
- `[ ]` Not started or not visible in the repo.

## Overview/Package Type, Manager, Distro matrix
| Status    | Distro             | Package Generated | Manager | Container[E2E]    |
|-----------|--------------------|-------------------|---------|-------------------|
| Supported | Ubuntu 26,25,24,22 | .deb [x]          | apt     | dokken[x]         |
| Supported | Ubuntu 26,25,24,22 | ppa [x]           | apt     | dokken[ ]         |
| Supported | Debian             | .deb [x]          | apt     | dokken[ ]         |
| Supported | Linux Mint         | .deb [x]          | apt     | linuxmintd[ ]     |
| Supported | Fedora             | .rpm [x]          | dnf     | dokken[x]         |
| TODO      | Fedora             | copr [ ]          | dnf     | dokken[ ]         |
| Supported | RHEL               | .rpm [x]          | dnf     | dokken[ ]         |
| Supported | Rocky Linux        | .rpm [x]          | dnf     | dokken[ ]         |
| Supported | openSUSE           | .rpm [x]          | zypper  | dokken[ ]         |
| Supported | openSUSE           | obs [ ]           | zypper  | dokken[ ]         |
| Supported | Arch Linux         | .pkg.tar.zst [x]  | pacman  | archlinux[x]      |
| TODO      | Arch Linux         | aur [ ]           | pacman  | archlinux[ ]      |
| Supported | CachyOS            | .pkg.tar.zst [x]  | pacman  | cachyos[ ]        |
| Supported | Alpine Linux       | .apk [x]          | apk     | alpine[x]         |
| Supported | NixOS              | flake tarball [x] | nix     | nixos/nix[x]      |
| TODO      | Gentoo             | .ebuild [ ]       | portage | gentoo/portage[ ] |
| TODO      | Void               | .xbps [ ]         | xbps    | gvcatafesta[ ]    |
| TODO      | FreeBSD            | .pkg [ ]          | pkg     | freebsd[ ]        |
| Supported | macOS              | .pkg [x]          | os      | local[x]          |
| Supported | macOS              | homebrew formula  | brew    | local[x]          |
| Supported | Windows            | .msi [x]          | os      | local[x]          |
| In Review | Windows            | winget package    | winget  | local[ ]          |
| Supported | Windows            | scoop package     | scoop   | local[ ]          |
| In Review | Windows            | choco package     | choco   | local[ ]          |

### Repository and naming

- `[x]` Repository migrated to `nehemiaharchives/bbl`.
- `[x]` Package name `bbl`.

Current naming policy:

```text
package name: bbl
repository: nehemiaharchives/bbl
external package namespace: gnit where available, otherwise nehemiaharchives
```

## Installed Layout Decision

The old plan said:

```text
/usr/bin/bbl
$HOME/.bbl/bin/bbl-search-*
$HOME/.bbl/packs/*.zip
```

That is still correct for runtime/user data, but it is not a clean public package-manager install layout if the `.deb` writes directly into a specific user's home directory.

### Public package rule

For PPA/Homebrew/COPR/OBS/AUR/winget/Chocolatey, prefer:

```text
system package installs:
  /usr/bin/bbl or package-manager bin wrapper
  root-owned bundled assets only under a package-owned prefix, if bundled at all

runtime/user install creates or updates:
  $HOME/.bbl/bin/bbl-search-*
  $HOME/.bbl/packs/*.zip
```

Critical decision before PPA:

- Option A: package only the core `bbl` executable and let `bbl install` fetch helper binaries and packs.
- Option B: package `bbl`, `bbl-search-common`, and `webus.zip` under a root-owned package path such as `/usr/lib/bbl` or `/usr/share/bbl`, then let `bbl` copy/update them into `$HOME/.bbl` on first run.
- Do not publish a PPA package that installs files into `/home/ubuntu` or any other fixed user home.

## GitHub Release Artifacts

Current status:

- `[x]` `.github/workflows/publish.yml` exists.
- `[x]` Publishes from version tags matching `v[0-9]*.[0-9]*`.
- `[x]` Supports manual `workflow_dispatch` with tag/ref/draft/prerelease inputs.
- `[x]` Builds release archives for:
    - `linux-x64`
    - `macos-arm64`
    - `windows-x64`
- `[x]` Generates `SHA256SUMS.txt`.
- `[x]` Archives include the core `bbl` binary, search helper binaries, and pack `.zip` files.
- `[x]` Public GitHub Releases published (e.g. `v2.0`).
- `[x]` `.deb` uploaded as a GitHub Release asset.
- `[x]` macOS `.pkg` uploaded as a GitHub Release asset.
- `[x]` `.msi` uploaded as a GitHub Release asset.
- `[ ]` Release assets are not signed/notarized.

Required next changes:

1. Keep `SHA256SUMS.txt` covering every release asset.
2. Add signing/notarization for macOS and optional GPG/cosign for other assets.

## CI and Package E2E

Current status:

- `[x]` `ci.yml` has a package fixture matrix for Linux `.deb`, macOS `.pkg`, Homebrew fixture, Linux/macOS/Windows install fixtures, search tests, config tests, completion tests, and pack-install tests.
- `[x]` CI installs nFPM on Ubuntu when the `.deb` fixture is needed.
- `[x]` CI builds and uploads a `bbl-install-linux-deb-files` fixture artifact.
- `[x]` CI verifies `.deb` contents before Kitchen runs:
    - `/usr/bin/bbl`
    - `/home/ubuntu/.bbl/bin/bbl-search-common`
    - `/home/ubuntu/.bbl/packs/webus.zip`
- `[x]` CI rejects old/bad locations such as `/usr/local`, `/usr/lib/bbl`, `/usr/share/bbl`, and the old flat `/home/ubuntu/.bbl/webus.zip`.
- `[x]` Kitchen has a `deb_install` suite covering Ubuntu 26.04, 25.10, 24.04, and 22.04 in `kitchen.yml`.
- `[~]` GitHub Actions currently runs the `.deb` install job against the `deb-install-ubuntu-2604` instance to save time.
- `[~]` External package-manager install tests exist for Homebrew and Scoop (tested via CI fixture, not live channel). PPA is live and tested via `add-apt-repository`.

Required next changes:

1. Add a public-package variant of the `.deb` E2E test.
2. Add `apt install ./bbl-*.deb` test in addition to raw `dpkg -i` if dependency handling ever matters.
3. Add uninstall test:

   ```sh
   sudo apt remove bbl
   command -v bbl && exit 1 || true
   ```

4. Add optional purge/idempotency tests if the package ever creates system-owned config or asset directories.
5. Add `lintian` once Debian packaging becomes public-facing.
6. Periodically run the `deb_install` Kitchen suite against Ubuntu 22.04 and 24.04, not only 26.04.

## Linux `.deb`

Current status:

- `[x]` Gradle task `buildLinuxDeb` exists.
- `[x]` `.deb` generation uses nFPM.
- `[x]` Package metadata is generated from Gradle/version constants.
- `[x]` Current package name is `bbl`.
- `[x]` Current output path is `build/distributions/bbl-<version>-linux-amd64.deb`.
- `[x]` Current staged CI fixture renames the output to `bbl.deb`.
- `[x]` Current `.deb` installs `/usr/bin/bbl`.
- `[x]` Current `.deb` includes `bbl-search-common`.
- `[x]` Current `.deb` includes `webus.zip`.
- `[x]` Current `.deb` is uploaded as a GitHub Release asset.
- `[x]` Public layout: helper/pack assets bundled, first-run copy to `$HOME/.bbl`.
- `[x]` Release `.deb` upload is wired in `publish.yml`.
- `[x]` Checksum verified via `SHA256SUMS.txt`.

Required next changes:

1. Add package uninstall/upgrade tests.
2. Add `lintian` check once the release `.deb` layout stabilizes.

## Ubuntu PPA

Current status:

- `[x]` PPA repository published at `ppa:nehemiaharchives/bbl`. source is https://github.com/nehemiaharchives/bbl-ppa, page is https://launchpad.net/~nehemiaharchives/+archive/ubuntu/bbl
- `[x]` `sudo add-apt-repository ppa:nehemiaharchives/bbl` installs bbl on Ubuntu.
- `[x]` Debian packaging maintained in the PPA repository, outside this repo.
- `[x]` Supports Ubuntu 24.04+ via Launchpad automated builds.
- `[x]` Local nFPM `.deb` still exists for CI/Küchen fixture tests.

Required next changes:

- None. PPA is live and maintained out of tree.

## NixOS

Current status:

- `[x]` Gradle task `stageBblInstallLinuxNixFixture` exists.
- `[x]` Nix packaging is a generated flake fixture, not an nFPM package.
- `[x]` Targets `x86_64-linux` initially.
- `[x]` CI uses direct Docker (`nixos/nix:latest`) E2E, not Chef/Test Kitchen.
- `[x]` E2E validates `nix build`, runtime execution, first-run `$HOME/.bbl` asset copy, and `nix profile install`.
- `[x]` Publish workflow uploads `bbl-<tag>-linux-x64-nix-flake.tar.gz`.
- `[ ]` True NixOS module/system integration is intentionally not included yet.

Required next changes:

1. Add NixOS module integration when NixOS system-level configuration is needed.
2. Consider publishing to a Nix flake registry or GitHub flake URL for easy `nix profile install github:nehemiaharchives/bbl`.
3. Add `aarch64-linux` platform support if there is demand.
4. Consider adding a NixOS container test after a full NixOS VM approach is available.

## Gentoo Linux
TODO fill this section using https://hub.docker.com/r/gentoo/portage as dokken image

## macOS `.pkg`

Current status:

- `[x]` Gradle task family for unsigned macOS `.pkg` exists.
- `[x]` Host task `buildMacosPkg` exists.
- `[x]` CI has a `pkg_install_macos` Kitchen suite.
- `[x]` `.pkg` fixture currently packages:
    - `bbl`
    - `bbl-search-common`
    - `webus.zip`
    - wrapper at `/usr/local/bin/bbl`
    - assets under `/usr/local/libexec/bbl`
- `[ ]` `.pkg` is not uploaded by `publish.yml`.
- `[ ]` `.pkg` is unsigned.
- `[ ]` `.pkg` is not notarized.

Required next changes:

1. Decide whether unsigned `.pkg` should be a prerelease artifact or stay CI-only.
2. Add `.pkg` upload to `publish.yml` if useful.
3. Add Developer ID signing and notarization before recommending `.pkg` to normal macOS users.
4. Keep Homebrew as the primary macOS package-manager path unless `.pkg` becomes signed/notarized.

## Homebrew

Current status:

- `[x]` Public tap repo: `nehemiaharchives/homebrew-bbl`. repo: https://github.com/nehemiaharchives/homebrew-bbl
- `[x]` `publish.yml` has `publish-homebrew-tap` job that pushes formula to the tap repo after each release.
- `[x]` Formula uses GitHub Release URLs and SHA256.
- `[x]` Dual-architecture: publishes separate macOS Arm64 and x64 assets.
- `[x]` Formula installs `bbl` and `bbl-search-common` into `libexec`, packs into `prefix/packs`, generates wrapper.
- `[x]` Shell completions bundled.
- `[x]` Install: `brew install nehemiaharchives/bbl/bbl`.
- `[x]` CI has `homebrew_install_macos` fixture test.
- `[x]` Secret `HOMEBREW_TAP_TOKEN` is wired in CI.

Required next changes:

- None. Homebrew tap is live and automated.

## Windows ZIP / Scoop / winget / Chocolatey

### Windows ZIP

Current status:

- `[x]` `publish.yml` builds `windows-x64.zip`.
- `[x]` Windows ZIP contains `bbl.exe`, search helper `.exe` files, and pack `.zip` files.
- `[x]` CI covers Windows local install, search, config, and completion tests.
- `[x]` PowerShell completion fixture exists.
- `[ ]` No Windows installer exists.

### Scoop

Current status:

- `[x]` Public Scoop bucket: `nehemiaharchives/bbl-scoop-bucket`.
- `[x]` `bucket/bbl.json` exists with GitHub Release URL and SHA256.
- `[x]` Install: `scoop bucket add bbl https://github.com/nehemiaharchives/bbl-scoop-bucket && scoop install bbl`.
- `[x]` README documents the install command.

Required next changes:

- None. Scoop bucket is live and maintained out of tree.

### winget

Current status:

- `[x]` `publish.yml` creates `bbl-winget.zip` release artifact.
- `[x]` MSI installer exists (`build/distributions/*.msi`).
- `[x]` Package identifier: `GNIT.bbl`.
- `[~]` Manifests submitted to `microsoft/winget-pkgs` — **under review**, waiting for approval. PR: https://github.com/microsoft/winget-pkgs/pull/394507
- `[ ]` Not yet available via `winget install Gnit.Bbl`.

Required next changes:

1. Wait for `microsoft/winget-pkgs` PR review and merge.
2. After approval, test:

   ```powershell
   winget install Gnit.Bbl
   bbl -v
   winget uninstall Gnit.Bbl
   ```

3. Add automated manifest update to `publish.yml` after reviews are complete.

### Chocolatey

Current status:

- `[~]` Chocolatey package submitted to the community repository — **under review**, waiting for moderation/approval. source is https://github.com/nehemiaharchives/bbl-chocolatey-package, site is https://community.chocolatey.org/packages/bbl/2.0.0
- `[ ]` No Chocolatey publish workflow in this repo (submission done manually).

Required next changes:

1. Wait for Chocolatey community repository review and approval. repo is 
2. After approval, test:

   ```powershell
   choco install bbl
   bbl -v
   bbl john 3:16
   choco uninstall bbl
   ```

3. Optionally add automated publish workflow with `CHOCOLATEY_API_KEY`. If decided, add secret to GitHub Actions.

## Fedora COPR / RPM

Current status:

- `[ ]` No `packaging/rpm/` directory exists.
- `[ ]` No `bbl.spec` exists.
- `[ ]` No COPR project/publish workflow exists.
- `[x]` nFPM generates rpm

Required next changes:

1. Decide whether RPM should be generated by nFPM or by a hand-written spec.
2. If using a spec, add:

   ```text
   packaging/rpm/bbl.spec
   ```

3. Decide whether COPR builds from source or from prebuilt GitHub Release payloads.
4. Start with prebuilt payload if Kotlin/Native source builds are too expensive.
5. Test locally with `rpmbuild` or `mock`.
6. Publish with:

   ```sh
   copr-cli build <project> <src.rpm>
   ```

7. Test:

   ```sh
   sudo dnf install bbl
   bbl -v
   bbl john 3:16
   sudo dnf remove bbl
   ```

8. Required secret:

   ```text
   COPR_CONFIG
   ```

## openSUSE OBS / zypper

Current status:

- `[ ]` No OBS files exist.
- `[ ]` No openSUSE-specific RPM adjustments exist.
- `[ ]` No OBS publish workflow exists.

Required next changes:

1. Reuse the RPM spec where possible.
2. Add OBS metadata such as `_service` if source tarballs are pulled from GitHub Releases.
3. Build locally with:

   ```sh
   osc build
   ```

4. Test Leap and Tumbleweed.
5. Publish with `osc commit` or CI-driven `osc`.
6. Required secret:

   ```text
   OBS_USERNAME
   OBS_PASSWORD_OR_TOKEN
   ```

## Arch Linux AUR / pacman

Current status:

- `[ ]` No `packaging/aur/PKGBUILD` exists.
- `[ ]` No AUR repository exists.

Required next changes:

1. Create AUR package repository:

   ```text
   bbl-bin
   ```

   Use `bbl-bin`.

2. Add `packaging/aur/PKGBUILD`.
3. Download Linux release archive or public `.deb` payload from GitHub Releases.
4. Verify `sha256sums`.
5. Install `bbl` to `/usr/bin/bbl`.
6. Decide how helper binaries/packs are handled:
    - rely on `bbl install`, or
    - install root-owned defaults and copy to `$HOME/.bbl`.
7. Validate:

   ```sh
   makepkg --syncdeps --cleanbuild
   sudo pacman -U bbl-bin-*.pkg.tar.zst
   bbl -v
   bbl john 3:16
   sudo pacman -R bbl-bin
   ```

8. Required secret:

   ```text
   AUR_SSH_PRIVATE_KEY
   ```

## Documentation Cleanup

Current status:

- `[x]` `PUBLISH.md` documents GitHub Release archive publishing.
- `[x]` README has install instructions for Homebrew, Scoop, `.deb`, `.rpm`, Arch, Alpine, Nix, macOS `.pkg`, and Windows `.msi`.
- `[x]` Release download table in README with links to GitHub Release assets.
- `[~]` `PUBLISH.md` may still need path cleanup if it mentions the old pack directory.

Required next changes:

1. Add a short "Pack/resource compatibility" note:
    - CLI version tag
    - pack manifest compatibility
    - helper binary compatibility
2. Add troubleshooting notes for:
    - missing `bbl-search-common`
    - missing packs
    - shell completion install
    - Windows PowerShell completion

## Recommended Next Order

1. [x] Rename `TOOD_PACKAGES.md` to `TODO_PACKAGES.md`.
2. [x] Fix `PUBLISH.md` path/reference drift.
3. [x] Decide public `.deb` layout (bundled assets + first-run copy).
4. [x] Split `.deb` fixture vs release tasks if needed.
5. [x] Add `.deb` artifact upload to `publish.yml`.
6. [x] Run CI and publish first GitHub Release for `v2.0`.
7. [x] Create Homebrew tap.
8. [x] Create Scoop bucket.
9. [x] Publish PPA.
10. [ ] Add AUR `bbl-bin`.
11. [ ] Add RPM/COPR and OBS.
12. [~] winget — submitted, under review.
13. [~] Chocolatey — submitted, under review.
14. [ ] Add signing/notarization:
    - GitHub Release checksums are live.
    - macOS Developer ID signing/notarization later.
    - Optional cosign/minisign/GPG signatures for CLI archives and packages later.