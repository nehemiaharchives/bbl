Kitchen install fixtures are placed here by CI artifact downloads before E2E runs.
For local Kitchen runs, prepare this directory with the platform-specific Gradle task:

```sh
./gradlew stageBblInstallWindowsCliAllFixture
./gradlew stageBblInstallLinuxCliAllFixture
./gradlew stageBblInstallMacosArm64CliAllFixture
./gradlew stageBblInstallMacosX64CliAllFixture
./gradlew stageBblInstallMacosPkgFixture -Pbblpacks.embed=false
```

The generic package task selects the current Mac architecture. CI uses
`stageBblInstallMacosArm64PkgFixture`; Intel Macs can explicitly use
`stageBblInstallMacosX64PkgFixture`.

The per-binary fixture tasks still write isolated outputs under `build/bblInstallFixtures/`.
The `*CliAllFixture` tasks flatten the selected platform's generated files into this directory.
The generated files are intentionally ignored by Git.

The macOS package suite installs `/usr/local/bin/bbl` and registers `org.gnit.bbl`.
After a local run, clean up with:

```sh
sudo rm -f /usr/local/bin/bbl
sudo pkgutil --forget org.gnit.bbl || true
```
