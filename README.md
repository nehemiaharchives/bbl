# Bible Kotlin Multiplatform

bbl-kmp is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM), Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/cli](./cli/src/commonMain/kotlin) is for bbl command line Bible. 

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Regenerate bbl Packs

`server/src/main/resources/files/bbltexts/` is the source content for downloadable bbl packs.
`server/src/main/resources/files/bblpacks/*.zip` is the canonical pack fixture and release payload used by
`bbl install`, Chef/Inspec fixture staging, and CLI release archives.

When lucene-kmp index format or analyzer behavior changes, regenerate packs explicitly instead of using a test
method as a packer entry point:

- one translation while iterating on an analyzer:
  ```shell
  ./gradlew packBblTranslation -Pbblpack.translation=sven
  ```
- every downloadable translation before publishing or after an index compatibility change:
  ```shell
  ./gradlew packBblAllTranslations
  ```
- verify committed server zips match the current bbl version:
  ```shell
  ./gradlew verifyServerBblPackVersions
  ```

JetBrains run configurations are committed for the same three commands:
`packBblTranslation sven`, `packBblAllTranslations`, and `verifyServerBblPackVersions`.
Change the `bblpack.translation` value in the first configuration when working on another language.

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
