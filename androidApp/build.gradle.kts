plugins {
    //alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    android {
        namespace = "org.gnit.bible.android"
    }

    dependencies {
        implementation(projects.shared)
        implementation(projects.composeApp)
        implementation(compose.preview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlin.logging)
        implementation(libs.slf4j.android)
    }
}


android {
    namespace = "org.gnit.bible"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.gnit.bible"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude duplicate META-INF index files pulled in by logging jars (e.g., logback)
            excludes += "META-INF/INDEX.LIST"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // THIS IS IMPORTANT FOR ROBOLECTRIC
    testOptions {
        unitTests {
            // This tells Gradle to use Robolectric for unit tests
            // that require the Android framework.
            isIncludeAndroidResources = true // Essential for Robolectric to access resources

            // If you were using JUnit 5 directly with Robolectric, you might add:
            // useJUnitPlatform()
            // However, with RobolectricTestRunner (JUnit 4 based), this isn't standard.
            // If RobolectricTestRunner internally leverages parts of JUnit Platform,
            // it's usually handled by Robolectric itself.
        }
    }
}

/*target {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}*/
