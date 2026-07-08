# Codex CLI Prompt: Build and Release BBL Android Embedded Edition APKs

You are working in the `nehemiaharchives/bbl` repository.

Goal: redesign Android app Bible-pack resource generation and release automation so GitHub Releases can publish many APK editions, each with a controlled set of embedded offline Bible packs.

This is not a "make it simpler" task. The requirement is ambitious on purpose. Make the hard path practical, deterministic, maintainable, and testable.

## Non-negotiable product requirement

The Android app must support GitHub Release APK editions:

1. One APK per single embedded translation.
2. One APK per unordered pair of two embedded translations.
3. Four regional APKs:
   - Western edition
   - East Asian edition
   - SEA edition
   - South Asia edition
4. All non-embedded translations in a given APK must be treated as downloadable, not bundled in that APK.
5. Google Play Store is not targeted for now.
6. GitHub Releases are the distribution target now.
7. F-Droid support should remain possible later, so do not design something that only works with proprietary Play delivery.

## Important current repo context

Inspect the repo first. Do not assume paths blindly.

Known relevant files at the time this prompt was written:

- `core/src/commonMain/kotlin/org/gnit/bible/SupportedTranslation.kt`
- `app/shared/build.gradle.kts`
- `app/androidApp/build.gradle.kts`
- `app/shared/src/commonMain/kotlin/org/gnit/bible/app/ComposeBibleResourcesReader.kt`
- `.github/workflows/ci-app.yml`
- `.github/workflows/publish.yml`
- `resources/bblpacks/*.zip`
- `app/shared/src/commonMain/composeResources/files/bblpacks/**`

Current architecture problem:

- `SupportedTranslation.kt` currently stores `embedded` as a fixed enum constructor Boolean.
- `ComposeBibleResourcesReader` reads paths like `files/bblpacks/<translation>/<translation>.<book>.<chapter>.txt`.
- If a translation directory is removed from Compose resources but the UI still tries to read it as embedded, the app can fail at runtime.
- The Android app release build currently needs proper artifact naming and release signing discipline.

Fix the architecture so "embedded" is edition-specific, not globally fixed.

## Translation codes

Use exactly these current translation codes unless the repo has changed:

```text
webus
kjv
rvr09
tb
delut
lsg
sinod
svrj
rdv24
ubg
ubio
sven
cunp
krv
jc
ayt
th1971
irvhin
irvben
irvtam
npiulb
abtag
kttv
irvguj
irvmar
irvtel
irvurd
```

Important: the code is `rdv24`, not `rvd24`.

The "IRV family" for South Asia means:

```text
irvhin
irvben
irvtam
irvguj
irvmar
irvtel
irvurd
```

## Required APK edition model

Use these edition IDs and embedded-code sets.

### Single embedded editions

For every supported translation code, create one single-code edition.

Examples:

```text
webus -> [webus]
jc -> [jc]
krv -> [krv]
irvhin -> [irvhin]
```

APK filename format:

```text
bbl-app-<edition-id>.apk
```

Examples:

```text
bbl-app-webus.apk
bbl-app-jc.apk
bbl-app-irvhin.apk
```

### Pair embedded editions

For every unordered pair of supported translation codes, create one pair edition.

Edition ID format:

```text
<first-code>-<second-code>
```

Use the canonical `SupportedTranslation.entries` order, not alphabetical order, so output is deterministic and stable.

Examples:

```text
webus-kjv -> [webus, kjv]
webus-jc -> [webus, jc]
```

Do not generate both `webus-jc` and `jc-webus`.
For the cases of paring with English-NoneEnglish bibles, only pair with webus, do not use kjv for pair editions.

APK filename format:

```text
bbl-app-<edition-id>.apk
```

### Regional editions

Hardcode these regional editions:

```text
western -> [
  webus, kjv, rvr09, tb, delut, lsg,
  sinod, svrj, rdv24, ubg, ubio, sven
]

east-asia -> [
  webus, kjv, cunp, krv, jc
]

sea -> [
  webus, kjv, ayt, th1971, abtag, kttv
]

south-asia -> [
  webus, kjv,
  irvhin, irvben, irvtam, irvguj, irvmar, irvtel, irvurd,
  npiulb
]
```

APK filename format:

```text
bbl-app-western.apk
bbl-app-east-asia.apk
bbl-app-sea.apk
bbl-app-south-asia.apk
```

Expected count if there are 27 translations:

```text
single editions: 27
pair editions: 26 * 26 / 2 = 338
regional editions: 4
total APK editions: 369
```

Do not hardcode the pair list manually. Generate pairs programmatically from the canonical translation order.

## Architecture design

### [x] 1. Make embedded status edition-specific

Refactor `SupportedTranslation.kt`.

The translation metadata must remain stable. Whether a translation is embedded must be decided by the current APK edition.

Recommended shape:

```kotlin
enum class SupportedTranslation(
    val translation: Translation,
) {
    WEBUS(...),
    KJV(...),
    // ...
}
```

Then add edition-aware helpers, either in `SupportedTranslation.kt` or a nearby file:

```kotlin
data class BblAppEdition(
    val id: String,
    val displayName: String,
    val embeddedCodes: Set<String>,
    val kind: Kind,
) {
    enum class Kind { single, pair, regional }
}
```

Expose:

```kotlin
val SupportedTranslation.Companion.defaultAppEditionId: String
val SupportedTranslation.Companion.allAppEditions: List<BblAppEdition>
fun SupportedTranslation.Companion.appEditionById(id: String): BblAppEdition
fun SupportedTranslation.Companion.embeddedTranslationsFor(codes: Set<String>): List<Translation>
fun SupportedTranslation.Companion.downloadableTranslationsFor(codes: Set<String>): List<Translation>
```

The default app edition can be `webus`.

Keep existing `all`, `byCode`, and `defaultTranslationOf(language)` behavior unless there is a strong reason to change it.

Remove the old fixed `embedded: Boolean` as the source of truth. If keeping deprecated compatibility properties, make them explicitly default-edition-only and do not use them in app availability logic.

### [x] 2. Generate build-time embedded registry

Add generated Kotlin source for the current APK edition.

Target generated file:

```text
app/shared/build/generated/bblAppEdition/<edition-id>/kotlin/org/gnit/bible/app/EmbeddedPackRegistry.kt
```

Generated content shape:

```kotlin
package org.gnit.bible.app

object EmbeddedPackRegistry {
    const val editionId: String = "webus-jc"
    const val editionDisplayName: String = "WEBUS + JC"
    val embeddedCodes: Set<String> = setOf("webus", "jc")
}
```

Add the generated Kotlin directory to `commonMain`.

The app UI and app availability code should use `EmbeddedPackRegistry.embeddedCodes` to decide:

- Built-in
- Installed/downloaded
- Downloadable

### [x] 3. Move app embedded resources to generated resources

Do not keep all pack directories committed under:

```text
app/shared/src/commonMain/composeResources/files/bblpacks/
```

Instead, use canonical pack artifacts from:

```text
resources/bblpacks/<code>.zip
```

Generate selected Compose resources during Gradle build.

Recommended generated resource root:

```text
app/shared/build/generated/bblAppEdition/<edition-id>/composeResources/files/bblpacks/<code>/
```

Register the generated resource directory with Compose Multiplatform resources for `commonMain`.

The current build already uses `compose.resources.customDirectory("commonTest", ...)` for test packs, so mirror that pattern for `commonMain`.

Implementation detail:

1. Inspect zip structure first:

   ```bash
   jar tf resources/bblpacks/webus.zip | head -50
   ```

2. Ensure generated output matches what `ComposeBibleResourcesReader` expects:

   ```text
   files/bblpacks/<code>/<code>.<book>.<chapter>.txt
   files/bblpacks/<code>/index/...
   files/bblpacks/<code>/<code>.0.manifest.json
   ```

3. If the zip already contains a top-level `<code>/` directory, unzip into:

   ```text
   .../composeResources/files/bblpacks/
   ```

4. If the zip contains flat files, unzip into:

   ```text
   .../composeResources/files/bblpacks/<code>/
   ```

The Gradle task must clean its generated edition directory before copying so stale packs are impossible.

### [x] 4. Add Gradle properties

Support both edition ID and explicit embedded code override.

Required properties:

```text
-Pbbl.app.edition=<edition-id>
-Pbbl.app.embeddedCodes=webus,jc
```

Rules:

1. If `bbl.app.embeddedCodes` is provided, it wins.
2. Else use `bbl.app.edition`.
3. Else default to `webus`.
4. Validate all codes against `SupportedTranslation`/Gradle edition catalog.
5. Fail fast with a clear error for unknown codes or unknown edition ID.

Example commands:

```bash
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus-jc
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=western
./gradlew :app:androidApp:assembleRelease -Pbbl.app.embeddedCodes=webus,jc
```

### [x] 5. Add Gradle tasks for edition discovery

Add tasks that CI can call:

```text
:listBblAppEditions
:printBblAppEditionIds
:printBblAppEditionIdsForShard
:generateBblAppEditionReadmeTable
```

The exact task project can be root or `:app:androidApp`; choose the cleanest implementation.

Required behavior:

```bash
./gradlew -q printBblAppEditionIds
```

Outputs one edition ID per line, stable order:

```text
webus
kjv
...
webus-kjv
webus-rvr09
...
western
east-asia
sea
south-asia
```

Shard task:

```bash
./gradlew -q printBblAppEditionIdsForShard \
  -Pbbl.app.shardIndex=0 \
  -Pbbl.app.shardCount=16
```

Outputs only the edition IDs assigned to that shard.

Use deterministic modulo sharding by index:

```text
editionIndex % shardCount == shardIndex
```

### [x] 6. Android release build configuration

Update `app/androidApp/build.gradle.kts`.

Release APKs for GitHub must be installable, so release signing must be handled.

Implement signing like this:

- Use release signing if all required environment variables are present.
- If they are missing, allow local debug/profile builds, but make publish release fail clearly before uploading unsigned APKs.

Recommended env vars:

```text
BBL_ANDROID_KEYSTORE_BASE64
BBL_ANDROID_KEYSTORE_PASSWORD
BBL_ANDROID_KEY_ALIAS
BBL_ANDROID_KEY_PASSWORD
```

In CI, decode:

```bash
echo "$BBL_ANDROID_KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/bbl-release.keystore"
```

Set Gradle properties or env vars so Android Gradle Plugin signs release APKs.

Also enable size optimization for release:

```kotlin
buildTypes {
    getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```

If R8 rules are needed for Compose/Lucene/Kotlin serialization, add them intentionally and keep them narrow.

### [x] 7. Runtime availability behavior

Any translation not embedded in `EmbeddedPackRegistry.embeddedCodes` must not be read from Compose resources.

Implement or verify this behavior:

```kotlin
sealed interface PackAvailability {
    data object BuiltIn : PackAvailability
    data object Installed : PackAvailability
    data object Downloadable : PackAvailability
}
```

Selection rule:

1. Built-in if code is in `EmbeddedPackRegistry.embeddedCodes`.
2. Installed if pack exists in Android app-private storage.
3. Downloadable otherwise.

Android storage should be app-private, not `$HOME/.bbl`:

```text
context.filesDir/bblpacks/<code>.zip
```

If the app does not yet have full in-app download implementation, at least prevent crashes:

- show "Download required" state
- disable opening the non-installed pack
- keep WEBUS or another embedded translation active

Do not let a downloadable-but-not-installed translation call `Res.readBytes("files/bblpacks/...")`.

### [x] 8. Reader design

Keep `ComposeBibleResourcesReader` for built-in packs.

Add a layered app reader if needed:

```kotlin
class AppBibleResourcesReader(
    private val embeddedReader: ComposeBibleResourcesReader,
    private val installedReader: InstalledPackResourcesReader,
    private val availability: PackAvailabilityProvider,
) : BibleResourcesReader {
    // route reads based on translation code
}
```

Do not overcomplicate it. The important rule is:

- embedded -> Compose resources
- installed zip/local -> installed reader
- missing -> user-facing downloadable state, not resource exception

### [x] 9. CI workflow redesign: `.github/workflows/ci-app.yml`

Do not build all 382 APKs on every normal CI run. That is release work.

CI should prove the mechanism with representative builds:

Required CI app checks:

1. `:app:shared:jvmTest`
2. `:app:androidApp:assembleDebug` or equivalent debug build
3. Release-smoke build for these editions:
   - `webus`
   - `jc`
   - `webus-jc`
   - `western`
   - `east-asia`
   - `sea`
   - `south-asia`

Use a small matrix for smoke editions.

For each smoke APK:

1. Build:

   ```bash
   ./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=<edition>
   ```

2. Copy/rename output:

   ```bash
   bbl-app-<edition>.apk
   ```

3. Inspect the APK contents:

   ```bash
   unzip -l bbl-app-<edition>.apk | grep 'files/bblpacks/'
   ```

4. Verify included packs match the edition.
5. Verify an obviously unrelated pack is not included.

For example:

- `webus` APK must include `webus`.
- `webus` APK must not include `jc`.
- `webus-jc` APK must include both `webus` and `jc`.
- `east-asia` APK must include `webus`, `kjv`, `cunp`, `krv`, `jc`.
- `east-asia` APK must not include `lsg` or `irvhin`.

If release signing secrets are unavailable in normal CI, use debug signing or an internal unsigned smoke output only for CI, but publish workflow must produce signed release APKs.

### [x] 10. Publish workflow redesign: `.github/workflows/publish.yml`

Add a new job:

```text
build-android-app-assets
```

It should run on `ubuntu-latest`.

It should:

1. Resolve tag/ref the same way existing publish jobs do.
2. Checkout the release ref.
3. Set up JDK 24.
4. Set up Android SDK.
5. Set up Gradle.
6. Configure release signing from GitHub secrets.
7. Build every app edition APK.
8. Upload the APKs as workflow artifacts.

Do not use one matrix job per edition. The edition count is large. Use shard jobs.

Recommended matrix:

```yaml
strategy:
  fail-fast: false
  matrix:
    shard: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
```

Each shard job should run:

```bash
set -euo pipefail

mkdir -p dist/android-apks

./gradlew -q printBblAppEditionIdsForShard \
  -Pbbl.app.shardIndex="${{ matrix.shard }}" \
  -Pbbl.app.shardCount="16" > "$RUNNER_TEMP/edition-ids.txt"

while IFS= read -r edition; do
  [ -n "$edition" ] || continue

  ./gradlew :app:androidApp:assembleRelease \
    -Pbbl.app.edition="$edition"

  source_apk="$(find app/androidApp/build/outputs/apk/release -name '*.apk' | head -n 1)"
  test -f "$source_apk"

  cp "$source_apk" "dist/android-apks/bbl-app-$edition.apk"

  # Optional but recommended: remove APK output before next edition to avoid stale pick-up.
  rm -f app/androidApp/build/outputs/apk/release/*.apk
done < "$RUNNER_TEMP/edition-ids.txt"
```

Upload artifacts:

```yaml
- uses: actions/upload-artifact@v7
  with:
    name: android-apks-shard-${{ matrix.shard }}
    path: dist/android-apks/*.apk
    if-no-files-found: error
    retention-days: 7
```

Then update `publish-release` job:

- add `build-android-app-assets` to `needs`
- download `android-apks-*`
- merge into `dist`
- include them in checksum generation
- upload with the existing release action

Current publish workflow already downloads `packaged-*` artifacts and uploads `dist/*`. Extend that pattern instead of rewriting everything.

### [x] 11. Release README/table generation

Because there are more than 300 apk files generated, also versioning of the app is different from cli version, we will create separate release cycle and release tag format than bbl cli.
the git tag for releasing bbl-app will be for example, "app-v4.0" where the version number portion is taken from app\androidApp\build.gradle.kts android { defaultConfig { versionName = "4.0" } }

Add generated Markdown table support.

Add markers to `app/README.md` if absent:

```markdown
<!-- BBL_ANDROID_APK_EDITIONS_START -->
<!-- BBL_ANDROID_APK_EDITIONS_END -->
```

Add a Gradle or script task:

```bash
./gradlew generateBblAndroidApkEditionTable -Pbbl.release.tag=vX.Y.Z
```

Generated table columns:



```markdown

## Single Language Editions
columns: translation code, translation name, translation native name, apk file name with hyper link 

## Bilingual Editions
matrix of language pairs with 26 language names rows  x 26 columns language names with cells language code combination such as en-ja, en-ko, en-zh for each cells hyper linked with release apk files for each.

## Reginal Editions
| Edition | Embedded translations | APK |
|---|---|---|
| Western | WEBUS, KJV, RVR09, TB, DELUT, LSG, SINOD, SVRJ, RDV24, UBG, UBIO, SVEN | [`bbl-app-western.apk`](https://...) |
...

```

For release URL table, generate links like:

```markdown
https://github.com/nehemiaharchives/bbl/releases/download/<tag>/bbl-app-webus.apk
```

If the table is too large for the root README, generate a dedicated file instead:

```text
docs/android-apk-editions.md
```

Then keep README concise and link to that file.

Recommended:

- `app/README.md` gets a short Android section and links to `docs/android-apk-editions.md`.
- `docs/android-apk-editions.md` gets the full table.

### [x] 12. F-Droid preparation

Do not implement F-Droid metadata in this task unless it is already simple.

But keep these constraints:

1. Do not depend on Google Play services.
2. Do not require Play Feature Delivery.
3. Keep release flavor/edition logic controllable by Gradle properties.
4. Ensure F-Droid can build a selected default edition later, probably `webus` or a future `fdroid` edition.

Add a short note to docs:

```text
F-Droid will likely publish one default APK edition first. Other edition APKs are distributed through GitHub Releases.
```

### [x] 13. Tests and verification

Add or update tests.

Required tests:

1. Edition catalog test:
   - all edition IDs are unique
   - all embedded codes exist in `SupportedTranslation.byCode`
   - single edition count equals translation count
   - pair edition count equals `n * (n - 1) / 2`
   - regional editions contain exactly the required codes
   - `rdv24` exists and `rvd24` does not

2. Generated registry test:
   - build with `-Pbbl.app.edition=webus-jc`
   - verify generated `EmbeddedPackRegistry.kt` contains `webus` and `jc`

3. Resource generation test:
   - build or run task for `webus-jc`
   - verify generated Compose resources contain `files/bblpacks/webus`
   - verify generated Compose resources contain `files/bblpacks/jc`
   - verify generated Compose resources do not contain unrelated pack such as `krv`

4. APK contents smoke test:
   - build `webus`
   - build `webus-jc`
   - build `east-asia`
   - inspect with `unzip -l`

Required local commands before final answer:

```bash
./gradlew :core:commonTest
./gradlew :app:shared:jvmTest
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus-jc
./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=western
./gradlew -q printBblAppEditionIds | wc -l
./gradlew -q printBblAppEditionIdsForShard -Pbbl.app.shardIndex=0 -Pbbl.app.shardCount=16
```

If a command is impossible because of existing repo/tooling issues, document the exact failure and the next fix.

### [x] 14. Do not break CLI packaging

The CLI release flow currently has its own strategy for packs. Do not regress it.

Important:

- Do not remove `resources/bblpacks/webus.zip`.
- Do not remove `resources/bblpacks/*.zip`.
- Do not change CLI installer behavior unless required.
- Android app edition generation should consume canonical pack zips, not change CLI pack packaging semantics.

### [x] 15. Avoid stale generated artifacts

Add generated paths to `.gitignore` if needed:

```text
app/shared/build/generated/bblAppEdition/
app/androidApp/build/bblAppEditions/
```

Do not commit generated pack directories or generated APKs.

### [x] 16. Implementation order

Work in this order:

1. [x] Inspect current repo and run baseline app build.
2. [x] Refactor `SupportedTranslation.kt` to remove fixed embedded truth and add edition catalog.
3. [x] Add edition catalog tests.
4. [x] Add generated `EmbeddedPackRegistry.kt`.
5. [x] Add generated Compose resource sync from selected pack zips.
6. [x] Make the app use `EmbeddedPackRegistry.embeddedCodes` for availability.
7. [x] Build `webus` APK.
8. [x] Build `webus-jc` APK.
9. [x] Build a regional APK.
10. [x] Add APK content verification script/task.
11. [x] Update `ci-app.yml` smoke builds.
12. [x] Update `publish.yml` sharded all-edition release build.
13. [x] Add `docs/android-apk-editions.md` generation.
14. [x] Run tests.
15. [x] Summarize changes and any remaining risks.

### [x] 17. Suggested file additions

You may add files like these if they fit the repo:

```text
core/src/commonMain/kotlin/org/gnit/bible/BblAppEdition.kt
core/src/commonTest/kotlin/org/gnit/bible/BblAppEditionTest.kt
app/shared/src/commonMain/kotlin/org/gnit/bible/app/PackAvailability.kt
app/shared/src/commonMain/kotlin/org/gnit/bible/app/AppBibleResourcesReader.kt
app/androidApp/scripts/verify-apk-packs.sh
docs/android-apk-editions.md
```

For Gradle build logic, choose one of these approaches:

Preferred if simple:

```text
buildSrc/src/main/kotlin/BblAppEditions.kt
```

Alternative:

```text
gradle/bbl-app-editions.gradle.kts
```

Avoid copying large edition lists between many files. One source of truth is strongly preferred.

### [x] 18. Acceptance criteria

The task is complete when:

1. `SupportedTranslation` no longer has one global hardcoded `embedded = true/false` truth for Android app packaging.
2. The Android app can be built with:

   ```bash
   ./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus
   ./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=webus-jc
   ./gradlew :app:androidApp:assembleRelease -Pbbl.app.edition=western
   ```

3. The generated APK file can be renamed deterministically as:

   ```text
   bbl-app-<edition-id>.apk
   ```

4. Generated APK contents include only the selected embedded packs.
5. Non-embedded translations are treated as downloadable/uninstalled, not as missing Compose resources.
6. CI validates representative editions.
7. Publish workflow builds and uploads all single, pair, and regional APK editions.
8. GitHub Release checksums include Android APKs.
9. Documentation includes the APK edition list or a generated table.
10. CLI release packaging still works.

## Final response required from Codex

When done, respond with:

1. Summary of changed files.
2. Exact edition count.
3. Exact commands run.
4. APK size comparison for at least:
   - `webus`
   - `webus-jc`
   - `western`
   - `east-asia` if built
5. Any failed commands and why.
6. Remaining manual setup, especially Android release signing secrets.
