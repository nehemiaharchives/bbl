---
name: port-kotlin-search-test-to-powershell
description: Port a translation's search test from NTGospelsPersonTest.kt into test_search.ps1 E2E tests.
---

# Port Kotlin Search Test to PowerShell E2E

Port a translation block from `NTGospelsPersonTest.kt` into the `test_search.ps1` CLI E2E test file.

## Step 1: Locate the Kotlin test

The source is at:
`test-framework/src/commonMain/kotlin/org/gnit/bible/test/search/person/NTGospelsPersonTest.kt`

Each translation has a `when` branch inside `searchJesusChrist()`:
```kotlin
when (supportedTranslation) {
    //...
    TB -> {
        val tb = supportedTranslation.translation
        listOf("Jesus Cristo", "Jesus", "Cristo").forEach { ptTerm ->
            // 5 search variants for each term:
            // 1. No filter → first() hit is Mateus 1:1
            // 2. bookNumber=romans → first() hit is Romanos 1:1
            // 3. bookNumber=romans, startChapter=2 → first() hit is Romanos 2:16
            // 4. bookNumber=romans, startChapter=3, endChapter=5 → first() hit is Romanos 3:22
            // 5. filter=Books.Category.filterOf("johns letters") → first() hit is 1 João 1:3
        }
    }
    //...
}
```

## Step 2: Map Kotlin API parameters to CLI args

| Kotlin API                                          | CLI equivalent                    |
|-----------------------------------------------------|-----------------------------------|
| `search(term = enTerm, translation = translation)`  | `@('search', $term, 'in', $code)` |
| `bookNumber = romans`                               | `'--book', 'romans'`              |
| `startChapter = 2`                                  | `'--chapter', '2'`                |
| `endChapter = 5`                                    | `'--end-chapter', '5'`            |
| `filter = Books.Category.filterOf("johns letters")` | `'in', 'johns letters'`           |

## Step 3: ⚠️ Critical — CLI phrase vs boolean search

**The biggest pitfall.** The CLI and the Kotlin API treat multi-word search terms differently:

- **CLI**: `@('search', 'Jesus Cristo', ...)` → exact phrase search. Only matches verses where "Jesus Cristo" appears as adjacent tokens in that exact order.
- **Kotlin API**: `search(term = "Jesus Cristo")` → boolean token search. Matches verses containing both "Jesus" AND "Cristo" as separate tokens anywhere in the verse.

**Consequences:**
- If the verse text has "Cristo Jesus" (reversed), the CLI phrase search **will not** find it, but the Kotlin API **will**.
- Example: Romanos 1:1 = "Paulo, servo de **Cristo Jesus**..." → CLI phrase "Jesus Cristo" → NOT matched.
- Example: Romanos 1:4 = "...Jesus Cristo, nosso Senhor" → CLI phrase "Jesus Cristo" → matched.

**Action:** When porting, run the actual CLI command first to find the real first hit. Do NOT blindly copy the Kotlin test's expected verse reference.

```powershell
# Check what the CLI actually returns as first hit:
& "path\to\bbl.exe" search "Jesus Cristo" "--book" "romans" "in" "tb"
```

## Step 4: Find verse text resources

Translation text files are at `resources/bbltexts/{code}/`, e.g. `resources/bbltexts/tb/`.

File naming: `{code}.{bookNumber}.{chapter}.txt` (e.g. `tb.40.1.txt` = Mateus chapter 1).

Read the manifest to confirm language and metadata:
```json
{"code":"tb","languageCode":"pt","englishName":"Brazilian Translation","nativeName":"Tradução Brasileira"}
```

Verses are numbered `{verseNum} {text}`. Strip the leading number when using as expected text.

**Key verse files to read:**
- Book 40, Chapter 1 (Mateus/Mateo/Matthew 1): `{code}.40.1.txt`
- Book 45, Chapter 1 (Romanos/Romans 1): `{code}.45.1.txt`
- Book 45, Chapter 2: `{code}.45.2.txt` (for `--chapter 2` tests)
- Book 45, Chapter 3: `{code}.45.3.txt` (for `--end-chapter 5` tests)
- Book 62, Chapter 1 (1 João/1 John): `{code}.62.1.txt`

## Step 5: Determine book names

Book names are defined per language in:
`core/src/commonMain/kotlin/org/gnit/bible/Language.kt`

Find the language code in the manifest, then look up `Language.{code}.bookNamesConcat`. The book names are `|`-separated; book 40 (Matthew) is at index 39 (0-based).

Alternatively, run a search to see the actual output format:
```
& "bbl.exe" search "Jesus" "in" "tb" "--max-results" "1"
```

## Step 6: Structure the test entries

Follow the exact pattern of existing translations. The order in `test_search.ps1` should match the order in `NTGospelsPersonTest.kt`:

```
WEBUS → KJV → RVR09 → TB → DELUT → LSG → SINOD → SVRJ → RDV24 → UBG → UBIO → SVEN → CUNP → KRV → JC → AYT → TH1971 → IRVHIN → IRVBEN → IRVTAM → NPIULB → ABTAG → KTTV → IRVGUJ → IRVMAR → IRVTEL → IRVURD
```

## Step 6a: Write the expected first line exactly (no substring match)

The test framework compares the **first line** of CLI output against your expected string using `-eq` (exact equality). This means:
- Each `Add-Test` call takes a single `[string]$ExpectedLine` parameter (not an array)
- Pass a bare string, **not** `@('...')` (the old array wrapping causes array-unrolling bugs in parallel runspaces)

```powershell
# GOOD — bare string:
Add-Test 'GROUP' 'name' @('args') 'Full expected first line...'

# BAD — @(...) wrapping produces single-char first line in ForEach-Object -Parallel:
Add-Test 'GROUP' 'name' @('args') @('Full expected first line...')  # ← DO NOT USE
```

**Basic search (all 3 terms):**
```powershell
foreach ($t in @('term1', 'term2', 'term3')) {
  Add-Test `
    'TRANSLATION_CODE' `
    "search $t in $code" `
    @('search', $t, 'in', $code) `
    'BookName 1:1 Full expected first line text...'
}
```

**Book/extended tests (first term only, following the existing pattern):**
- `search FirstTerm --book romans in $code` → first hit in Romans
- (optional) `search FirstTerm in $code --book romans --chapter 2` → if the exact phrase appears in chapter 2
- `search FirstTerm in $code --book romans --chapter 3 --end-chapter 5` → verse in chapter 3
- `search FirstTerm in $code in "johns letters"` → 1 John 1:3

**IMPORTANT:** Run the CLI for `--book romans` and `--chapter 2` variants to verify actual first hit. The Kotlin API might expect Romanos 1:1, but the CLI phrase search may return Romanos 1:4 as the first hit (if "Jesus Cristo" as exact phrase doesn't appear in verse 1). If `--chapter 2` returns NO results (because the exact phrase doesn't appear in chapter 2), skip that test case.

## Step 6b: Embed non-ASCII characters directly with here-strings

You can write non-ASCII characters (é, ü, ö, ä, ß, etc.) directly in the expected line using PowerShell **single-quoted here-strings** (`@'...'@`):

```powershell
Add-Test 'LSG' 'name' @('args') @'
Matthieu 1:1 Généalogie de Jésus-Christ, fils de David, fils d'Abraham.
'@
```

After writing the file, **always enforce UTF-8 encoding** — this is critical because edit tools may not preserve the file encoding:

```powershell
$content = Get-Content .\test_search.ps1 -Raw
Set-Content .\test_search.ps1 -Value $content -Encoding utf8
```

For curly/smart quotes `"` (U+201C) and `"` (U+201D) in CLI output, embed them directly in the here-string if they transmit correctly; otherwise fall back to `$([char]0x201C)` / `$([char]0x201D)` in a double-quoted interpolated string.

### Why here-strings instead of `[char]` variables

The old approach used `[char]0x00XX` variables because the tool chain couldn't reliably transmit non-ASCII characters in double-quoted strings. Using `@'...'@` here-strings with `-Encoding utf8` enforce-save resolves this:
- The PowerShell parser reads the here-string verbatim
- The file is guaranteed to be UTF-8 on disk regardless of what the edit tool produced
- Results in much cleaner, more readable expected lines

## Step 7: Verify

```powershell
Set-Location bbl_install
bundle exec kitchen converge search-script-windows  # only needed first time or after binary changes
bundle exec kitchen verify search-script-windows
```

If tests fail, the error message shows both the expected and actual first line. Copy the **actual** first line from the failure output and use it as your expected string to fix the mismatch.

