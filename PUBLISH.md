# Publishing bbl-kmp CLI Releases

This project publishes CLI binaries through `.github/workflows/publish.yml`.

The workflow creates GitHub Release assets for:

- `linux-x64`
- `macos-arm64`
- `windows-x64`

Each archive contains `bbl`, the language-specific `bbl-search-*` helper executables, and the bundled `.zip` bblpack files from `server/src/main/resources/files/bblpacks`.

## Automatic Release

Push a version tag. bbl-kmp uses bare numeric tags, without a `v` prefix:

```sh
git tag 4.0.0
git push origin 4.0.0
```

The `Publish` workflow runs automatically for semantic version tags like `4.0.0`. It builds the release archives and creates a published GitHub Release with generated release notes.

## Manual Release

Manual releases can create the release tag from a selected ref.

1. Open GitHub Actions.
2. Select the `Publish` workflow.
3. Click `Run workflow`.
4. Enter the tag, for example `4.0.0`.
5. Enter the ref to build, for example `master`, an existing tag, or a commit SHA.
6. Choose whether to create a draft release and whether to mark it as a prerelease.

The manual path is useful for preparing a draft release before publishing it from the GitHub Releases UI.

## Assets

The workflow uploads these files to the GitHub Release:

- `bbl-<tag>-linux-x64.tar.gz`
- `bbl-<tag>-macos-arm64.tar.gz`
- `bbl-<tag>-windows-x64.zip`
- `SHA256SUMS.txt`

If a release already exists for the tag, the workflow replaces assets with the same names.

## Notes

- The release tag should use the `X.Y.Z` format, without a `v` prefix.
- Regenerate packs with `bbl pack` before publishing a version that changes lucene-kmp index compatibility.
- The workflow does not sign binaries yet.
- Windows distribution is currently ZIP-first and suitable for Scoop packaging.
- Run normal CI before publishing from a tag. The publish workflow builds release binaries, but it is not intended to replace PR and branch validation.
