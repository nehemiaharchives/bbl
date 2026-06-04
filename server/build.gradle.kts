plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "org.gnit.bible"
version = "1.0.0"
application {
    mainClass = "org.gnit.bible.server.ApplicationKt"
}

dependencies {
    api(projects.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

tasks.register<Sync>("syncBblPacks") {
    from(rootProject.layout.projectDirectory.dir("resources/bblpacks"))
    into(layout.buildDirectory.dir("processedResources/files/bblpacks"))
    include("*.zip")
}

tasks.register<Sync>("syncBblTexts") {
    from(rootProject.layout.projectDirectory.dir("resources/bbltexts"))
    into(layout.buildDirectory.dir("processedResources/files/bbltexts"))
}

kotlin {
    sourceSets {
        val main by getting {
            resources.srcDir(tasks.named("syncBblPacks"))
            resources.srcDir(tasks.named("syncBblTexts"))
        }
    }
}
