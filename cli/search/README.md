# bbl search helper binaries (protocol)

This directory documents the protocol used by **external search helper binaries**.

The main `bbl` CLI delegates search to these helpers when a translation’s
`Language.searchModuleId` is not `COMMON`.

Helper binaries are installed into:

- `$HOME/.bbl/bin/`

and are named:

- `bbl-search-<moduleId>`
  - examples: `bbl-search-kuromoji`, `bbl-search-nori`, `bbl-search-smartcn`, ...

## Command line contract

### Executable name

`bbl-search-${moduleIdLowercase}`

Where `moduleIdLowercase` is the lowercase name of `SearchModuleId`.

### Arguments

Helpers MUST accept the same CLI shape as `bbl search` for the delegated subset:

Required:

- `-t <translationCode>` (or `--translation <translationCode>`)
- `<term...>` one or more positional args forming the search query.
  - The main `bbl` process joins these with a single space.

Optional:

- `--book <bookNumber>`
- `--chapter <chapterNumber>` (requires `--book`)
- `--end-chapter <chapterNumber>` (requires `--book` and `--chapter`)
- `--verses <maxResults>` (must be > 0)

Notes:

- Helpers do **not** need to implement translation resolution or installation logic.
  If the pack for `translationCode` is missing, they should fail with a clear message.

### Output

Stdout MUST match `bbl search` output semantics exactly:

- One result per line.
- Each line formatted as:

```
<BookName> <chapter>:<verse> <verseText>
```

This is the same format produced by `SearchEngine` and used by `InternalSearchBackend`.

If there are no results, print nothing to stdout and exit `0`.

### Errors

On error, helpers MUST:

- exit with non-zero code
- write a human-readable explanation to **stderr**

The delegating CLI surfaces stderr to users.

## Minimal example

A helper invocation produced by the main CLI looks like:

```
~/.bbl/bin/bbl-search-kuromoji -t jc --book 1 --chapter 1 --verses 5 grace
```

## Test fixture pack

All helper binaries should be able to run against a minimal deterministic test pack.

See `shared/src/commonTest/resources/bblpacks/fixture/`.

