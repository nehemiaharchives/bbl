Kitchen install fixtures are placed here by CI artifact downloads before E2E runs.
For local Kitchen runs, prepare this directory with the platform-specific Gradle task:

```sh
./gradlew stageBblInstallWindowsCliAllFixture
./gradlew stageBblInstallLinuxCliAllFixture
./gradlew stageBblInstallMacosArm64CliAllFixture
./gradlew stageBblInstallMacosX64CliAllFixture
```

The per-binary fixture tasks still write isolated outputs under `build/bblInstallFixtures/`.
The `*CliAllFixture` tasks flatten the selected platform's generated files into this directory.
The generated files are intentionally ignored by Git.
