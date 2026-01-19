package org.gnit.bible.test

import android.content.ContentProvider
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.gnit.bible.Platform
import org.gnit.bible.getPlatform
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(DynamicAndroidTestRunner::class)
actual abstract class ResourcesTestBase actual constructor() {

    actual fun createTestPlatform(): Platform {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        return getPlatform(ctx)
    }

    @Before
    fun setupResourcesTestBaseForAndroidPlatform() {
        setupAndroidContextProvider()
    }

    // Initializes Compose's AndroidContextProvider when Robolectric is available (host tests).
    private fun setupAndroidContextProvider() {
        if (hasInstrumentation()) {
            return
        }
        val providerClassName = "org.jetbrains.compose.resources.AndroidContextProvider"
        val robolectricClassName = "org.robolectric.Robolectric"
        try {
            @Suppress("UNCHECKED_CAST")
            val providerType = Class.forName(providerClassName) as Class<ContentProvider>
            val robolectric = Class.forName(robolectricClassName)
            val setup = robolectric.getMethod("setupContentProvider", Class::class.java)
            setup.invoke(null, providerType)
            println("$providerClassName initialized successfully for Robolectric.")
        } catch (_: ClassNotFoundException) {
            // Robolectric or Compose provider not on classpath (device tests).
        } catch (e: Exception) {
            println("Error setting up $providerClassName: ${e.message}")
            throw e
        }
    }

    private fun hasInstrumentation(): Boolean {
        return try {
            val registry = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            val method = registry.getMethod("getInstrumentation")
            method.invoke(null)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: NoSuchMethodException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
