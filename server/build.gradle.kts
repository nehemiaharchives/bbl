plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "org.gnit.bible"
version = "1.0.0"
application {
    mainClass.set("org.gnit.bible.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okio)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

kotlin {
    sourceSets {
        main.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonMain/composeResources").asFile
        )

        test.get().resources.srcDir(
            rootProject.layout.projectDirectory
                .dir("composeApp/src/commonTest/composeResources").asFile
        )
    }
}
