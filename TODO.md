# TODO: SEARCH_BINARY_SPLIT_PLAN (split search binaries)

> Goal: keep the main `bbl` Kotlin/Native CLI binary small by **not linking heavyweight lucene-kmp analyzer modules** (kuromoji/nori/smartcn/morfologik/extra dictionaries). Instead, delegate search (and optionally indexing/packing) to external helper binaries installed on demand.
>
> Constraints / notes:
> - Don’t add or modify any files under `lucene/` (Java lucene mirror).
> - Prefer Kotlin common code; use `expect/actual` only when necessary (e.g., process execution).
> - Keep public API names stable where possible; avoid churn.
>

## Phase 0 — Baseline + coupling inventory (no behavior change)

- [x] **DOC**: Identify and list every place analyzer selection leaks into `bbl` core.
  - Likely files (verify & update list as you go):
    - `shared/.../SearchEngine.kt`
    - `shared/.../Language.kt` (or wherever `analyzerFactory` lives)
    - `cli/.../IndexBuilder.kt`
    - `cli/.../PackCli.kt`
    - any `Translation`/`Language` initialization tables
  - Acceptance: short comment block (or `PROJECT.md` note) with file paths + why each dependency causes analyzer modules to be linked.
  - Commit: docs-only (or comment-only).


## Phase 1 — Introduce “analysis module id” in models (minimal refactor, testable)

- [x] Add a stable identifier representing which search/analyzer bundle a language needs.
  - Add `SearchModuleId` (enum or sealed interface) in `shared/commonMain`:
    - `COMMON`, `MORFOLOGIK`, `SMARTCN`, `NORI`, `KUROMOJI`, `EXTRA`
  - Add mapping `Language -> SearchModuleId` (prefer property on `Language` or a separate mapper in `shared`).
  - **Do not** reference lucene analyzers here; this must stay “data-only”.
- [x] Update `Language` / `Translation` metadata:
  - Replace `analyzerFactory: (() -> Analyzer)?` (or similar) with `searchModuleId: SearchModuleId`.
  - If a fallback existed (`?: SimpleAnalyzer()`), preserve it but implemented outside `shared`.
- [x] Tests (`shared/commonTest`):
  - Assert every `Language` has a non-null `searchModuleId`.
  - Assert known languages map correctly (at least 1 per module id).
  - Lock mapping down (so accidental changes break tests).
- [x] Ensure `:cli` still builds and existing tests pass.

Commit criteria:
- Behavior unchanged.
- Analyzer classes are no longer referenced from `shared` model objects.


## Phase 2 — Make `SearchEngine` accept analyzers via injection (preserve behavior)

- [x] Introduce `AnalyzerProvider` in `shared/commonMain`:
  - `fun analyzerFor(language: Language): Analyzer`
  - (Optional) allow query-time overrides if current API supports.
- [x] Refactor `SearchEngine` to take an `AnalyzerProvider` (constructor arg or param to `search`).
  - Remove any analyzer selection logic that pulls in lucene-kmp analyzer modules.
- [x] Add tests (`shared/commonTest`):
  - A fake provider is invoked for a given `Language`.
  - Fallback behavior remains correct (if applicable).

Commit criteria:
- No CLI behavior change.
- Search still works for at least one smoke test path.


## Phase 3 — Add core CLI delegation layer (internal vs external search)

- [x] Define a `SearchBackend` interface in `cli` (or `shared` if needed):
  - Internal backend: uses `SearchEngine` + **COMMON** analyzers only.
  - External backend: runs `bbl-search-<module>` executable.
- [x] Implement a delegating `SearchCommand`:
  - Determine `SearchModuleId` from `Translation.language.searchModuleId`.
  - If `COMMON` → internal search.
  - Else → external exec:
    - Primary path: `$HOME/.bbl/bin/bbl-search-<moduleId-lowercase>`
    - Optional: if not found, give actionable error (“run `bbl install <translation>`”).
- [x] Introduce process execution abstraction:
  - `expect/actual ProcessRunner` (common API): run command + args, capture exit code/stdout/stderr.
  - `actual` for `jvm` using `ProcessBuilder`.
  - `actual` for `native` using `posix_spawn`/`fork+exec` (choose based on existing project utilities).
- [x] Tests (`cli/commonTest`):
  - `COMMON` language uses internal backend.
  - Non-common language builds correct argv and tries external executable.
  - Error propagation does not lose stderr and includes module id.

Commit criteria:
- `bbl search ...` still works for `COMMON` languages.
- Non-common languages fail gracefully (until helpers exist).


## Phase 4 — Define helper binary protocol (contract + fixtures)

- [x] Write a minimal CLI contract for helper binaries (document in `cli/README.md` or new `cli-search/README.md`):
  - Required args: `-t <translationCode>` and query term(s).
  - Output format: match `bbl search` output semantics exactly.
  - Non-zero exit code on error; stderr contains human-readable message.
- [x] Create a tiny “test fixture pack” (small index + minimal bible text) usable by all helper modules.
  - Prefer storing as a small test resource in a shared test module.

Commit criteria:
- Protocol documented.
- Shared test fixture committed.


## Phase 5 — Create helper-binary modules (commit per module; add smoke tests)

> Create these as new K/N executables so they can be installed into `~/.bbl/bin/`.

- [x] Add module `:cli:search:common`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp kuromoji analyzer module.
  - Implements `main()` following the helper protocol.
  - Tests: smoke search using test fixture pack.
- [x] Add module `:cli:search:kuromoji`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp kuromoji analyzer module.
  - Implements `main()` following the helper protocol.
  - Tests: smoke search using test fixture pack.
- [x] Add module `:cli:search:nori`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp nori analyzer module.
  - Implements helper protocol.
  - Tests: smoke search.
- [x] Add module `:cli:search:smartcn`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp smartcn analyzer module.
  - Implements helper protocol.
  - Tests: smoke search.
- [x] Add module `:cli:search:morfologik`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp morfologik analyzer module.
  - Implements helper protocol.
  - Tests: smoke search.
- [x] Add module `:cli:search:extra`:
  - Module dir, empty package and noop class already added so fill them
  - Depends on `shared` + lucene-kmp analysis-extra module.
  - Implements helper protocol.
  - Tests: smoke search.

Commit criteria (each module):
- Module builds its K/N executable.
- Module has at least one passing test.

## Phase 5.5 — :cli module have submodules :core, :packer, :search
- [x] move cli/src to cli/core/src

## Phase 6 — Refactor packing/index building out of the main `bbl` binary

> End state: building `bbl` should not require linking heavyweight analyzers *even for pack/index creation*.

- [x] Create new dev-only module `cli-packer`.
  - Move `PackCli` and `IndexBuilder` into this module.
  - Allow `cli-packer` to depend on “all analyzers” (size doesn’t matter).
- [x] Remove `pack` subcommand from the end-user `bbl` binary (or gate it behind JVM-only tooling).
- [x] Update any references (docs/tests) to new entrypoint.
- [x] Tests:
  - `cli` tests verify `pack` is no longer available.
  - `cli-packer` tests cover pack/index creation (at least a smoke test).
- [x] Create new lucene-kmp index without stored verse texts

Commit criteria:
- `bbl` CLI module no longer depends on indexing/analyzer selection code.

## Phase 6.5 — Test all modules under :cli:search
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:common
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:morfologik
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:smartcn
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:nori
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:kuromoji
- [ ] Write, run, fix bugs and iterate over until pass Unit tests for :cli:search:extra

## Phase 7 — Installer changes: co-install pack + helper binary (functional, testable)

- [ ] Update translation install flow:
  - When installing translation pack `X`, compute `SearchModuleId`.
  - If not `COMMON`, install the corresponding helper binary if missing.
  - Ensure helper is executable (`chmod +x`) and stored at `~/.bbl/bin/bbl-search-<module>`.
- [ ] Add safety checks:
  - Verify `$HOME/.bbl/bin` exists / created.
  - Verify helper is runnable; print clear remediation if not.
- [ ] Tests (`cli/commonTest`):
  - Installing a `KUROMOJI` translation triggers helper installation.
  - Installing a `COMMON` translation does not.

Commit criteria:
- Installing a non-common translation makes subsequent `bbl search -t <code> ...` succeed (when helper present).


## Phase 8 — Policy changes for embedded translations + migration

- [ ] Decide and implement: move PL/UK/ZH/KO/JA translations from embedded → downloadable.
  - Add compat behavior:
    - If user requests an old embedded translation code, prompt to install.
    - Optional: auto-install on first use.
- [ ] Update docs (`README.md`) describing:
  - Which translations are embedded and why.
  - How helper binaries work.

Commit criteria:
- Backward incompatibilities documented and tested for friendly failure messages.


## Phase 9 — Size verification (manual check + optional automated guard)

- [ ] Document expected binary size deltas for `bbl` vs helper binaries.
- [ ] Optional: add a CI/gradle task to report executable size per target.


---

## Suggested commit sequence (one per bullet)

1. Phase 0 docs
2. Phase 1 SearchModuleId + mapping + tests
3. Phase 2 SearchEngine injection + tests
4. Phase 3 CLI delegation + ProcessRunner + tests
5. Phase 4 helper protocol + fixtures
6. Phase 5 helper modules (one PR/commit per module)
7. Phase 6 move packer to `cli-packer`
8. Phase 7 install co-install helpers
9. Phase 8 embedded → downloadable migration + docs
10. Phase 9 size reporting
