package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallCliTest : ResourcesTestBase() {

    @Test
    fun testBblInstallKttv() {
        val platform = createTestPlatform().apply { overridePlatformPackDir = "/tmp/bblpack" }
        val httpClient = HttpClient(TestFixtures.kttvDownloadingMockEngine)
        val fakeFs = FakeFileSystem()
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        val bible = Bible(assetManager = assetManager)
        val result = Bbl(bible = bible).test("install kttv")
        assertEquals("Installed kttv\n", result.stdout)
        val zipPath = "/tmp/bblpack/kttv.zip".toPath()
        assertTrue(fakeFs.exists(zipPath))
        fakeFs.metadata(zipPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }
}
