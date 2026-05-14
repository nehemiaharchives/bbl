# Package Distribution TODO

## Shared Decisions

Use package-manager conventions for the core `bbl` executable, and let `bbl install` manage user-scoped packs and search helper binaries.

Package naming:

```text
package name: bbl
fallback only if taken: bbl-kmp
GitHub repository namespace: nehemiaharchives
GitHub package-channel repository style: nehemiaharchives/bbl-<channel>
external package namespace: gnit where available, otherwise nehemiaharchives
```

Check package name availability in each registry before publishing. Prefer the clean `bbl` package name whenever it is not already taken.

Linux package layout:

```text
/usr/bin/bbl
$HOME/.bbl/bin/bbl-search-*
$HOME/.bbl/packs/*.zip
```

macOS Homebrew layout:

```text
#{bin}/bbl
$HOME/.bbl/bin/bbl-search-*
$HOME/.bbl/packs/*.zip
```

Windows package layout:

```text
<package-manager-bin>\bbl.exe
%LOCALAPPDATA%\.bbl\bin\bbl-search-*.exe
%LOCALAPPDATA%\.bbl\packs\*.zip
```

## GitHub Release Artifacts

1. Build release archives for Linux, macOS, and Windows.
2. Include the core `bbl` binary and metadata such as checksums.
3. Keep search helper binaries and packs available as release assets for `bbl install`.
4. Generate SHA256 checksums for every archive and executable asset.
5. Add package E2E tests that install from the package artifact and run the same smoke searches as Kitchen.

## Ubuntu PPA

1. Add Debian packaging under `packaging/debian/`.
2. Create `control`, `changelog`, `rules`, `install`, `copyright`, and `source/format`.
3. Use Debian source and binary package name `bbl` if available.
4. Create a Launchpad owner/team named `gnit` if available, and publish to a PPA named `bbl`, giving users a `ppa:gnit/bbl` style repository. If `gnit` is taken, use the `nehemiaharchives` Launchpad namespace instead.
5. Install only the core Linux binary to `/usr/bin/bbl`.
6. Build a local `.deb` in GitHub Actions with `dpkg-buildpackage -us -uc -b`.
7. Test the `.deb` with `apt install ./bbl_*_amd64.deb`, `bbl -v`, search smoke tests, and package uninstall.
8. Create a signed source package for Launchpad once the local `.deb` is stable.
9. Upload to PPA with `dput ppa:gnit/bbl ../bbl_*_source.changes`, or the equivalent `nehemiaharchives` PPA if `gnit` is unavailable.
10. Use Ubuntu-specific version suffixes such as `0.3.0-1~ppa1~ubuntu24.04.1`.
11. Required secrets: `LAUNCHPAD_PPA`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `GPG_KEY_ID`, `DEBEMAIL`, `DEBFULLNAME`.

## Fedora COPR

1. Create a COPR project named `bbl` under a `gnit` account or group if available, otherwise under `nehemiaharchives`.
2. Add RPM packaging under `packaging/rpm/`.
3. Write a `bbl.spec` that installs the Linux binary to `/usr/bin/bbl`.
4. Decide whether COPR builds from source or from a prebuilt release payload; start with prebuilt payload if Kotlin/Native source builds are too expensive.
5. Build locally with `rpmbuild` or `mock` before uploading to COPR.
6. Add CI to generate a source RPM with `rpmbuild -bs`.
7. Publish to COPR with `copr-cli build <project> <src.rpm>`.
8. Test with `dnf install bbl`, `bbl -v`, smoke searches, and `dnf remove bbl`.
9. Required secrets: COPR API token or `~/.config/copr` contents.

## openSUSE OBS and zypper

1. Create an OBS project for `bbl`, preferably under a `gnit` namespace if available, otherwise under `nehemiaharchives`.
2. Reuse the RPM spec where possible, adjusting openSUSE dependencies and macros.
3. Add OBS metadata files such as `_service` if source tarballs are pulled from GitHub releases.
4. Install the Linux binary to `/usr/bin/bbl`.
5. Build locally with `osc build` for at least Leap and Tumbleweed targets.
6. Publish through OBS with `osc commit` or CI-driven `osc` commands.
7. Test with `zypper install bbl`, `bbl -v`, smoke searches, and `zypper remove bbl`.
8. Required secrets: OBS username and password or token.

## Arch Linux AUR and pacman

1. Create an AUR package repository named `bbl-bin` if available, using `bbl-kmp-bin` only as a fallback.
2. Add `packaging/aur/PKGBUILD`.
3. Download the Linux release artifact in `source=()`.
4. Verify checksums with `sha256sums`.
5. Install the binary to `/usr/bin/bbl` in `package()`.
6. Validate with `makepkg --syncdeps --cleanbuild`.
7. Test install with `pacman -U bbl-bin-*.pkg.tar.zst`, `bbl -v`, smoke searches, and `pacman -R bbl-bin`.
8. Publish to AUR by pushing to the AUR Git remote.
9. Required secrets: AUR SSH private key.

## Homebrew

1. Create a Homebrew tap repository such as `nehemiaharchives/bbl-homebrew`.
2. Add a formula named `Formula/bbl.rb`.
3. Use the macOS release archive and SHA256 checksum.
4. Install the executable with `bin.install "bbl"`.
5. Do not hardcode `/usr/local/bin` or `/opt/homebrew/bin`; Homebrew decides the prefix.
6. Add a formula `test do` block that runs `system "#{bin}/bbl", "-v"`.
7. Validate with `brew audit --strict`, `brew install --build-from-source` if applicable, and `brew test`.
8. Publish by pushing the formula update to the tap.
9. Required secrets: GitHub token with access to the tap repository.

## Scoop

1. Create a Scoop bucket repository such as `nehemiaharchives/bbl-scoop`.
2. Add `bucket/bbl.json`.
3. Point `url` to the Windows release archive.
4. Include SHA256 checksum and `bin` entry for `bbl.exe`.
5. Add `checkver` and `autoupdate` once the GitHub release naming is stable.
6. Test with `scoop install ./bucket/bbl.json`, `bbl -v`, smoke searches, and `scoop uninstall bbl`.
7. Publish by pushing the manifest update to the bucket.
8. Required secrets: GitHub token with access to the bucket repository.

## winget

1. Decide whether to publish to `microsoft/winget-pkgs` or a private source first.
2. Build a Windows installer or zip package with stable release URLs.
3. Generate manifests with `wingetcreate`.
4. Use package identifier `GNIT.bbl` if available, otherwise use `NehemiahArchives.bbl`.
5. Include package, installer, locale, version, publisher, license, and checksum metadata.
6. Validate with `winget validate`.
7. Test with `winget install --manifest <manifest-dir>`, `bbl -v`, smoke searches, and `winget uninstall`.
8. Submit to `microsoft/winget-pkgs` after manual install works.
9. Required secrets: GitHub token if automating PR creation.

## Chocolatey

1. Create Chocolatey package files under `packaging/chocolatey/`.
2. Add `.nuspec`, `tools/chocolateyinstall.ps1`, and `tools/chocolateyuninstall.ps1`.
3. Use Chocolatey package id `bbl` if available.
4. Download the Windows release archive and verify checksum in the install script.
5. Install or shim `bbl.exe` so it is available on PATH.
6. Validate locally with `choco pack` and `choco install bbl --source .`.
7. Test `bbl -v`, smoke searches, and `choco uninstall bbl`.
8. Publish with `choco push`.
9. Required secrets: Chocolatey API key.

## Suggested Order

1. GitHub release archives and checksums.
2. Ubuntu `.deb` and E2E install test.
3. Ubuntu PPA.
4. Homebrew tap.
5. Windows package channel: start with Scoop, then winget, then Chocolatey.
6. Fedora COPR and openSUSE OBS.
7. Arch AUR.
