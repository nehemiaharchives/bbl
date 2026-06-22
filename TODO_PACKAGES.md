# Package Distribution TODO

Last updated: 2026-06-20

This document tracks package distribution status for the `bbl` CLI based on the current `nehemiaharchives/bbl-kmp` repository state.

## Current Status Snapshot

Legend:

- `[x]` Done in the current repo.
- `[~]` Partly done or implemented only as a CI/local fixture.
- `[ ]` Not started or not visible in the repo.

## Overview/Package Type, Manager, Distro matrix
| Status    | Distro             | Package Generated | Manager | Container[E2E]    |
|-----------|--------------------|-------------------|---------|-------------------|
| Supported | Ubuntu 26,25,24,22 | .deb [x]          | apt     | dokken[x]         |
| TODO      | Ubuntu 26,25,24,22 | ppa [ ]           | apt     | dokken[ ]         |
| Supported | Debian             | .deb [x]          | apt     | dokken[ ]         |
| Supported | Linux Mint         | .deb [x]          | apt     | linuxmintd[ ]     |
| Supported | RHEL               | .rpm [x]          | dnf     | dokken[ ]         |
| Supported | Fedora             | .rpm [x]          | dnf     | dokken[x]         |
| Supported | Rocky Linux        | .rpm [x]          | dnf     | dokken[ ]         |
| Supported | openSUSE           | .rpm [x]          | zypper  | dokken[ ]         |
| Supported | openSUSE           | obs [ ]           | zypper  | dokken[ ]         |
| Supported | Arch Linux         | .pkg.tar.zst [x]  | pacman  | archlinux[x]      |
| TODO      | Arch Linux         | aur [ ]           | pacman  | archlinux[ ]      |
| Supported | CachyOS            | .pkg.tar.zst [x]  | pacman  | cachyos[ ]        |
| WIP       | Alpine Linux       | .apk [~]          | apk     | alpine[~]         |
| TODO      | NixOS              | .nix [ ]          | nix     | nixos/nix[ ]      |
| TODO      | Gentoo             | .ebuild [ ]       | portage | gentoo/portage[ ] |
| TODO      | Void               | .xbps [ ]         | xbps    | gvcatafesta[ ]    |
| TODO      | FreeBSD            | .pkg [ ]          | pkg     | freebsd[ ]        |
| Supported | macOS              | .pkg [x]          | os      | local[x]          |
| Supported | macOS              | homebrew formula  | brew    | local[x]          |
| Supported | Windows            | .msi [x]          | os      | local[x]          |
| WIP       | Windows            | winget package    | winget  | local[ ]          |
| TODO      | Windows            | scoop package     | scoop   | local[ ]          |
| TODO      | Windows            | choco package     | choco   | local[ ]          |

### Repository and naming

- `[x]` Development repository currently remains `nehemiaharchives/bbl-kmp`.
- `[~]` Runtime/version code is already preparing for migration to `nehemiaharchives/bbl`.
- `[~]` Keep using package name `bbl` where available; use `bbl-kmp` only as a fallback.

Current package naming policy:

```text
package name: bbl
fallback only if taken: bbl-kmp
development repository: nehemiaharchives/bbl-kmp
target release repository: nehemiaharchives/bbl
external package namespace: gnit where available, otherwise nehemiaharchives
```

Before external publishing, confirm whether release URLs should point to `nehemiaharchives/bbl-kmp` or `nehemiaharchives/bbl`. Do not publish package-manager manifests against `nehemiaharchives/bbl` until that repository has the required tags, release assets, and pack resources.

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
- `[ ]` No public GitHub Release has been published yet.
- `[ ]` `.deb` is not yet uploaded as a GitHub Release asset.
- `[ ]` macOS `.pkg` is not yet uploaded as a GitHub Release asset.
- `[ ]` Release assets are not signed/notarized.

Required next changes:

1. Update `PUBLISH.md` so the pack path matches the current repo layout: `resources/bblpacks`, not the older `server/src/main/resources/files/bblpacks`.
2. Add `buildLinuxDeb` or `stageBblInstallLinuxDebFixture` to the publish workflow after the public `.deb` layout is fixed.
3. Upload a versioned `.deb`, for example:

   ```text
   bbl-v2.0-linux-amd64.deb
   ```

4. Upload unsigned macOS `.pkg` as an artifact.
5. Keep `SHA256SUMS.txt` covering every release asset.
6. After first release, update Homebrew/Scoop/winget/Chocolatey manifests to stable release URLs.

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
- `[ ]` No external package-manager install tests exist yet because no external package channels are published.

Required next changes:

1. Add a public-package variant of the `.deb` E2E test after the install layout is fixed.
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
- `[~]` Current `.deb` is good enough as a local/CI installer fixture.
- `[ ]` Current `.deb` is not PPA-ready because it installs helper/pack assets into a fixed user home.

Required next changes before publishing `.deb` broadly:

1. Decide final public package layout:
    - core-only package, or
    - root-owned bundled assets plus first-run copy/update to `$HOME/.bbl`.
2. Remove `bblDebInstallUser`, `bblDebInstallGroup`, and `bblDebInstallHome` from the public package path, or limit them to test-only fixture tasks.
3. Split tasks if necessary:

   ```text
   buildLinuxDebFixture      # may install into /home/ubuntu for Kitchen-only tests
   buildLinuxDebRelease      # public layout, no fixed user home
   ```

4. Add release `.deb` upload to `publish.yml`.
5. Add checksum verification against release `.deb`.
6. Add package uninstall/upgrade tests.
7. Add `lintian` check once the release `.deb` layout stabilizes.

## Ubuntu PPA

Current status:

- `[ ]` No `packaging/debian/` directory exists.
- `[ ]` No Debian source package flow exists.
- `[ ]` No Launchpad upload flow exists.
- `[ ]` No PPA secrets are wired.
- `[~]` Local `.deb` exists via nFPM, but that is not the same as Launchpad source-package publishing.

Required next changes:

1. Decide whether PPA is still necessary now that nFPM `.deb` generation exists.
2. If yes, add Debian packaging under `packaging/debian/`:

   ```text
   control
   changelog
   rules
   install
   copyright
   source/format
   ```

3. Build a source package for Launchpad instead of only an nFPM binary package.
4. Use package/source name `bbl` if available.
5. Use Launchpad owner/team `gnit` if available, otherwise `nehemiaharchives`.
6. Use Ubuntu suffixes such as:

   ```text
   2.0-1~ppa1~ubuntu24.04.1
   ```

7. Required secrets when automated:

   ```text
   LAUNCHPAD_PPA
   GPG_PRIVATE_KEY
   GPG_PASSPHRASE
   GPG_KEY_ID
   DEBEMAIL
   DEBFULLNAME
   ```

8. Test install path:

   ```sh
   sudo add-apt-repository ppa:gnit/bbl
   sudo apt update
   sudo apt install bbl
   bbl -v
   bbl john 3:16
   sudo apt remove bbl
   ```

## NixOS
TODO fill this section using https://hub.docker.com/r/nixos/nix as dokken image

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

- `[x]` Gradle generates a Homebrew formula fixture for macOS Arm64 and x64.
- `[x]` CI has `homebrew_install_macos`.
- `[x]` Formula fixture installs `bbl` and `bbl-search-common` into `libexec`.
- `[x]` Formula fixture places `webus.zip` under the Homebrew prefix and copies it to `$HOME/.bbl/packs` on wrapper execution.
- `[ ]` No public Homebrew tap repo exists in this repo.
- `[ ]` Formula still uses fixture-local `file://__BBL_HOMEBREW_ARCHIVE__`.
- `[ ]` Formula is not wired to GitHub Release URLs.

Required next changes:

1. Create tap repository, preferably:

   ```text
   nehemiaharchives/homebrew-bbl
   ```

   or another stable name you choose.

2. Convert generated formula from fixture-local file URL to a GitHub Release URL.
3. Decide final release repository before publishing the formula.
4. Add formula update automation after GitHub Release publish succeeds.
5. Test:

   ```sh
   brew install --build-from-source ./Formula/bbl.rb
   brew test bbl
   bbl john 3:16
   bbl search God limit 1
   ```

6. Required secret if automation pushes to the tap:

   ```text
   HOMEBREW_TAP_GITHUB_TOKEN
   ```

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

- `[ ]` No Scoop bucket repository exists.
- `[ ]` No `bucket/bbl.json` exists.

Required next changes:

1. Create a Scoop bucket repository, for example:

   ```text
   nehemiaharchives/scoop-bbl
   ```

2. Add `bucket/bbl.json`.
3. Point `url` to the GitHub Release Windows ZIP.
4. Include SHA256 from `SHA256SUMS.txt`.
5. Add `bin` entry for `bbl.exe`.
6. Add `checkver` and `autoupdate` after release naming stabilizes.
7. Test:

   ```powershell
   scoop install .\bucket\bbl.json
   bbl -v
   bbl john 3:16
   bbl search God limit 1
   scoop uninstall bbl
   ```

### winget

Current status:

- `[ ]` No winget manifests exist.
- `[ ]` No MSI/MSIX installer exists.
- `[~]` A ZIP-based winget package may be possible, but the user experience is usually better with a stable installer or portable manifest.

Required next changes:

1. Decide whether winget should use portable ZIP first or wait for MSI/MSIX.
2. Use package identifier:

   ```text
   GNIT.bbl
   ```

   if available, otherwise:

   ```text
   NehemiahArchives.bbl
   ```

3. Generate manifests with `wingetcreate`.
4. Validate:

   ```powershell
   winget validate <manifest-dir>
   winget install --manifest <manifest-dir>
   bbl -v
   winget uninstall bbl
   ```

5. Submit to `microsoft/winget-pkgs` only after manual manifest install works.

### Chocolatey

Current status:

- `[ ]` No Chocolatey package files exist.
- `[ ]` No Chocolatey publish workflow exists.

Required next changes:

1. Add:

   ```text
   packaging/chocolatey/bbl.nuspec
   packaging/chocolatey/tools/chocolateyinstall.ps1
   packaging/chocolatey/tools/chocolateyuninstall.ps1
   ```

2. Download Windows release ZIP and verify SHA256.
3. Install/shim `bbl.exe` onto PATH.
4. Test:

   ```powershell
   choco pack
   choco install bbl --source .
   bbl -v
   bbl john 3:16
   choco uninstall bbl
   ```

5. Required secret:

   ```text
   CHOCOLATEY_API_KEY
   ```

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

   Use `bbl-kmp-bin` only if `bbl-bin` is unavailable.

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

- `[~]` `PUBLISH.md` documents GitHub Release archive publishing.
- `[ ]` `PUBLISH.md` does not yet document `.deb`, `.pkg`, Homebrew tap, or Windows package-manager publishing.
- `[ ]` `PUBLISH.md` still needs path cleanup if it mentions the old pack directory.
- `[ ]` README does not yet have package install instructions for release users.

Required next changes:

1. Update `PUBLISH.md` after deciding final release repository.
2. Add install instructions to README only after the first public release exists.
3. Add a short "Pack/resource compatibility" note:
    - CLI version tag
    - pack manifest compatibility
    - helper binary compatibility
4. Add troubleshooting notes for:
    - missing `bbl-search-common`
    - missing packs
    - shell completion install
    - Windows PowerShell completion

## Recommended Next Order

1. [x] Rename `TOOD_PACKAGES.md` to `TODO_PACKAGES.md`.
2. Fix `PUBLISH.md` path/reference drift.
3. Decide public `.deb` layout:
    - core-only, or
    - root-owned bundled assets plus first-run copy.
4. [~] Split `.deb` fixture vs release tasks if needed.
5. Add `.deb` artifact upload to `publish.yml`.
6. Run CI and publish first draft GitHub Release for `v2.0`.
7. Create Homebrew tap using the current formula fixture as the starting point.
8. Create Scoop bucket using the current Windows ZIP release asset.
9. After release URLs are stable, add AUR `bbl-bin`.
10. Only after the `.deb` layout is public-grade, start PPA.
11. Add RPM/COPR and OBS after Debian/Homebrew/Scoop are stable.
12. Add winget and Chocolatey after the Windows ZIP/installer decision is stable.
13. Add signing/notarization:
    - GitHub Release checksums now.
    - macOS Developer ID signing/notarization later.
    - Optional cosign/minisign/GPG signatures for CLI archives and packages later.