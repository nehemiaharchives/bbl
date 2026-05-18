val sharedKotlinDataDir = File(System.getProperty("user.home"), ".konan").absolutePath
val configuredKotlinDataDir = System.getProperty("kotlin.data.dir")
if (configuredKotlinDataDir != sharedKotlinDataDir) {
    logger.lifecycle(
        "Using kotlin.data.dir=$sharedKotlinDataDir for bbl-kmp composite native builds"
            + (configuredKotlinDataDir?.let { " instead of $it" } ?: "")
    )
    System.setProperty("kotlin.data.dir", sharedKotlinDataDir)
}

rootProject.name = "Bible"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// --- Optional local composite dependency on sibling lucene-kmp ---
// This repository is often used as a parent workspace that also contains a sibling
// checkout of lucene-kmp. When that sibling exists, use it for fast iteration.
// When it doesn't (e.g. cloned standalone / CI), fall back to published artifacts.
val luceneKmpSiblingDir = file("../lucene-kmp")

// When we develop bbl-kmp feature other than search, we can make following switch to "false" to speed up build.
// When we encounter poor, incorrect or unexpected search result, we switch back to overwriting it by local lucene-kmp so the lucene-kmp changes are reflected in bbl-kmp just by compiling dependent modules without needing to publish lucene-kmp to maven local and updating bbl-kmp's lucene-kmp version.
if (
    luceneKmpSiblingDir.isDirectory
    //false // now we work on bbl-kmp and lucene-kmp side by side.
    ) {

    logger.lifecycle("Found sibling lucene-kmp at ${luceneKmpSiblingDir.absolutePath} substituting maven published lucene-kmp dependency with local development version")

    includeBuild("../lucene-kmp") {
        dependencySubstitution {
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-core")).using(project(":core"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-queryparser")).using(project(":queryparser"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-common")).using(project(":analysis:common"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-morfologik")).using(project(":analysis:morfologik"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-smartcn")).using(project(":analysis:smartcn"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-nori")).using(project(":analysis:nori"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-kuromoji")).using(project(":analysis:kuromoji"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-extra")).using(project(":analysis:extra"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-test-framework")).using(project(":test-framework"))
        }
    }
}

include(":composeApp")
include(":androidApp")
include(":server")
include(":shared")
include(":cli")
include(":test-framework")
include(":cli:search")
include(":cli:search:common")
include(":cli:search:morfologik")
include(":cli:search:smartcn")
include(":cli:search:nori")
include(":cli:search:kuromoji")
include(":cli:search:extra")
include(":cli:packer")
include(":cli:core")
