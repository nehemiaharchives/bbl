# AGENTS.md for bbl-kmp Project

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

* 1. Apply the chosen code changes.
* 2. If you have access to JetBrains/Android Studio MCP server,
  * 2.1. Try to use find problems tool to check if there are any errors and iterate over till all errors are fixed.
  * 2.2. Try to compile so that you can find compilation errors and fix/edit iterate till it compiles without errors.
* 3. Run builds/tests and fix compilation errors.
  * 3.1. If you have access to JetBrains/Android Studio MCP server, find run configuration and run compileKotlin task to find compilation errors then iterate over until you fix all of them.
  * 3.2. If you don't have access to JetBrains/Android Studio MCP server, use terminal to run ./gradle compileKotlin commands to find compilation errors then iterate over until you fix all of them.
* 4. Perform internet search **only** if an error is unclear and you need confirmation of a fix. If you are confident in the solution, skip research and proceed.

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

## Code, Run, Debug Cycle

### Priority 1, IntelliJ IDEA MCP Server
When you have access to IntelliJ IDEA MCP server, you should use the IDEA's internal test runner.

### Priority 2, Gradle command line
When you don't have access to IntelliJ IDEA MCP server, you should use the command line Gradle test runner.
