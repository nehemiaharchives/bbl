package org.gnit.bible.test

import android.content.ContentProvider
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.gnit.bible.Platform
import org.gnit.bible.getPlatform
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [/* e.g., Build.VERSION_CODES.P */], manifest = Config.DEFAULT_MANIFEST_NAME)
actual abstract class ResourcesTestBase {

    actual fun createTestPlatform(): Platform {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        return getPlatform(ctx)
    }

    @Before
    fun setupResourcesTestBaseForAndroidPlatform() {
        setupAndroidContextProvider()
    }

    // Configures Compose's AndroidContextProvider to access resources in tests.
    // See https://youtrack.jetbrains.com/issue/CMP-6612
    private fun setupAndroidContextProvider() {
        val providerClassName = "org.jetbrains.compose.resources.AndroidContextProvider"
        try {
            @Suppress("UNCHECKED_CAST")
            val type = Class.forName(providerClassName) as Class<ContentProvider>
            // Robolectric.buildContentProvider(type).create().get() might be more robust
            // if it needs full lifecycle.
            Robolectric.setupContentProvider(type)
            println("$providerClassName initialized successfully for Robolectric.")
        } catch (e: ClassNotFoundException) {
            println("Class not found: $providerClassName. This is expected if the test doesn't use Compose resources.")
        } catch (e: Exception) {
            println("Error setting up $providerClassName: ${e.message}")
            // Rethrow or handle as a test failure if this provider is essential for all tests
            // using this base class.
            throw e
        }
    }
}
