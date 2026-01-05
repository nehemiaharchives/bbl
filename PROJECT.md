# PROJECT.md

## Overview
**bbl-kmp** is the Kotlin Multiplatform successor to:

- [bbl-android](https://github.com/nehemiaharchives/bbl-android) — Android reader app  
- [bbl](https://github.com/nehemiaharchives/bbl) — CLI Bible with Lucene search

Goals:
- One shared core across Android, iOS, and Native CLI
- Unified search powered by [lucene-kmp](https://github.com/nehemiaharchives/lucene-kmp), a Kotlin Multiplatform port of Apache Lucene created specifically for this project
- **Embedded default translations** (fast start, offline) + **downloadable packs** for additional languages

> **Note:** lucene-kmp is a ground-up port of Apache Lucene to Kotlin Multiplatform (KMP). It re‑implements Lucene's indexing and search capabilities in pure Kotlin so that the same search engine runs natively on Android, iOS, Linux, macOS, and Windows without relying on the JVM. This port exists solely to power bbl-kmp and its CLI/mobile apps.

---

## Platforms

### MVP (now)
1. **Mobile (Compose Multiplatform)**  
   - Android (Kotlin/JVM)  
   - iOS (Kotlin/Native)
2. **Desktop CLI (single native binary)**  
   - Linux (Kotlin/Native)  
   - macOS (Kotlin/Native)  
   - Windows (Kotlin/Native)

### Future (not MVP)
- Desktop CLI (JVM fallback)
- Desktop GUI (Compose Multiplatform / JVM)
- Wasm browser app (Compose Multiplatform)

---

## Translations

### Embedded Packs (Tier A – included in app/CLI)
These ship inside the app/binary as resources for instant offline use:

| Code  | Name                                         | Language     |
|-------|----------------------------------------------|--------------|
| WEBUS | World English Bible                          | English      |
| KJV   | King James Version                           | English      |
| RVR09 | Reina-Valera (1909)                          | Spanish      |
| TB    | Tradução Brasileira (1917)                   | Portuguese   |
| DELUT | Lutherbibel (1912)                           | German       |
| LSG   | Louis Segond (1910)                          | French       |
| SINOD | Russian Synodal Bible                        | Russian      |
| SVRJ  | Statenvertaling (Jongbloed)                  | Dutch        |
| RDV24 | Versione Diodati Riveduta (1924)             | Italian      |
| UBG   | Uwspółcześniona Biblia gdańska (2017)        | Polish       |
| UBIO  | Ukrainian Bible (Ivan Ogienko, 1962)         | Ukrainian    |
| SVEN  | Svenska 1917                                 | Swedish      |
| CUNP  | Chinese Union Version (New Punctuation)      | Chinese      |
| KRV   | Korean Revised Version (1961)                | Korean       |
| JC    | Japanese Colloquial (1955)                   | Japanese     |

### Downloadable Packs (Tier B)
- Additional languages/regions (e.g., SE Asia, India, etc.) delivered as downloadable packs using the same format and loader.  
- Download UI will present size and any shared resource requirements (e.g., fonts already present).

---

## Data Format & Layout

Each pack (embedded or downloaded) uses the same structure:

```
packs/<id>@<version>/
  manifest.json
  text/...
  index/...
  dict/...   (optional; analyzer dictionaries)
  font/...   (optional; script fonts)
  LICENSE/
  NOTICE/
```

**Manifest schema**
```json
{
  "id": "webus",
  "version": "2025.09",
  "luceneKmpVersion": "0.1.0",
  "analyzerId": "standard-en",
  "dictVersion": "2025.05",
  "files": [
    { "path": "text/GEN/1.txt", "sha256": "...", "size": 12345 },
    { "path": "index/_0.cfe",   "sha256": "...", "size": 4567  }
  ],
  "totalSize": 38000000
}
```

**Where packs live**
- **Embedded**: `commonMain/resources/packs/<id>@<version>/...` (bundled into APK/IPA/binary)  
- **Downloaded**: app sandbox (`Android/iOS`) or `~/.bbl/packs/` (CLI)

**Lookup order** (transparent to the rest of the app):
1) user-installed dir → 2) embedded resources

---

## Search & Analyzers

- **All analyzer code is bundled** in the shared KMP module (easier builds/QA).  
- Heavy **dictionaries** (e.g., Kuromoji) are stored as resources and **loaded lazily** on first use.  
- Analyzer registry maps language/ID → implementation.  
- Index compatibility is enforced via `luceneKmpVersion` + `analyzerId` + optional `dictVersion`.

### Analyzer coupling inventory (Phase 0)
These file locations currently pull analyzer modules into the main `bbl` binary.

- `shared/src/commonMain/kotlin/org/gnit/bible/DefaultAnalyzerProvider.kt` maps language codes to concrete analyzers, which still pulls analyzer modules into `shared` when used.
- `shared/src/commonMain/kotlin/org/gnit/bible/SearchEngine.kt` takes an `AnalyzerProvider`, so the provider choice determines which analyzers are linked.
- `cli/src/commonMain/kotlin/org/gnit/bible/cli/IndexBuilder.kt` uses an `AnalyzerProvider` (defaults to `DefaultAnalyzerProvider`), so pack/index paths link analyzers too.
- `cli/src/commonMain/kotlin/org/gnit/bible/cli/PackCli.kt` depends on `IndexBuilder`, so analyzer linkage reaches the CLI binary through pack command.

---

## Fonts

- Latin script fonts bundled once.  
- CJK/other scripts included as resources within Tier-A packs that need them (Chinese/Korean/Japanese).  
- Runtime font registration supported on Android/iOS/macOS.  
- If size pressure emerges later, fonts can be moved to Tier-B packs without changing loaders.

---

## Pack Installer (for Tier-B downloads)

KMP module built on:
- **Ktor Client** (HTTP streaming)
- **Okio FileSystem** (disk I/O, SHA-256 via HashingSink/Source)
- **kotlinx-serialization** (manifest)
- **Coroutines/Flow** (progress)

**Install flow**
1. Fetch manifest  
2. Stream each file → tmp dir (validate hash/size)  
3. Atomic move to final dir on success  
4. Register pack; refresh catalog

**Safety rails**
- Path traversal & symlink rejection  
- Size caps (`totalSize`, per-file)  
- Cleanup stale tmp dirs on startup  
- Analyzer/version gates before enabling

---

## Packaging & Distribution

- **Android**: AAB with embedded Tier-A packs; other packs via in-app download to sandbox.  
- **iOS**: IPA with embedded Tier-A packs; Tier-B packs downloaded as data (no code).  
- **CLI**: Single native binary + embedded Tier-A packs; Tier-B packs to `~/.bbl/packs/`.

---

## CI / Workflow

- **PR CI (fast)**  
  - Common/JVM build + tests  
  - Android unit tests  
  - iOS compile-only  
  - Pack schema tests (manifest/hash/path rules)
- **Nightly CI (full)**  
  - Full KMP matrix, iOS simulator tests, coverage  
- **Branch protection**: require green CI + approval for merge  
- **Agent PRs**: allowed; same gate as humans

---

## Roadmap

**Phase 1 (MVP)**
- Shared core (lucene-kmp, analyzers, pack manager)
- Android app + Linux CLI with Tier-A embedded
- Download installer for one Tier-B language (pilot)

**Phase 2**
- iOS app
- macOS CLI
- More Tier-B translations

**Phase 3**
- Windows CLI (codesigned)
- Optional: JVM CLI fallback

**Future**
- Desktop GUI (Compose Multiplatform / JVM)
- Wasm prototype

---

## Repo Layout (reference)

```
bbl-kmp/
  PROJECT.md
  settings.gradle.kts
  gradle/
  buildSrc/
  shared/
    src/commonMain/kotlin/...      # core, search, analyzers, pack installer
    src/commonMain/resources/      # embedded packs (Tier A)
  app-android/
  app-ios/
  cli/
    src/linuxX64Main/...
    src/macosArm64Main/...
    src/mingwX64Main/...
  packs/                           # tools/tests for building packs
    tools/pack-build.kts
    tests/...
```

---

### Notes

- **lucene-kmp** is the backbone search library, ported from Apache Lucene to pure Kotlin for this project.  
- We accept the larger base image implied by the 14 embedded translations (historical precedent: ~100 MB installer).  
- If base size becomes a problem later, we can move individual packs (or just big fonts) to Tier-B without changing APIs.
- This document was generated in the result of [the discussion](https://chatgpt.com/share/68c5346f-6024-800b-9947-8ada67a96c1e) with the author and OpenAI Chat GPT-5
