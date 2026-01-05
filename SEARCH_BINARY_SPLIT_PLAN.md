## Problem

I ran `gradle linkReleaseExecutableMacosX64` and got this:

```
➜  releaseExecutable git:(master) ✗ ls -alh
total 654272
drwxr-xr-x@ 4 joel  staff   128B Jan  4 20:03 .
drwxr-xr-x@ 6 joel  staff   192B Jan  4 17:29 ..
-rwxr-xr-x@ 1 joel  staff   319M Jan  4 17:34 bbl.kexe
drwxr-xr-x@ 3 joel  staff    96B Jan  4 17:34 bbl.kexe.dSYM
➜  releaseExecutable git:(master) ✗ pwd
/Users/joel/code/bbl-lucene/bbl-kmp/cli/build/bin/macosX64/releaseExecutable
```

The bbl-kmp had goal to produce single binary because it is a command line application.
the previous bbl (kotlin/jvm) was not able to produce single binary so I wanted to solve this problem with next generation bbl-kmp.

So, the default compilation target is decided to be Kotlin/Native on macos and linux but that means not able to benefit from jvm ecosystem which ended up porting entire Apache Lucene to be ported into lucene-kmp project only for make the full text search capability available in Kotlin/Native.
bbl-kmp now has lucene-kmp and its analyzers for each languges.

For example, we know that `lucene-kmp-analisis-xxx` modules contain embedded dictionary data to perform morphological analysis e.g. `morfologik`, `smartcn`, `nori` and `kuromoji`. each of dicitonary data add ups 10MB to 80MB.

On top of that, bbl-kmp has embedded bible text and search index as resources which is compiled into C, then O, then A file using llvm. 15 translations are embedded with each size 20MB or so.

For downloadable translations, they are packed into a zip file using `bbl pack` command then commited insdie of resources dir of `server` module then will be downloaded via `bbl install ${translation.code}`. This downloadable bible pack mechanism seems good to reduce binary size.

However, bbl-kmp includes lucene-kmp analyzers for languages of downloadable bibles for example vietnamiese, tagalog etc. This means those who never read Bible in those languages has to download dead weight binary of those analyzers together with core module.

### Additional root cause: indexing/packing also drags analyzers into core
Even if we split the *runtime search* into external binaries, the current *index build / pack* flow also depends on analyzers.

- `PackCli` calls `IndexBuilder.createLuceneKmpIndex(...)`.
- `IndexBuilder.createLuceneKmpIndex(...)` currently chooses analyzer via an `AnalyzerProvider` (defaults to `DefaultAnalyzerProvider`).

This means `bbl-kmp:cli` (and/or `bbl-kmp:core` where these classes are also used) ends up depending on language analyzers such as `kuromoji`, `nori`, etc.

Therefore, in addition to refactoring `SearchEngine.kt`, `PackCli` + `IndexBuilder` must also be refactored so that the main `bbl` binary and any shared/core modules don’t need to depend on heavyweight analyzer modules.

## Possible Solution
Give up 100% single binary strategy, but provide split binaries built with Kotlin/Native each of them work independently and/or, together with.

`bbl search $term` commands will be independent into multiple binaries to perform search on specific language which will be downloaded and installed on demand.
The desitnateion will be `$HOME/.bbl/bin/` file names will be `bbl-search-${lucene-kmp-analysis.module}` like this:
1. `bbl-search-common` EnglishAnalyzer, SpanishAnalyzer, etc
2. `bbl-search-morfologik` MorfologikAnalyzer for Polish, UkranianMorfologikAnalyzer for Ukranian (comes with embedded dictionary data)
3. `bbl-search-smartcn` ChineseAnalyzer (comes with embedded dictionary data)
4. `bbl-search-nori` KoreanAnalyzer (comes with embedded dictionary data)
5. `bbl-search-kuromoji` JapaneseAnalyzer (comes with embedded dictionary data)
6. `bbl-search-extra` VietnameseAnalyzer, UrudoAnalyzer etc

My assumption is, splitting per language module is too much work and does not pay cost of investment. the lucene-kmp library is split into those 6 gradle dependenceis. So I guess splitting `bbl-sesarch-common` which is already small size does not make sense only resulting in increasing the number of modules to create and mantain.

`bbl-search-common` will contain `EnglishAnalyzer` and `bbl-search-kuromoji` will contain `JapaneseAnalyzer` inside of its binary so that main binary `bbl` does not need to contain them like now causing 300MB problem.

`bbl-search-common` works independently. It reads bbl pack zip file stored in `$HOME//.bbl/packs` where current implementation of `bbl install ${translation.code}` installs bbl pack zip files into. so the command will be something like `bbl-search-common ${translation.code}` e.g. `bbl-search-common -t webus $term`, `bbl-search-common -t kjv $term` or `bbl-search-kuromoji -t jc $term`.

`bbl install ${translation.code}` will be changed not only instaling specific ${translation} but also compute depending search binary to be installed together with bbl translation pack zip file. e.g. `bbl install jc` will download and install both `jc` and if not found `bbl-search-kuromoji`.

`bbl-search-common` could be merged as part of single binary with `bbl-kmp:cli:core` because `lucene-kmp:analysis:core` module is not such big but contains many Analyzers for many languages including `EnglishAnalyzer` and combining `:bbl-kmp:cli:core` and `lucene-kmp:analyzer:common` will preserve "out of the box single binary convenience and simplicity" and I think this good enough to justify the 5-10?MB of increase in the core bbl binary size. In that case, we need to have some interface and 2 different implementation of the interface to delegate search, one is for internal-embedded `bbl-search-common`, one is for external-download `bbl-search-${analysis-module}` which runs exec.

As a result, Polish, Ukranian, Chinese, Korean, Japanse translations must be moved from embedded translation to downloadable translation and it breaks the backword compatibility but I think it can not be helped. However, I think we can add feature to "automatic OS language detection and automatic download and install the default translation bbl pack and search binary"

The main command `bbl` then do not need to contain large size Analyzers other than `bbl-search-common`. but it can delegate each search binaries in the way, `bbl search $term in ${translation.code}` will
1. in case the ${trnslation.langauge.code} is covered by `bbl-search-common` execute search within bbl binary.
2. if not, `bbl-kmp:cli` assumes and run verification/check logic that `$HOME/.bbl/bin/` is in the `$PATH`. or `$HOME/.bbl/bin/bbl-sarch-${analysis-module}` absolute path. then automatically select `bbl-sarch-${analysis-module}` to execute outside search binary. Installer need to do `chmod +x` to make those binaries executable.

## PackCli, IndexBuilder, Translation, Language
`bbl-kmp:indexbuilder` module need to be newly created to decouple indexing logic from `bbl-kmp:cli` module.

Analyzer selection in `SearchEngine`/`IndexBuilder` now flows through `AnalyzerProvider` and should be kept out of `bbl-kmp:core` for non-common analyzers.
`bbl pack` command needs to be `bbl-pack` command line tool separately first with kotlin/jvm because this is developer tool to create new bbl pack with text and search index. For now the user of the pack command is only me, the author of bbl. So the size does not matter that much. So all Analyzers and all analysis modules can be linked into `bbl-pack` command line tool. It does not even need to be a executable, but it can just be an main() function to be called from Intellij run configuration. As a result, a new module named `bbl-kmp:cli:packer` will be created to contain `PackCli.kt` and `IndexBuilder.kt`. In the future, if advanced user of bbl wants to create their own we can delegate `bbl pack` command to `bbl-pack` command line tool. It will be installed via `bbl install packer` or something.

## Steps to implement/refactor
1. Add `bbl-search-${analysis-module}` gradle modules with name `bbl-kmp:cli:search:${analysis-module}` e.g. `bbl-kmp:cli:search:common` `bbl-kmp:cli:search:kuromoji` etc for all supported languages. implement search feature there with unit tests.
2. Make `bbl search $term` command to delegate execution of search to `bbl-search-common` internally, and `bbl-search-${analysis-module}` externally (upto this point, try to only add features, do not change existing code unless absolutely needed, if you need to change, change should be minimal).
3. Refactor `SearchEngine.kt` so that analyzer selection is not done by linking all analyzers into the main binary. Instead, the main `bbl` binary should either:
   - execute internal search only for languages supported by `bbl-search-common`, OR
   - delegate to external `bbl-search-${analysis-module}` binary.
4. Refactor `PackCli` and `IndexBuilder` to decouple indexing from heavyweight analyzer modules:
   - Move analyzer-dependent indexing code into the corresponding `bbl-search-${analysis-module}` modules, **or**
   - Introduce an explicit `AnalyzerProvider` / "analysis module" mapping that lives outside `core` and is only linked into the search/index binaries that need it.

   The end goal is: building `bbl` (core CLI binary) should not link `kuromoji`, `nori`, `smartcn`, `morfologik` dictionaries unless explicitly requested.
5. Delete search and analyzer related dependencies from `bbl-kmp:cli` except `bbl-search-common`
