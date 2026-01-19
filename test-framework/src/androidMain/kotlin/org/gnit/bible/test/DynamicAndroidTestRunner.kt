package org.gnit.bible.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.InitializationError
import org.junit.runners.model.RunnerBuilder

/**
 * Uses Robolectric when available (host tests) and AndroidJUnit4 on device.
 * Reflection avoids hard dependency on Robolectric for device tests.
 */
class DynamicAndroidTestRunner(testClass: Class<*>) : Runner() {
    private val delegate: Runner = createDelegate(testClass)

    override fun getDescription() = delegate.description

    override fun run(notifier: RunNotifier) {
        delegate.run(notifier)
    }

    private fun createDelegate(testClass: Class<*>): Runner {
        // If instrumentation is available, prefer AndroidJUnit4 (device tests).
        if (hasInstrumentation()) {
            return AndroidJUnit4(testClass)
        }
        // Otherwise, fall back to Robolectric when on the host.
        val robolectricRunner = tryLoadRobolectricRunner(testClass)
        if (robolectricRunner != null) return robolectricRunner
        return AndroidJUnit4(testClass)
    }

    private fun tryLoadRobolectricRunner(testClass: Class<*>): Runner? {
        return try {
            val clazz = Class.forName("org.robolectric.RobolectricTestRunner")
            val ctor = clazz.getConstructor(Class::class.java)
            ctor.newInstance(testClass) as Runner
        } catch (_: ClassNotFoundException) {
            null
        } catch (e: Exception) {
            // If Robolectric is present but cannot be initialized, surface the error.
            throw InitializationError(e)
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
        } catch (e: Exception) {
            // AndroidJUnit4 throws when not running under instrumentation.
            false
        }
    }
}
