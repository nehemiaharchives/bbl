# AGENTS.md for bbl-kmp Project

## Project Overview
This project is bbl-kmp, a set of software to read/search bible in many languages.
First software is [bbl-kmp/cli] which is Kotlin/Native single binary executable command line tool targeting macOS and Linux
Second software is [bbl-kmp/composeApp] which is Compose Multiplatform GUI app targeting Android/iOS/Desktop JVM
The tech stack is Kotlin Multiplatform and both cli and composeApp depends on [bbl-kmp/shared] in this project and [lucene-kmp]  
[lucene-kmp] is a Kotlin Multiplatform work in progress port of Apache Lucene Java search engine library and
[lucene-kmp] is made for [bbl-kmp] and nobody has used/tested word yet, so a lot of bug is expected.
[lucene-kmp] core functions are ported but many components should still need to be ported from Java version. 
So [bbl-kmp/cli] is the world first user of [lucene-kmp] and aiming to heavily dogfood, debug and make it working.
[bbl-kmp] development proceeds at the same time [lucene-kmp] whenever you find bug, you need to create github issue using github mcp server in [lucene-kmp] github repo.
We develop bbl-kmp together with [lucene-kmp]. [bbl-kmp] dogfood [lucene-kmp] as primary user project. This means whenever poor, incorrect or unexpected search result appears, we will fix [lucene-kmp]. So remember that whenever we see poor analyzer behavior, stem problem, certain words/stemming etc not recognized, and other problem which could be fixed by [lucene-kmp], we will always go back to [lucene-kmp] to fix them to improve [lucene-kmp] search quality. So we never compromise test assertions in [bbl-kmp] but always fix [lucene-kmp].

## Language

Use **English** to communicate with me.

## Agent‑Human Coworking Flow

### Step 1: Suggest & Discuss

* Propose multiple solutions using your built‑in knowledge.
* Only perform external research if:
  * You are uncertain about an API or behavior, **or**
  * I explicitly request official documentation or references.
* Otherwise, skip research and move straight to proposing fixes.

### Step 2: Code, Run, Debug

1. Apply the chosen code changes. when you code be careful of writing code in platform agnostic kotlin common code. avoid expect/actual pattern as much as possible. do not mix platform specific code such as jvm code in commonMain/commonTest.
2. After any code change, run JetBrains `open_file_in_editor` on the edited file, wait 2 seconds, then run `get_file_problems` on that same file and fix compilation errors immediately. iterate over until you solve all errors. 
3. Use `get_run_configuration` tool `jetbrains` MCP server to find proper run configuration. to run specific unit test and use execute_run_configuration to run tests. if any test fail, find out root cause, iterate over until you fix all of them
4. Perform internet search **only** if an error is unclear and you need confirmation of a fix. If you are confident in the solution, skip research and proceed.

## Tool Use Priority

### Priority 1, jetbrains MCP Server (always)
When you have access to jetbrains MCP server, you should use the IDEA's internal test runner. `.run` dir contains.
Example agent runtime environment: locally running ai coding agent in desktop/laptop of a developer such as codex cli, GitHub Copilot Agent.

### Priority 2, Gradle command line (avoid as much as possible)
When you don't have access to jetbrains MCP server, first ask Human developer to enable it and wait until it is enabled! Never use Gradle wrapper (./gradlew).
If you are in cloud environment where you have NO access to jetbrains MCP server, you are allowed to use the command line Gradle wrapper (./gradlew) to compile and run tests.
Example agent runtime environment: desktop/laptop but human developer forgot to launch JetBrains IDEs, or cloud coding agent such as codex web, Google Jules.

### Start running tests in specific test function, then specific test file, then all test files.
Principle: gradle execution takes time, and we want to save time to wait for tests. Do not waste time running unnecessarily tests when smaller tests is enough to detect fail. Run `allTest` at last when you confirmed tests related to your change passes.
For example, you have changed `BookRange` of `shared/src/commonMain/kotlin/org/gnit/bible/BibleFilter.kt`
1. First you need to run `fun bookRangeTest()` of `BibleFilterTest` to verify quickly with less time to spend. If it fail iterate over until the specific test function pass.
2. Then you proceed to run each test within `BibleFilterTest`, if some test fail, find out root cause, iterate over until you fix all of them.
3. If your change only affects specific module `cli` or `composeApp`, then run `bbl-kmp_cli [allTests]` or `bbl-kmp_composeApp [allTests]` instead to save time.
4. Run `bbl-kmp [allTests]` after tests for small changes pass, or when you change code in `shared` module or `test-framework` module which is used in other modules to verify anything not broken with your change.

### Test workflow

When you run a Gradle test command, do not rely only on your own quick reading of the output.

After the test command finishes, spawn `test_result_reviewer` and give it the complete command, exit code, and relevant stdout/stderr. Wait for its report. Then tell the user:

- whether the test passed or failed
- the failing Gradle task, test class, or exception if any
- the next recommended action

## Git Commit Policy (GPG Signed)

- When user asks to commit, always create a GPG-signed commit.
- Use per-command unsandboxed execution (escalated command) for signing commands.
- Run commit command in a PTY and export `GPG_TTY=$(tty)` in the same command.
- Standard commit flow:
1. `git add <intended files only>`
2. `export GPG_TTY=$(tty) && git commit -S -m "<message>"`
3. `git log --show-signature -1` and confirm `Good signature`.
- Do not fall back to unsigned commit unless the user explicitly asks for unsigned commit.

## Internet Research Guidelines
* Use official sources **only when needed**:
  * Kotlin: [https://kotlinlang.org/docs/](https://kotlinlang.org/docs/) and [https://github.com/JetBrains/kotlin](https://github.com/JetBrains/kotlin)
  * Android: [https://developer.android.com](https://developer.android.com) and [https://android.googlesource.com/platform/frameworks/base](https://android.googlesource.com/platform/frameworks/base)
  * Apple platforms: [https://developer.apple.com/documentation](https://developer.apple.com/documentation)
* When fetching open source code, use [https://raw.githubusercontent.com](https://raw.githubusercontent.com).
* Third‑party sources (Stack Overflow, blogs, etc.) are acceptable only as a last resort.
* Confirm the relevant versions (Kotlin, Gradle, macOS, Xcode, JDK, Clang/LLVM, etc.) before suggesting a solution to ensure compatibility with this project.

## Fast Mode Option

If I say **"fast mode"**, skip all research unless absolutely required. Focus on rapid code editing, running, and debugging.
