# TODO: Category Search in bbl-kmp CLI

## Goal

Adopt the shared `Bible.search(..., filter/filters = BibleFilter)` support in:

- `bbl-kmp:cli`
- `bbl-kmp:cli:search:common`
- `bbl-kmp:cli:search:extra`
- `bbl-kmp:cli:search:kuromoji`
- `bbl-kmp:cli:search:morfologik`
- `bbl-kmp:cli:search:nori`
- `bbl-kmp:cli:search:smartcn`

The user-facing target is commands such as:

```bash
bbl search king in david
bbl search gospel in paul
bbl search Jesus Christ --category johns-letters
```

## Design Decision

Use the existing shared `BibleFilter` and `Books.Category` model end-to-end.

Do not add another CLI-specific category enum and do not serialize raw `BibleFilter`
shapes across process boundaries. Keep the helper command protocol simple by passing
category keys, then resolve those keys inside each helper binary.

### Parsing precedence

Inline `in <scope>` should be resolved in this order:

1. Translation suffix, as today: `bbl search Jesus in kjv`
2. Book/chapter location, as today: `bbl search Jesus in john 3`
3. Category key: `bbl search king in david`
4. Translation and book/chapter may coexist only as separate `in` clauses: `bbl search Jesus in john 3 in kjv`
5. Category key with translation may coexist only as separate `in` clauses: `bbl search king in david in kjv`

This preserves the existing book-name contract. For example, `in john` remains the
book of John because `ParseBook.kt` already owns canonical book aliases. Categories
should represent scopes that are not already a single book or that need explicit
semantic grouping, such as `paul`, `johns letters`, `johannine`, or `david`.

Literal search text must still be possible. For example, `bbl search in christ`
should search the two-word term `in christ`, not be parsed as a scope clause.
That means `in` is treated as scope syntax only when it appears in a recognized
scope position and the preceding search text is non-empty. Quoted text should
remain the explicit escape hatch for fully literal phrasing.
The compact mixed form `bbl search Jesus in kjv john 3` is intentionally rejected.

### Request shape

Extend `SearchRequest` with:

```kotlin
val filters: List<BibleFilter> = emptyList()
val categoryKeys: List<String> = emptyList()
```

`filters` is the in-process representation used by `InternalSearchBackend`.
`categoryKeys` is the process-safe representation used by `ExternalSearchBackend`
when it builds helper command arguments.

The main CLI resolves category keys to filters immediately for internal execution,
but keeps the original category keys so external helper binaries can resolve them
inside their own process without depending on serialized filter internals.

Inline parsing should collect all recognized scopes before building the request:

```kotlin
private data class InlineSearchFilters(
    val term: String,
    val translationCode: String?,
    val bookNumber: Int?,
    val startChapter: Int?,
    val endChapter: Int?,
    val categoryKeys: List<String>,
    val filters: List<BibleFilter>
)
```

Only one translation and one book/chapter location should be accepted. Multiple
categories may be accepted and should be passed as multiple filters.

### Helper protocol

Add a repeatable option to every search helper:

```bash
--category <key>
```

Then external command building becomes:

```bash
bbl-search-common -t webus --category paul gospel
bbl-search-common -t kjv --book 43 --chapter 3 --category david king
```

For now, prefer only `--category` in code and tests. A shorter alias such as
`-c` can be added later if the CLI wording proves awkward.

### Search execution

All search backends should call:

```kotlin
bible.search(
    term = request.term,
    bookNumber = request.bookNumber,
    startChapter = request.startChapter,
    endChapter = request.endChapter,
    verses = request.verses,
    filters = request.filters,
    translation = request.translation
)
```

Book/chapter filters and category filters may coexist. That gives predictable
intersection semantics through shared `SearchEngine` Boolean clauses.

## Implementation Plan

- [x] 1. Add a small shared CLI resolver in `cli/core`
  - Create a helper such as `resolveCategoryFilter(key: String): BibleFilter`.
  - Use `Books.Category.fromKey(key)` and throw `UsageError` with a clear message when unknown.
  - Reuse this resolver from `SearchCli` and helper CLIs where possible.
  - Keep category keys normalized to the exact user-provided scope text after trimming and lowercasing.

- [x] 2. Extend `SearchRequest`
  - Add `filters: List<BibleFilter> = emptyList()`.
  - Add `categoryKeys: List<String> = emptyList()` for external helper command construction.
  - Update tests and call sites to use default values where no category is requested.

- [x] 3. Update main `SearchCli` option parsing
  - Add `--category`.
  - Replace the current single trailing-suffix parser with a small parser that can consume more than one `in <scope>` clause.
  - Parse each scope as translation, then book/chapter, then category.
  - Support repeated-scope forms:
    - `bbl search Jesus in john 3 in kjv`
    - `bbl search king in david in kjv`
  - Store both resolved `BibleFilter` values and raw category keys.
  - Reject conflicting duplicate translations or duplicate book/chapter locations with a `UsageError`.
  - Keep `--book` and inline book/chapter behavior unchanged.

- [x] 4. Update `InternalSearchBackend`
  - Pass `request.filters` to `bible.search`.

- [x] 5. Update `ExternalSearchBackend`
  - Add each `request.categoryKeys` value as `--category <key>`.
  - Do not attempt to serialize `BibleFilter` itself.

- [x] 6. Update each helper binary
  - Add repeatable `--category`.
  - Resolve category keys to `Books.Category.filter`.
  - Pass the resulting `filters` to `bible.search`.
  - Apply the same update in common, extra, kuromoji, morfologik, nori, and smartcn helpers.

- [x] 7. Add unit tests
  - `SearchCliTest`: `bbl search king in david` sends `Books.Category.DAVID.filter`.
  - `SearchCliTest`: `bbl search Jesus in john` still sends `bookNumber = 43`, not a category.
  - `SearchCliTest`: `bbl search Jesus in john 3 in kjv` combines translation and book/chapter when scopes are repeated.
  - `SearchCliTest`: `bbl search king in david in kjv` combines category and translation in the reverse order.
  - `SearchCliTest`: `bbl search Jesus Christ in johns letters` resolves a category key containing spaces.
  - `SearchCliTest`: `bbl search gospel --category paul` sends the Pauline category filter.
  - `SearchCliTest`: `bbl search gospel --category johns letters` sends a spaced category key as one option value.
  - `SearchCliTest`: `bbl search in christ` stays literal query text.
  - `SearchBackendTest`: external backend includes `--category paul` in the helper command.
  - `SearchBackendTest`: external backend includes `--category johns letters` as a single argument.
  - `SearchBackendTest`: external backend includes both `--book`/`--chapter` and `--category` when both are present.
  - `BooksTest`: category aliases must not overlap with `bookNameNumberArray` aliases.
  - Add one focused helper CLI test if an existing helper test harness supports command-level assertions.

- [x] 8. Verification
  - [x] Run focused CLI core tests first:
    `:cli:core:allTests` or the IDE run configuration for `SearchCliTest` and `SearchBackendTest`.
  - [x] Run focused helper compile checks for changed helper modules.
  - [x] Run broader CLI tests only after focused tests pass.

  Broader CLI JVM tests are currently blocked by unrelated fixture setup failures in the repo-wide test harness, not by the category-search changes themselves.

- [x] 9. Add E2E tests
  - [x] Add a few focused CLI tests that run the full process and assert on output.
  - [x] Mirror the relevant `SearchTestBase.kt` cases in `bbl_install/test/integration/default/default_spec.rb`.
  - [x] Run the full CLI test suite to verify no regressions.
  - [x] For now this computer is linuxX64, so run `./gradlew stageBblInstallLinuxCliAllFixture` then `bundle exec kitchen test ubuntu-26` only is enough.
  - [x] If there are any failing tests, debug and iterate over till you all pass.

- [x] 10. Bug Fix
    - [x] In E2E only, "bbl search Jesus" in English bible (webus, kjv) returns no results, while unit tests pass. Debug and fix the issue.

## Non-Goals

- Do not move category logic into `SearchEngine`.
- Do not add analyzer dependencies to unrelated helper binaries.
- Do not make `Books.Category` duplicate canonical single-book aliases.
- Do not introduce a custom serialized `BibleFilter` protocol unless category keys become insufficient.
