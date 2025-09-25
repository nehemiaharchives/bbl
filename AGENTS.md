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

* Apply the chosen code changes.
* Run builds/tests and fix compilation or runtime errors.
* Perform internet search **only** if an error is unclear and you need confirmation of a fix. If you are confident in the solution, skip research and proceed.

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
