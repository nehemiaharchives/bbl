package org.gnit.bible.test

import android.content.ContentProvider
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okio.FileSystem
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.Platform
import org.gnit.bible.Translation.Companion.downloadableTranslationsCmp
import org.gnit.bible.Translation.Companion.embeddedTranslationCodes
import org.gnit.bible.getPlatform
import org.junit.Before
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream

@RunWith(DynamicAndroidTestRunner::class)
actual abstract class ResourcesTestBase actual constructor() {

    init {
        ensureResourcesSetup()
    }

    actual fun createTestPlatform(): Platform {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        return getPlatform(ctx)
    }

    actual fun seedComposePackDirIfNeeded(platform: Platform) {
        // No-op on Android; packs are handled via assets/downloads in setup.
    }

    @Before
    fun setupResourcesTestBaseForAndroidPlatform() {
        ensureResourcesSetup()
    }

    private fun ensureResourcesSetup() {
        if (!resourcesPrepared) {
            synchronized(resourcesLock) {
                if (!resourcesPrepared) {
                    setupAndroidContextProvider()
                    resourcesPrepared = true
                }
            }
        }
        ensureDownloadedPacks()
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

    private fun ensureDownloadedPacks() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val packDirs = linkedSetOf(
            getPlatform(ctx).packDir,
            (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "bbl_compose_bible_test_dir").toString()
        )
        val canonicalPackDir = findCanonicalPackDir()
        val downloadableCodes = downloadableTranslationsCmp.map { it.code }

        downloadableCodes.forEach { code ->
            if (embeddedTranslationCodes.contains(code)) {
                return@forEach
            }
            val sourceFile = canonicalPackDir?.let { File(it, "$code.zip") }?.takeIf { it.isFile }
            if (sourceFile != null) {
                packDirs.forEach { packDir ->
                    copyZipIfOutdated(sourceFile, File(packDir, "$code.zip"))
                }
                return@forEach
            }
            packDirs.forEach { packDir ->
                copyZipIfMissing({ openAssetZip(ctx, code) }, File(packDir, "$code.zip"))
            }
            packDirs.forEach { packDir ->
                downloadZipIfMissing(code, File(packDir, "$code.zip"))
            }
        }
    }

    private fun copyZipIfMissing(openStream: () -> InputStream?, destination: File) {
        if (destination.exists() && destination.length() > 0L) {
            return
        }
        val input = openStream() ?: return
        destination.parentFile?.mkdirs()
        input.use { inStream ->
            destination.outputStream().use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }

    private fun copyZipIfOutdated(source: File, destination: File) {
        if (destination.exists() && destination.length() == source.length()) {
            return
        }
        destination.parentFile?.mkdirs()
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun openAssetZip(ctx: Context, code: String): InputStream? {
        val candidates = listOf(
            "bblpacks/$code.zip",
            "files/bblpacks/$code.zip"
        )
        val contexts = listOfNotNull(ctx, instrumentationTestContextOrNull())
        contexts.forEach { assetContext ->
            candidates.forEach { path ->
                runCatching { return assetContext.assets.open(path) }
            }
        }
        return null
    }

    private fun downloadZipIfMissing(code: String, destination: File) {
        if (destination.exists() && destination.length() > 0L) {
            return
        }
        val url = "${DOWNLOADABLE_BIBLE_BASE_URL.trimEnd('/')}/$code.zip"
        runCatching {
            destination.parentFile?.mkdirs()
            java.net.URL(url).openStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun findCanonicalPackDir(): File? {
        val userDir = System.getProperty("user.dir") ?: return null
        var current: File? = File(userDir)
        while (current != null) {
            val candidates = listOf(
                File(current, "resources/bblpacks"),
                File(current, "bbl/resources/bblpacks")
            )
            candidates.firstOrNull { it.isDirectory }?.let { return it }
            current = current.parentFile
        }
        return null
    }

    private fun instrumentationTestContextOrNull(): Context? {
        return runCatching {
            val registry = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            val method = registry.getMethod("getInstrumentation")
            val instrumentation = method.invoke(null) as? android.app.Instrumentation
            instrumentation?.context
        }.getOrNull()
    }

    private companion object {
        @Volatile
        private var resourcesPrepared = false
        private val resourcesLock = Any()
    }
}
