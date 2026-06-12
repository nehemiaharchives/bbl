# bbl Design and Migration Plan

This document is the first design pass for migrating the working `bbl-kmp` project into the new `bbl` project structure.

The direction follows JetBrains' new Kotlin Multiplatform default: keep shared code in library modules, keep runnable platform applications in separate app modules, and add a root `core` module for code shared by both server and clients.

`Books.kt` and `BibleFilter.kt` are intentionally preserved. The historical `Created by Joel on 10/22/2014.` block in `Books.kt` stays.

## Points to be changed

### [x] 1. Give every module one responsibility

Change:

- `:core`
  - Own Bible domain model, parsing, filtering, translation metadata, pack/resource readers, installation/download state, search orchestration, and platform abstractions.
  - Must not depend on Compose, Android application APIs, Ktor server, Clikt, or packer tooling.
  - Keep package `org.gnit.bible` for migrated domain code to avoid churn.

- `:app:shared`
  - Own shared Compose UI only.
  - Depends on `:core`.
  - Replaces old `composeApp` shared UI code.
  - Should not own Bible text data or server downloadable packs.

- `:app:androidApp`
  - Android entry point, manifest, launcher resources, packaging.
  - Depends on `:app:shared`.
  - No domain logic.

- `:app:iosApp`
  - Xcode/Swift entry point that consumes `:app:shared`.
  - No copied Kotlin domain logic.

- `:app:desktopApp`
  - New desktop entry point and desktop packaging.
  - Depends on `:app:shared`.
  - This replaces the old desktop responsibility that was mixed into `composeApp`.

- `:server`
  - Ktor server entry point and routing.
  - Depends on `:core`.
  - Should serve generated/canonical pack resources, but should not be the canonical owner of raw Bible text.
  - Design target package is `org.gnit.bible.server`; current code now uses that package.

- `:cli:core`
  - User-facing `bbl` command.
  - Owns commands for reading, listing, installing, uninstalling, config, random verse, and delegating search.
  - Depends on `:core` and the minimal search dependency needed for built-in/common search.

- `:cli:packer`
  - Developer/publishing tool only.
  - Owns pack generation and Lucene index generation.
  - Depends on all analyzer libraries needed to create packs.
  - Should not leak packer helpers into `:cli:core`.

- `:cli:search`
  - Search helper executable(s).
  - Current `bbl-kmp` has six modules: `common`, `extra`, `kuromoji`, `morfologik`, `nori`, `smartcn`.
  - Current decision: keep split helper modules by lucene-kmp analyzer grouping to keep native binaries smaller.

- `:test-framework`
  - Shared test helpers only.
  - Depends on `:core`, not app/server/CLI modules unless a specific helper truly needs one.

Why:

- The old `composeApp` did multiple jobs: shared UI, Android library config, desktop app config, iOS framework output, resources, fonts, and embedded Bible packs.
- The new structure makes responsibility obvious. A file can usually be placed by answering: domain, shared UI, app entry, server, CLI, packer, or test helper.

Trade-off for Joel:

- `:cli:search` can be one simple module or remain several small dependency-specific helper modules.
- One module is easier to understand and removes most duplicate code.
- Several modules keep native helper binaries smaller and avoid linking analyzer libraries that a helper does not use.
> Joel's chose: split them by lucene-kmp:analysis:[module] grouping to keep binary size small. Becauase a user never use many search modules which is not for their language.

### [x] 2. Keep the new Gradle shape, but copy only needed build logic

Change:

- Start from the new `bbl` root scripts:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle.properties`
  - `gradle/libs.versions.toml`
  - Gradle wrapper files under `gradle/`

- Copy dependency versions and plugins from `bbl-kmp` only when the migrated source needs them:
  - `clikt`
  - `okio`
  - `kmp-io`
  - `kotlinx.serialization`
  - `kotlinx.coroutines`
  - `ktor-client-*`
  - `kotlin-logging`
  - `multiplatform-settings`
  - `multiplatform-locale`
  - `lucene-kmp-*`
  - test libraries used by the migrated tests

- Current `bbl` decision: use published Maven `org.gnit.lucene-kmp:*` artifacts.
  - `settings.gradle.kts` currently has no `../lucene-kmp` composite substitution.
  - Re-add a local composite only if Joel decides this new scaffold should dogfood sibling `lucene-kmp` directly.

- Move the large old root `build.gradle.kts` custom task block out of the root build.
  - Root build should mainly load plugins and shared defaults.
  - Put Kitchen fixture staging in `gradle/bbl-install.gradle.kts` 
  - Put pack-generation tasks in `:cli:packer`.

- Use the default KMP hierarchy where possible.
  - Keep custom `posixMain` only where real POSIX code exists.
  - Keep `mingwX64Main` only where Windows-native code differs.
  - Do not manually create `nativeMain`, `nativeTest`, `iosMain`, or `iosTest` unless the source tree or dependency needs it.

Why:

- The old build has working knowledge but too much global configuration.
- The new build should be boring: standard modules, standard target declarations, targeted custom code only where the platform split is real.

### [x] 3. Make Bible text and pack resources single-source

Observed state in `bbl-kmp`:

- `server/src/main/resources/files/bbltexts` has the raw server text source with 32319 files.
- `server/src/main/resources/files/bblpacks` has 27 generated downloadable zip packs.
- `composeApp/src/commonMain/composeResources/files/bblpacks` has 17956 embedded unpacked pack files.
- `composeApp/src/androidDeviceTest/assets/bblpacks` has 12 zip test assets.
- `shared/src/commonTest/resources` has small fixtures.
- `server/build.gradle.kts` currently points server resources at `composeApp/src/commonMain/composeResources`, which makes resource ownership unclear.

Change:

- Create one canonical resource input area outside app/server modules:
  - `resources/bbltexts/<translation>/...` for raw translation text and manifests.
  - `resources/bblpacks/<translation>.zip` for committed generated downloadable packs if we keep generated packs in git.
  - `resources/embedded-bblpacks/<translation>/...` for embedded app packs if unpacked resources must stay committed.

- Generate or sync module-specific resources from the canonical area:
  - `:server` receives `src/main/resources/files/bblpacks`.
  - `:app:shared` receives Compose resources under `src/commonMain/composeResources/files/bblpacks`.
  - Android device tests receive `src/androidDeviceTest/assets/bblpacks`.
  - Kitchen install fixtures receive `bbl_install/files`.

- Make `:cli:packer` read canonical raw text and write canonical generated packs.
- Stop wiring `:server` resources directly from app resources.

Why:

- Server resources and app resources are outputs for different consumers.
- Neither server nor UI should be the hidden source of truth for Bible text.
- This makes stale-pack problems easier to reason about: raw text -> packer -> generated packs -> consuming module.

Trade-off for Joel:

- Keeping generated packs in git makes app/server testing easier and offline.
- Generating packs during builds is cleaner but slower and increases Gradle complexity.
- Recommended first migration: keep generated packs committed, but make the canonical input/output locations explicit and add verification tasks.
> Joel's choice: keep generated packs committed for now, but single source of truth should be as you said at resouces/bbltexts and resources/bblpacks. embedded-bblpacks is subset of bblpacks, so I would like to avoid duplicating.

### [x] 4. Preserve the good domain code, group scattered helpers by topic

Keep mostly unchanged:

- `Books.kt`
- `BibleFilter.kt`
- `BookChapterVerse`
- `BibleFilter` variants
- existing filter tests

Change top-level helper groups into named objects when the functions are not idiomatic Kotlin extension APIs:

- `ParseBook.kt`
  - Current: `filterByBookChapter`, `bookNumber`, `bookNameEnglish`, `bookNameEnglishCapital`, `bookNameFor`, `formatHeader`, top-level arrays/maps.
  - Proposed: `object BookNames` or `object BibleReferences`.
  - Keep extension-style helpers only if they read naturally at call sites.
  > Joel's chosce: move them into Books.kt and unit tests into BooksTest.kt
  > Current state: moved into `Books.kt` with `BooksTest`, but temporary top-level compatibility wrappers still remain.

- `CategorySearch.kt`
  - Current: top-level `resolveCategoryFilterOrThrow`, `resolveCategoryFiltersOrThrow`.
  - Proposed: move into `Books.Category` companion or `object BibleCategories`.
  > Joel's choice: move them into Books.kt and unit tests into BooksTest.kt
  > Current state: moved into `Books.kt`, but temporary top-level compatibility wrappers still remain.

- `BblVersion.kt`
  - Current: constants plus top-level functions.
  - Proposed: `object BblVersion` with `version`, `searchHelperVersionLine(...)`, and manifest helpers.
  > Current state: `object BblVersion` exists, but temporary top-level compatibility wrappers still remain.

- `LoggingConfig.kt`
  - Current: top-level `suppressKotlinLoggingStartupMessage`.
  - Proposed: `object LoggingSetup`.
  > Current state: `object LoggingSetup` exists, but temporary top-level compatibility wrapper still remains.

- `SearchEngine.kt`
  - Current: `searchTermFromArgs` near search engine internals.
  - Proposed: move CLI/query-string helpers to `object SearchQueryText` in CLI or core, depending on reuse.
  > Current state: `SearchQueryText` exists in core, but the temporary top-level `searchTermFromArgs` wrapper still remains.

Why:

- The goal is not to make everything object-oriented.
- The goal is to make "where should I look?" easier for a human.
- Small extension functions are fine. Unrelated top-level function piles are not.

### [x] 5. Simplify search helper duplication

Observed state:

- `SearchCommon.kt`, `SearchExtra.kt`, `SearchMorfologik.kt`, `SearchSmartcn.kt`, `SearchNori.kt`, and `SearchKuromoji.kt` repeat almost the same Clikt options and result encoding.
- Only binary name and analyzer provider differ.

Change:

- Accepted current decision: do not introduce one reusable search helper command now.
  - The duplicated helper command classes are easier for Joel to understand.
  - Preserve split module and binary behavior.
  - Revisit only if option/result handling starts drifting across helpers.

- Deferred option if duplication becomes painful later:
  - `SearchHelperCommand(binaryName, analyzerProviderFactory)`
  - Handles version flags, artifact compatibility flag, translation lookup, book/chapter validation, category validation, search execution, and JSON output.

- Keep analyzer providers as small focused classes:
  - `CommonAnalyzerProvider`
  - `ExtraAnalyzerProvider`
  - `MorfologikAnalyzerProvider`
  - `SmartcnAnalyzerProvider`
  - `NoriAnalyzerProvider`
  - `KuromojiAnalyzerProvider`

- If using one `:cli:search` module, entry points become thin wrappers:
  - `mainCommon(args)`
  - `mainExtra(args)`
  - or one `main(args)` with `--provider` only if we are willing to change installed binary behavior.

Why:

- The current duplication makes every CLI option change six edits.
- A single command class reduces bug risk without hiding business rules.

Trade-off for Joel:

- Keeping binary names such as `bbl-search-common` preserves install behavior.
- A single `bbl-search` helper is easier, but changes install scripts and search delegation.
- Recommended first migration: preserve existing binary names and installed behavior, but share the implementation.
> Joel's choice: keep them split, duplicated helper clases can be kept because it is easiy for me to understand than introducing abstruction.

### [x] 6. Reduce analyzer provider duplication carefully

Observed state:

- `CommonAnalyzerProvider` exists in both shared/core and `cli/search/common`.
- `CmpAnalyzerProvider` and `PackerAnalyzerProvider` each know about all analyzers.
- Scope filters for Russian, Swedish, and Ukrainian are duplicated across providers.

Change:

- Accepted current decision: keep `CmpAnalyzerProvider` and `PackerAnalyzerProvider` separate.
  - This preserves dependency and binary-size boundaries.
  - Analyzer coverage should be enforced by tests instead of by a shared heavy catalog.

- Deferred option if provider drift becomes painful later: define analyzer metadata in one place:
  - language code
  - search module id
  - factory function
  - optional query-time Bible filter rule

- Keep dependency boundaries intact:
  - `:core` cannot instantiate analyzers from every lucene analysis module if that would force all clients to depend on every analyzer.
  - Full analyzer catalogs belong in `:app:shared`, `:cli:packer`, or `:cli:search`, where the dependencies are intentional.

- Move shared rules that do not require analyzer construction into `:core`.
  - Example: language-specific New Testament filter requirements can be represented as domain rules if they do not require heavy analyzer imports.

Why:

- Analyzer creation is one of the main places where search correctness and binary size interact.
- This should be explicit, not copied by hand in three places.
> Joel's choice: we can ensure analyzer coverage by unit tests. Including multiple lucene-kmp:analysis:xxx module would explode the binary size of cli:search module, so keeping them in `CmpAnalyzerProvider` and `PackerAnalyzerProvider` is okay.

### [ ] 7. Split the large Compose app file by responsibility

Observed state:

- Old `composeApp/src/commonMain/kotlin/org/gnit/bible/App.kt` was 1127 lines.
- Current `app/shared/src/commonMain/kotlin/org/gnit/bible/app/App.kt` is now a thin environment/entry wrapper.
- Current `BibleScreen.kt` owns the main app screen/chrome, and multiple reused pieces already live under `ui/widgets`.

Change:

- [x] Move app state to:
  - `app/shared/src/commonMain/kotlin/org/gnit/bible/app/state/BibleAppState.kt`
  - `ReadingMode`
  - `BibleState`
  - saver/serialization helpers

- [x] Move app services to:
  - `app/shared/src/commonMain/kotlin/org/gnit/bible/app/services/BibleAppEnvironment.kt`
  - Own `Bible`, `AssetManager`, `Settings`, platform context, and resource reader.
  - Remove global mutable `am` and `bible`.

- [x] Move UI screens/components to smaller files:
  - [x] `app/shared/src/commonMain/kotlin/org/gnit/bible/app/screens/BibleScreen.kt`
  - [x] existing `ui/widgets/*` where appropriate
  - [x] `screens/ReadingScreen.kt`
  - [x] `screens/TranslationManagerScreen.kt`
    - Current state: intentionally still lives in `ui/widgets/TranslationManager.kt` because it is a reusable app widget, not a Bible screen shell.
  - [x] `components/TopBar.kt`
  - [x] `components/BookControls.kt`
  - [x] `components/ChapterControls.kt`

- Keep top-level `@Composable` functions.
  - Compose convention favors top-level composables.
  - Do not force composables into objects unless there is a real namespace problem.

Why:

- The problem is not top-level Compose functions.
- The problem is too many responsibilities in one file and app-global mutable state.

### [x] 8. Clarify CLI, packer, and domain boundaries

Observed state:

- `IndexBuilder.kt` and `PackCli.kt` in `:cli:core` are placeholder files saying they moved to `:cli:packer`.
- `PathExt.kt` lives in `:cli:shared` and is reused by both CLI modules.
  - `PackCli` now honors the convention-over-configuration project-root paths and supports overrides.
Current state:
- Placeholder moved files are gone from `:cli:core`.
- Shared path helpers are centralized in `:cli:shared`.
- `PackCli` now honors `--source`, `--packs`, and `--all`, and the Clikt command delegates through `run()`.

Change:

- Delete placeholder moved files from `:cli:core`.
- Put shared path/process helpers in a small internal CLI support package if both CLI modules need them:
  - either `:cli:support` if the module count is acceptable,
  - or duplicate only tiny `expect fun currentDir()` if avoiding a support module is more important.

- Make `:cli:packer` path inputs explicit Gradle properties or command options:
  - `--source resources/bbltexts`
  - `--packs resources/bblpacks`
  - `--embedded resources/embedded-bblpacks`

Why:

- Placeholder files preserve history but add noise in a new clean project.
- Hard-coded `../../server/...` and `../../composeApp/...` paths encode the old layout and should not survive the migration.

Trade-off for Joel:

- A `:cli:support` module reduces duplication but adds a module.
- Re-declaring tiny platform helpers is less abstract but slightly duplicated.
- Recommended first migration: no new support module unless duplication grows beyond `currentDir`/process helpers.
> Joel's choice: I created cli:shared module, so use it for shared code such as currentDir and small code needed by multiple or all modules

### [x] 9. Keep defensive checks only at boundaries

Change:

- Keep validation in public input boundaries:
  - CLI arguments
  - packer input/output paths
  - downloaded pack manifests
  - zip entry names read from external files
  - server route params

- Remove or simplify defensive checks inside already-validated internal flows.

Why:

- Too much internal validation makes simple flows hard to read.
- Boundary validation is still important because pack files, downloads, and CLI input are untrusted.

### [x] 10. Delete generated boilerplate early

Change:

- Remove generated demo code from `bbl`:
  - `Greeting.kt`
  - `GreetingUtil.kt`
  - sample shared tests
  - sample app button/image content
  - placeholder `README.md` under source folders if not useful

- Keep new project scaffolding that is real infrastructure:
  - wrapper
  - app module directories
  - Xcode project
  - Android manifest and launcher resources until replaced

Why:

- The new project should not start with unrelated demo concepts mixed with real Bible code.

## Steps of migration

### [x] Step 1. Stabilize the target scaffold

1. [x] Keep `bbl` as a separate git repo/project.
2. [x] Delete only generated demo files that have no migration value.
3. [x] Keep `Books.kt` and `BibleFilter.kt` in `:core` unchanged.
4. [x] Decide and apply package naming:
   [x] - Shared code among all projects: `org.gnit.bible`.
   [x] - App-only code: use `org.gnit.bible.app`
   [x] - CLI-only code: use `org.gnit.bible.cli`
   [x] - Server-only code: use `org.gnit.bible.server`
   - Current state: server source and Gradle `mainClass` now use `org.gnit.bible.server`.

### [x] Step 2. Migrate root Gradle files

1. [x] Start from new `bbl/settings.gradle.kts`.
2. [x] Add includes for all target modules:
   [x] - `:core`
   [x] - `:server`
   [x] - `:test-framework`
   [x] - `:cli:core`
   [x] - `:cli:search`
   [x] - `:cli:packer`
   [x] - `:cli:shared`
   [x] - `:app:shared`
   [x] - `:app:androidApp`
   [x] - `:app:desktopApp`
   [x] - `app/iosApp` stays an Xcode project directory, not a Gradle included project
   [x] - `:cli:search:common`
   [x] - `:cli:search:extra`
   [x] - `:cli:search:kuromoji`
   [x] - `:cli:search:morfologik`
   [x] - `:cli:search:nori`
   [x] - `:cli:search:smartcn`
3. [x] Use published `lucene-kmp` Maven dependencies by current decision; now optional sibling `../lucene-kmp` composite is configured and switchable via `useLocalLuceneKmp` in `gradle.properties`.
4. [x] Merge version catalog entries from `bbl-kmp`, but only for dependencies used by migrated modules. Update `lucene-kmp` to `10.2.0-alpha13` to match local development version.
5. [x] Keep root `build.gradle.kts` small.
6. [x] Move old Kitchen fixture staging into `gradle/bbl-install.gradle.kts` after CLI binaries compile.
7. [x] Update Gradle wrapper to `9.3.1` to support local composite build with `lucene-kmp`.

### [x] Step 3. Migrate `:core`

1. [x] Copy `bbl-kmp/shared/src/commonMain/kotlin/org/gnit/bible` to `bbl/core/src/commonMain/kotlin/org/gnit/bible`.
2. [x] Preserve existing `Books.kt` and `BibleFilter.kt`.
3. [x] Remove generated `org/gnit/bible/bbl/GreetingUtil.kt`.
4. [x] Add only the dependencies required by copied core code.
5. [x] Keep platform source sets only where files exist:
   [x] - `androidMain`
   [x] - `iosMain`
   [x] - `jvmMain`
   [x] - `posixMain`
   [x] - `mingwX64Main`
6. [x] Compile `:core` before refactoring.
7. [x] Refactor top-level helpers after tests pass in copied form.
   - [x] `ParseBook.kt`: move into `Books.kt` and `BooksTest.kt`. (DONE: moved, wrappers removed)
   - [x] `CategorySearch.kt`: move into `Books.kt` and `BooksTest.kt`. (DONE: moved, wrappers removed)
   - [x] `BblVersion.kt`: move into `object BblVersion`. (DONE: moved, wrappers removed)
   - [x] `LoggingConfig.kt`: move into `object LoggingSetup`. (DONE: moved, wrappers removed)
   - [x] `SearchEngine.kt`: move `searchTermFromArgs` to `SearchQueryText`. (DONE: moved, wrappers removed)

### [x] Step 4. Migrate `:test-framework`

1. [x] Copy `bbl-kmp/test-framework/src` to `bbl/test-framework/src`.
2. [x] Replace dependencies on `projects.shared` with `projects.core`.
3. [x] Remove app/server assumptions from shared test helpers.
   - Current state: shared helpers now use canonical `resources/bblpacks` and no longer reference old server/app resource paths.
4. [x] Compile `:test-framework`.

### [x] Step 5. Migrate core tests

1. [x] Copy `bbl-kmp/shared/src/commonTest` to `bbl/core/src/commonTest`.
2. [x] Copy small test fixtures from `bbl-kmp/shared/src/commonTest/resources`.
3. [x] Keep tests green before broader refactoring.
4. [x] Use the old search behavior tests as guardrails; do not weaken assertions.

### [x] Step 6. Establish canonical resources

1. [x] Create canonical shared resource directories under `bbl/resources/bblpacks` and `bbl/resources/bbltexts`.
2. [x] Move/copy raw texts from `bbl-kmp/server/src/main/resources/files/bbltexts`.
3. [x] Move/copy generated server packs from `bbl-kmp/server/src/main/resources/files/bblpacks`.
4. [x] Keep app-specific assets and module-specific test resources owned by each module instead of centralizing them here.
5. [x] Add Gradle sync tasks only where the shared pack/text buckets need to be consumed.
6. [x] Add verification that pack manifests match `BblVersion.VERSION`.

### [x] Step 7. Migrate `:server`

1. [x] Replace generated sample Ktor app with `bbl-kmp/server/src/main/kotlin`.
2. [x] Update package/main class if server package naming remains `org.gnit.bible.server`.
   - Current state: package/main class now use `org.gnit.bible.server`.
3. [x] Depend on `projects.core`.
4. [x] Remove resource wiring from old `composeApp`.
5. [x] Serve synced/generated resources from the server module.
6. [x] Copy server tests and update imports.

### [x] Step 8. Migrate `:cli:packer`

1. [x] Copy `bbl-kmp/cli/packer/src`.
   [x] - Copy commonMain source sets
   [x] - Copy commonTest source sets, and if test fail, adjust to make all tests pass before refactoring.
2. [x] Update dependencies from `projects.shared` to `projects.core`.
3. [x] Replace hard-coded old resource paths with command options or Gradle properties.
   - Current state: `--source` and `--packs` are honored, and `--all` uses the same roots.
4. [x] Keep pack generation behavior identical first.
5. [ ] After tests pass, simplify `PackCli` and `IndexBuilder` boundaries.

### [x] Step 9. Migrate `:cli:search`

1. [x] Recreate submodules under `:cli:search:*`
2. [x] Fill proper build.gradle.kts file contents for each module.
3. [x] First implementation should preserve installed binary behavior unless Joel chooses otherwise.
4. [x] Do not extract repeated CLI parsing into `SearchHelperCommand` for now; Joel chose explicit duplicated helper classes for readability.
5. [x] Keep provider-specific code in provider-specific modules.
6. [x] Verify helper compilation through Linux native compile tasks and broad `jvmTest`; dedicated search-helper tests are not currently present.
7. [x] Remove mordant dependency which is implicitly contained in clikt to keep kotlin/native compiler cache working 
       ref1: https://ajalt.github.io/clikt/advanced/#core-module
       ref2: https://chatgpt.com/share/6a210394-72c0-83a9-92b5-d53617d6d1ce
   [x] - replace maven dependency declaration in lib.versions.toml from `module = "com.github.ajalt.clikt:clikt"` to `module = "com.github.ajalt.clikt:clikt-core"`
   [x] - replace CliktCommand with CoreCliktCommand in all cli:search helper commands
   [x] - implement extension function `fun CoreCliktCommand.test()` so migrated test cases still work. Requirement is string input/output verification only. Mimic `fun CliktCommand.test()` partially and ignore env vars, width, height, and other console wiring. `CliktConsole` is removed, and we are not keeping the Mordant terminal dependency, so this helper needs a simpler workaround that does not use Mordant.
       - Current state: implemented in `:cli:shared`; `:cli:core:compileTestKotlinJvm` and the other CLI test-compile targets pass after removing the stale `CliktConsole`-based duplicate shim from `:cli:core`.
8. [x] Migrate `:cli:search:*` src/commonTest source sets.
9. [x] Restore biblical analyzers (e.g., `BibleEnglishAnalyzer`) and `bibleFiltersFor` logic to ensure search correctness and test passage.

### [x] Step 10. Migrate `:cli:core`

1. [x] Copy `bbl-kmp/cli/core/src`.
2. [x] Delete placeholder moved files for `IndexBuilder.kt` and `PackCli.kt`.
3. [x] Update dependencies from `projects.shared` to `projects.core`.
4. [x] Update search helper binary paths/names only after `:cli:search` is decided.
5. [x] Restore native target declarations and `posixMain`/`mingwX64Main` only where source files require them.
6. [x] Run focused CLI tests, especially `CliBibleTest.searchJesusChristInWebus()`.
   - New `bbl` does not currently have that exact method name; verified migrated CLI/search coverage with `:cli:core:jvmTest` and broad `jvmTest`.

### [x] Step 11. Migrate `:app:shared`

1. [x] Copy old `composeApp/src/commonMain/kotlin` into `bbl/app/shared/src/commonMain/kotlin`.
2. [x] Remove generated demo app code.
3. [x] Copy Compose resources:
   [x] - drawables
   [x] - fonts
   [x] - app embedded pack resources from canonical sync output
4. [x] Add all Compose and lucene analyzer dependencies needed by shared UI.
5. [x] Copy old `composeApp/src/commonTest/kotlin` into `bbl/app/shared/src/commonTest/kotlin`, if fail, adjust to make all tests pass before refactoring.
   - Current state: copied `ComposeBibleTest`, `ComposeBibleResourcesReaderTest`, and `ComposeSearchTest` compile after wiring `:app:shared` test dependencies to `:test-framework`, `ktor-client-mock`, and `okio-fakefs`.
6. [x] Split old `App.kt` into state, environment, screens, and components.
   - Current state: `BibleScreen.kt` is now a thin shell around `BibleApp`, and the shared app chrome/helpers live in `screens/ReadingScreen.kt`, `screens/TopBar.kt`, `screens/BookControls.kt`, and `screens/ChapterControls.kt`. `TranslationManagerScreen` remains in `ui/widgets` as a reusable widget.
7. [x] Remove global mutable app services.
8. [x] Keep top-level composables where that matches Compose convention.

### [ ] Step 12. Migrate app entry modules

1. Android:
   [x] - Copy useful pieces from old `androidApp`.
   [x] - Keep new AGP 9 separate app module.
   [x] - Depend on `:app:shared`.
   [x] - Keep generated Compose asset sync only if the new Compose resources pipeline still needs it.
   - Current state: `syncAndroidDeviceTestPacks` copies from canonical `resources/bblpacks`.

2. iOS:
   [x] - Compare old `iosApp` with new Xcode project.
   [ ] - Keep the new project if it builds cleanly.
   [x] - Update framework name and Swift imports to match `:app:shared`.
   - Current state: Swift imports `Shared` and calls `MainViewControllerKt.MainViewController()`, but full Xcode/macOS build is unverified in this Linux environment.

3. Desktop:
   [x] - Use new `:app:desktopApp`.
   [x] - Move desktop main class and packaging from old `composeApp`.
   [x] - Keep desktop-specific dependencies here, not in shared UI.

### [x] Step 13. Restore install and packaging workflows

1. [x] Port Kitchen/bbl_install files if this repo will keep local installer tests.
2. [x] Recreate staging tasks using new module paths.
3. [x] Keep task names compatible where useful:
   [x] - `stageBblInstallLinuxCliAllFixture`
   [x] - `stageBblInstallWindowsCliAllFixture`
   [x] - `stageBblInstallMacosArm64CliAllFixture`
   [x] - `stageBblInstallMacosX64CliAllFixture`
4. [x] Verify staged files come from the new `bbl` build, not stale `bbl-kmp` output.

### [x] Step 14. Refactor only with tests in place

1. [x] First compile copied code with minimal behavior changes.
2. [ ] Then refactor one theme at a time:
   [x] - top-level helper grouping
   [x] - search helper command deduplication decision: intentionally not extracting now
   [x] - analyzer catalog/rules decision: intentionally keep provider duplication now
   [x] - resource ownership
   [x] - app state/environment split
3. [x] Run the smallest related tests after each change.
4. [x] Only then run broader module tests.

### [ ] Step 15. migrate bbl_install integration tests basd on test kitchen and inspec
1. [x] Copy `bbl-kmp/bbl_install` to `bbl/bbl_install`.
2. [x] Run and confirm test fixture preparation gradle tasks work
3. [ ] Run `bundle exec kitchen verify 26` to iterate over till all tests pass.

### [ ] Step 16. Final cleanup before replacing `bbl-kmp`

1. [ ] Remove unused generated boilerplate.
2. [ ] Remove old path names from code and Gradle scripts:
   [x] - `composeApp`
   [x] - `shared`
   [x] - hard-coded `../../server`
   - Current state: download URLs now point at `bbl/resources`, shared tests and packer tests use canonical repo-root resource paths, and the old `bbl_kmp_*` temp-dir names were removed from migrated tests.
3. [ ] Search for duplicated source files that should not exist in the new design.
4. [ ] Verify app/server/CLI all read from the intended resource outputs.
5. [ ] Verify search behavior with existing high-value tests.
6. [ ] Update README and developer workflow docs.

## Decisions needed from Joel

Answered:

1. Should `:cli:search` become one simple module even if helper binaries become larger, or should it keep the old split-by-analyzer module shape?
> Joel's choice: keep them split, duplicated helper clases can be kept because it is easiy for me to understand than introducing abstruction.

2. Should generated `bblpacks` stay committed for offline/reproducible app/server tests, or should they be generated during migration/build tasks?
> Joel's choice: keep generated packs committed

3. Should migrated app-only Kotlin packages remain `org.gnit.bible` for low churn, or move to `org.gnit.bible.app` for clearer boundaries?
> Joel's choice:
shared code will go into `org.gnit.bible` package.
cli specific code will go into `org.gnit.bible.cli` package.
app specific code will go into `org.gnit.bible.app` package.
server specific code will go into `org.gnit.bible.server` package.

I accidentally generated `org.gnit.bible.bbl` packge via kmp new project wizard but it was not intentional, so delete them in all modules.

4. `settings.gradle.kts` currently uses published Maven `lucene-kmp` dependencies and no sibling composite build. Confirm the final shape should stay switchable between published and local composite, with the local composite commented out when not needed.
> Joel's choice: it needs to be switchable between published and local composite for development. It is beause bbl is dogfooding lucene-kmnp. as we add specific searcht test assertions and get unexpected or poor search result, we will improve language specific analyzers in lucene-kmp. however when we develop none-search features, we do not want to build all lucene-kmp modules. so the config condition can be commented out just like bbl-kmp settings.gradle.kts does.

5. The remaining `app/shared` UI split is still incomplete. Should we break `BibleScreen.kt` into smaller files for reading, translation manager, top bar, book controls, and chapter controls now, or keep the current grouping and treat `BibleScreen.kt` as the final coarse-grained boundary?

6. iOS Swift imports are updated to `Shared`, but the full Xcode/framework build is still unverified on Linux. Mark complete only after a macOS/Xcode verification pass.
> Joel's choice: YES, I will commit and push to GitHub from this local linux dev box, then pull it in my macOS dev box and will verify the Xcode build and run the iOS simulator.
