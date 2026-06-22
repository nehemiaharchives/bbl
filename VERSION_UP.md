# bbl CLI Version Update Guide

The bbl CLI version is controlled by:

```text
core/src/commonMain/kotlin/org/gnit/bible/BblVersion.kt
```

Change `BblVersion.VERSION`. Use the GitHub release tag string, for example `v2.0`.

## Step-by-step

1. Edit `core/src/commonMain/kotlin/org/gnit/bible/BblVersion.kt`.

   ```text
   const val VERSION = "v2.0"
   ```

2. Check the derived URLs in the same file.

   `RELEASE_DOWNLOAD_URL` and server resource paths are built from `VERSION`. Usually they should not be edited directly.

3. Run the focused version tests from JetBrains when available.

   Existing useful run configurations:

   - `BblVersionTest.bblVersionUsesGithubReleaseTag`
   - `BblVersionTest.releaseDownloadUrlUsesPinnedReleaseTag`

   If you need wider coverage, run:

   - `bbl [jvmTest]`
   - `bbl [linuxX64Test]`

4. Update the existing bbl pack zip manifest JSON versions.

   `bbl install` checks each pack manifest `version` field and treats packs from another bbl version as incompatible. If the Lucene index contents can stay the same, do not rebuild the packs. Instead, update only the manifest JSON file inside each existing pack zip:

   ```shell
   ./gradlew updateBblPackManifestVersions
   ```

   Then verify committed pack zips match the current bbl version:

   ```shell
   ./gradlew verifyServerBblPackVersions
   ```

5. Rebuild bbl packs only when index contents need to change.

   If lucene-kmp index compatibility, analyzer behavior, source texts, or pack layout changed, regenerate all downloadable translations instead of using the manifest-only task:

   ```shell
   ./gradlew packBblAllTranslations
   ```

   Then verify committed pack zips match the current bbl version:

   ```shell
   ./gradlew verifyServerBblPackVersions
   ```

   If you are iterating on one translation before the full pack rebuild, use:

   ```shell
   ./gradlew packBblTranslation -Pbblpack.translation=<translation-code>
   ```

6. Refresh the bbl_install version fixture.

   Use the JetBrains Gradle run configuration:

   ```text
   bbl [stageBblInstallLinuxCliAllFixture]
   ```

   For only the shared version file, run the Gradle task `stageBblInstallVersionFixture`. It writes the current version to:

   ```text
   build/bblInstallFixtures/common/version.txt
   bbl_install/files/version.txt
   ```

7. If you changed Kotlin code and need installed CLI coverage, restage the target platform fixtures before Kitchen.

   Common Linux development path:

   ```shell
   ./gradlew stageBblInstallLinuxCliAllFixture
   cd bbl_install
   bundle exec kitchen list
   bundle exec kitchen verify <instance>
   ```

   Read `bbl_install/AGENTS.md` before editing or debugging Kitchen tests.

8. For publishing, create or run the release with the same version tag.

   ```shell
   git tag v2.0
   git push origin v2.0
   ```

   The release workflow expects tags like `v2.0`. See `PUBLISH.md` for the full release process.

## Quick checklist

- [ ] Update `BblVersion.VERSION`.
- [ ] Run focused `BblVersionTest` run configurations.
- [ ] Run `updateBblPackManifestVersions` so each existing pack manifest JSON uses the new version.
- [ ] Verify committed server zips with `verifyServerBblPackVersions`.
- [ ] Rebuild packs with `packBblAllTranslations` only if index contents need to change.
- [ ] Refresh `bbl_install/files/version.txt` through the staging task.
- [ ] Restage Kitchen fixtures before installed CLI verification.
- [ ] Publish with a matching git tag.
