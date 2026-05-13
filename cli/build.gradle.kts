plugins {
    base
}

val stageBblInstallFixtures = rootProject.tasks.named("stageBblInstallFixtures")

tasks.register("linkReleaseExecutableLinuxX64") {
    group = "build"
    description = "Build all Linux release CLI executables and stage install fixtures."

    dependsOn(
        ":cli:core:linkReleaseExecutableLinuxX64",
        ":cli:search:common:linkReleaseExecutableLinuxX64",
        ":cli:search:extra:linkReleaseExecutableLinuxX64",
        ":cli:search:kuromoji:linkReleaseExecutableLinuxX64",
        ":cli:search:morfologik:linkReleaseExecutableLinuxX64",
        ":cli:search:nori:linkReleaseExecutableLinuxX64",
        ":cli:search:smartcn:linkReleaseExecutableLinuxX64",
    )

    finalizedBy(stageBblInstallFixtures)
}

tasks.register("linkReleaseExecutableMingwX64") {
    group = "build"
    description = "Build all Windows release CLI executables."

    dependsOn(
        ":cli:core:linkReleaseExecutableMingwX64",
        ":cli:search:common:linkReleaseExecutableMingwX64",
        ":cli:search:extra:linkReleaseExecutableMingwX64",
        ":cli:search:kuromoji:linkReleaseExecutableMingwX64",
        ":cli:search:morfologik:linkReleaseExecutableMingwX64",
        ":cli:search:nori:linkReleaseExecutableMingwX64",
        ":cli:search:smartcn:linkReleaseExecutableMingwX64",
    )
}
