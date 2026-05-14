Kitchen install fixtures are placed here by CI artifact downloads before E2E runs.
Local Gradle staging writes isolated per-platform/per-binary fixture directories under:

```sh
build/bblInstallFixtures/
```

The generated files are intentionally ignored by Git.
