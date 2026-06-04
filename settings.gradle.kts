rootProject.name = "bbl"
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

val useLocalLuceneKmp = providers.gradleProperty("useLocalLuceneKmp").orNull == "true" ||
    providers.environmentVariable("USE_LOCAL_LUCENE_KMP").orNull == "true"

if (/*useLocalLuceneKmp && file("../lucene-kmp").isDirectory*/ // this is commented out for now because we at this point we do not dogfood development of bbl and lucene-kmp
    false /* this is commented out because we do not work on dog fooding */
    ) {
    logger.lifecycle("Using sibling lucene-kmp composite build via useLocalLuceneKmp=true")
    includeBuild("../lucene-kmp") {
        dependencySubstitution {
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-core")).using(project(":core"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-queryparser")).using(project(":queryparser"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-common")).using(project(":analysis:common"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-extra")).using(project(":analysis:extra"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-kuromoji")).using(project(":analysis:kuromoji"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-morfologik")).using(project(":analysis:morfologik"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-nori")).using(project(":analysis:nori"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-analysis-smartcn")).using(project(":analysis:smartcn"))
            substitute(module("org.gnit.lucene-kmp:lucene-kmp-test-framework")).using(project(":test-framework"))
        }
    }
}

// shared code among all project
include(":core")

// server
include(":server")

// cli tools
include(":cli:shared")
include(":cli:core")
include(":cli:packer")
include(":cli:search")
include(":cli:search:common")
include(":cli:search:extra")
include("cli:search:kuromoji")
include("cli:search:morfologik")
include("cli:search:nori")
include("cli:search:smartcn")

// TODO add cli:search:xxx modules

// mobile and desktop apps
include(":app:shared")
include(":app:androidApp")
include(":app:desktopApp")

// shared test related code
include(":test-framework")
