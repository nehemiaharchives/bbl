# Memory-only embedded Lucene index for `SearchEngine`

## Goal
Enable `SearchEngine` (in `shared/commonMain`) to search Lucene indexes **entirely in memory**, with **no disk/filesystem access**, including on Kotlin/Native (single executable).

## Key design decision
Do **not** use `FSDirectory` for this feature.

- In lucene-kmp, `FSDirectory` is explicitly a filesystem-backed `Directory` and it creates/opens real directories.
- lucene-kmp already provides an in-memory `Directory`: `org.gnit.lucenekmp.store.ByteBuffersDirectory`.

Therefore the simplest correct approach is:
- Read embedded index files as bytes
- Load them into a `ByteBuffersDirectory`
- Open `StandardDirectoryReader` on that in-memory directory

## Index packaging format
Package each translation’s Lucene index files as embedded resources (same principle as `BibleResourcesReader` for chapter text).

Recommended resource layout:

```
bblindexes/
  <translation>/
    index.manifest
    _0.cfe
    _0.cfs
    _0.si
    segments_1
    write.lock
    ...
```

Notes:
- Lucene `Directory` is flat (no subdirectories), so the manifest should list **filenames only**.
- `index.manifest` is required because cross-platform resource systems typically cannot reliably list directory contents.

## Abstractions in commonMain
Add a minimal index-bytes provider interface in `shared/commonMain`:

- `BibleResourcesReader` additional functions
  - `fun listIndexFiles(translation: Translation): List<String>`
  - `fun readIndexFile(translation: Translation, name: String): ByteArray`

`SearchEngine` must remain platform-agnostic and depend only on lucene-kmp `Directory` (or on `BibleResourcesReader` to build one).

## Build an in-memory Directory from embedded bytes
Implement a small builder in `shared/commonMain`:

- Create `ByteBuffersDirectory()`
- For each `name` from `BibleResourcesReader.listIndexFiles(translation)`:
  - `dir.createOutput(name, IOContext.DEFAULT).use { it.writeBytes(bytes, 0, bytes.size) }`
- Return the populated `Directory`

This keeps search fully in memory.

## Refactor `SearchEngine`
Option A (preferred):
- `class SearchEngine(private val directory: Directory)`

Option B:
- `class SearchEngine(private val reader: BibleResourcesReader, private val analyzerProvider: AnalyzerProvider)` and build/cache a `Directory` per translation.

Then:
- `StandardDirectoryReader.open(directory, leafSorter = null, commit = null)`
- Construct `IndexSearcher` and run queries.

## Platform implementations of `BibleResourcesReader` index reading functionality 
Implement `BibleResourcesReader` read index functions outside `shared`:

- CLI Kotlin/Native:
  - Reuse the existing TAR→C→.a + cinterop approach.
  - Add a parallel pipeline for `composeApp/src/commonMain/composeResources/files/bblpacks/$translationCode/index/$file`.
  - Read `index.manifest` and files from the embedded TAR.

- JVM (desktop + tests):
  - Read via `getResourceAsStream` from packaged resources.

- CMP (App Android/iOS/Desktop JVM):
  - Use the Compose Multiplatform’s `composeApp/src/commonMain/composeResources` access.

## Validation / acceptance criteria
- `SearchEngine` compiles in `shared/commonMain` with no TODOs.
- Search runs without touching the filesystem:
  - Uses `ByteBuffersDirectory`, not `FSDirectory`.
- JVM test loads an embedded index for one translation (e.g. `webus`) and asserts search returns at least one hit.
- CLI native can load the same embedded index and perform a search.

## Future optimization (optional)
This plan loads embedded index bytes into `ByteBuffersDirectory` (copies data into memory). If memory footprint becomes a problem, a phase-2 improvement is implementing a read-only `Directory` + `IndexInput` that reads directly from the embedded TAR pointer (zero-copy on Native).
