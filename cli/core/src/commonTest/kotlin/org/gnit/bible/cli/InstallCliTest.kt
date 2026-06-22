package org.gnit.bible.cli

import org.gnit.bible.SupportedTranslation

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.BblVersion
import org.gnit.bible.InMemorySettings
import org.gnit.bible.Platform
import org.gnit.bible.MANIFEST_JSON_POSTFIX
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.ZipUtil
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var fakeFs: FakeFileSystem
    private lateinit var platform: Platform
    private var originalPackDir: String? = null
    private var originalFileSystem: okio.FileSystem? = null
    private var originalSettings: com.russhwolf.settings.Settings? = null

    @BeforeTest
    fun setup(){

        fakeFs = FakeFileSystem()
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        platform.overrideFileSystem = fakeFs
        platform.overridePlatformPackDir = "/tmp/bbl_cli_install_test_dir"
        platform.overrideSettings = InMemorySettings()
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine())
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        bible = Bible(assetManager = assetManager)
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
    }

    @Test
    fun testBblInstallKttv() {
        val result = Bbl(bible = bible).test("install kttv").output
        assertInstallResult(result, listOf("kttv"), listOf(searchHelperName("extra")))
    }

    @Test
    fun testBblInstallWebusDownloadsPlatformReleaseAssetAndRenamesItLocally() {
        val requestedPaths = mutableListOf<String>()
        val localBinaryName = searchHelperName("common")
        val releaseAssetName = platform.releaseAssetName(localBinaryName)
        val httpClient = HttpClient(MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when (request.url.encodedPath.substringAfterLast('/')) {
                "webus.zip" -> respond(
                    content = TestFixtures.webusMinimalZipBytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(TestFixtures.webusMinimalZipBytes.size.toString())
                    )
                )

                releaseAssetName -> {
                    val bytes = "common helper".encodeToByteArray()
                    respond(
                        content = bytes,
                        headers = headersOf(
                            "Content-Type" to listOf("application/octet-stream"),
                            "Content-Length" to listOf(bytes.size.toString())
                        )
                    )
                }

                else -> error("Unexpected request for path: ${request.url.encodedPath}")
            }
        })
        val versionedBible = Bible(
            assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        )

        val result = Bbl(bible = versionedBible).test("install webus")

        assertEquals("Installed webus\nInstalled $localBinaryName\n", result.output.replace("\r\n", "\n"))
        assertTrue(requestedPaths.any { it.endsWith("/$releaseAssetName") })
        val binDir = CliBinaryPaths.binDir(platform.packDir)
        assertTrue(fakeFs.exists(binDir / localBinaryName))
        assertTrue(!fakeFs.exists(binDir / releaseAssetName))
    }

    @Test
    fun testBblInstallRecordsHistoryWhenHistoryEnabled() {
        val result = Bbl(bible = bible).test("install kttv")

        assertEquals(0, result.statusCode, "Command should succeed. stderr=${result.stderr}")
        assertEquals(listOf("bbl install kttv"), BblHistory.read(bible).map { it.command })
    }

    @Test
    fun testBblAliasGetKttv(){
        val result = Bbl(bible = bible).test("get kttv").output
        assertInstallResult(result, listOf("kttv"), listOf(searchHelperName("extra")))
    }

    @Test
    fun testBblAliasPullKttv(){
        val result = Bbl(bible = bible).test("pull kttv").output
        assertInstallResult(result, listOf("kttv"), listOf(searchHelperName("extra")))
    }

    @Test
    fun testBblInstallMultipleTranslations() {
        val extraSearchHelperName = searchHelperName("extra")
        val commonSearchHelperName = searchHelperName("common")
        val result = Bbl(bible = bible).test("install kttv th1971").output
        assertInstallResult(
            result = result,
            expectedCodes = listOf("kttv", "th1971"),
            expectedSearchBinaries = listOf(extraSearchHelperName, commonSearchHelperName),
            expectedOutputLines = listOf(
                "Installed kttv",
                "Installed $extraSearchHelperName",
                "Installed th1971",
                "Installed $commonSearchHelperName",
            )
        )
    }

    @Test
    fun testBblInstallJcInstallsKuromojiSearchBinary() {
        val searchHelperName = searchHelperName("kuromoji")
        val result = Bbl(bible = bible).test("install jc").output.replace("\r\n", "\n")

        assertEquals("Installed jc\nInstalled $searchHelperName\n", result)
        assertInstalledPack("jc")
        assertInstalledSearchBinary(searchHelperName)
    }

    @Test
    fun testBblInstallFallsBackToLegacyRepositoryWhenPrimaryRepositoryUnavailable() {
        val searchHelperName = searchHelperName("kuromoji")
        val releaseAssetName = platform.releaseAssetName(searchHelperName)
        val primaryReleasePath = "/nehemiaharchives/bbl/releases/download/${BblVersion.VERSION}/$releaseAssetName"
        val legacyReleasePath = "/nehemiaharchives/bbl-kmp/releases/download/${BblVersion.VERSION}/$releaseAssetName"
        val primaryPackPath = "/nehemiaharchives/bbl/releases/download/${BblVersion.VERSION}/jc.zip"
        val legacyPackPath = "/nehemiaharchives/bbl-kmp/releases/download/${BblVersion.VERSION}/jc.zip"
        val httpClient = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath == primaryPackPath ->
                    respond("", status = HttpStatusCode.NotFound)

                request.url.encodedPath == legacyPackPath ->
                    respond(
                        content = TestFixtures.jcMinimalZipBytes,
                        headers = headersOf(
                            "Content-Type" to listOf("application/zip"),
                            "Content-Length" to listOf(TestFixtures.jcMinimalZipBytes.size.toString())
                        )
                    )

                request.url.encodedPath == primaryReleasePath ->
                    respond("", status = HttpStatusCode.NotFound)

                request.url.encodedPath == legacyReleasePath -> {
                    val bytes = "kuromoji helper".encodeToByteArray()
                    respond(
                        content = bytes,
                        headers = headersOf(
                            "Content-Type" to listOf("application/octet-stream"),
                            "Content-Length" to listOf(bytes.size.toString())
                        )
                    )
                }

                else -> error("Unexpected request for path: ${request.url.encodedPath}")
            }
        })
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        val versionedBible = Bible(assetManager = assetManager)
        val result = Bbl(bible = versionedBible).test("install jc").output.replace("\r\n", "\n")

        assertEquals("Installed jc\nInstalled $searchHelperName\n", result)
        assertInstalledPack("jc")
        assertInstalledSearchBinary(searchHelperName)
    }

    @Test
    fun testBblInstallUsesBuiltInCatalogWhenFetchedListOmitsEmbeddedTranslation() {
        val primaryReleasePath =
            "/nehemiaharchives/bbl/releases/download/${BblVersion.VERSION}/" +
                platform.releaseAssetName(searchHelperName("kuromoji"))
        val httpClient = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath ==
                    "/nehemiaharchives/bbl/releases/download/${BblVersion.VERSION}/jc.zip" -> respond(
                    content = TestFixtures.jcMinimalZipBytes,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(TestFixtures.jcMinimalZipBytes.size.toString())
                    )
                )

                request.url.encodedPath == primaryReleasePath -> {
                    val bytes = "kuromoji helper".encodeToByteArray()
                    respond(
                        content = bytes,
                        headers = headersOf(
                            "Content-Type" to listOf("application/octet-stream"),
                            "Content-Length" to listOf(bytes.size.toString())
                        )
                    )
                }

                else -> error("Unexpected request for path: ${request.url.encodedPath}")
            }
        })
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        val versionedBible = Bible(assetManager = assetManager)
        val result = Bbl(bible = versionedBible).test("install jc").output.replace("\r\n", "\n")

        assertEquals("Installed jc\nInstalled ${searchHelperName("kuromoji")}\n", result)
        val packPath = versionedBible.assetManager.platform.packDir.toPath() / "jc.zip"
        val helperPath = versionedBible.assetManager.platform.packDir.toPath().parent!! / "bin" / searchHelperName("kuromoji")
        assertTrue(fakeFs.exists(packPath))
        assertTrue(fakeFs.exists(helperPath))
    }

    @Test
    fun testBblInstallSkipsExistingSharedSearchBinaryDependency() {
        val searchHelperName = searchHelperName("morfologik")
        val first = Bbl(bible = bible).test("install ubio").output.replace("\r\n", "\n")
        assertEquals("Installed ubio\nInstalled $searchHelperName\n", first)
        assertInstalledPack("ubio")
        assertInstalledSearchBinary(searchHelperName)

        val second = Bbl(bible = bible).test("install ubg").output.replace("\r\n", "\n")
        assertEquals("Installed ubg\n$searchHelperName already installed, skipping download\n", second)
        assertInstalledPack("ubg")
        assertInstalledSearchBinary(searchHelperName)
    }

    @Test
    fun testBblInstallReinstallsExistingIncompatiblePack() {
        val packDir = bible.assetManager.platform.packDir.toPath()
        fakeFs.createDirectories(packDir)
        val wrongVersionZip = ZipUtil.buildMinimalZip(
            listOf("kttv$MANIFEST_JSON_POSTFIX" to SupportedTranslation.KTTV.translation.copy(version = "0.0.1").toJson())
        )
        fakeFs.write(packDir / "kttv.zip") { write(wrongVersionZip) }

        val result = Bbl(bible = bible).test("install kttv").output.replace("\r\n", "\n")

        assertEquals(
            "kttv installed pack is incompatible with bbl ${BblVersion.VERSION}, reinstalling\n" +
                "Installed kttv\n" +
                "Installed ${searchHelperName("extra")}\n",
            result
        )
        assertInstalledPack("kttv")
    }

    @Test
    fun testBblInstallFailsWhenPackManifestVersionMismatches() {
        val downloadableTranslationsJson = """
            [
              {
                "code": "jc",
                "languageCode": "ja",
                "englishName": "Japanese Colloquial Bible",
                "nativeName": "口語訳",
                "year": 1955,
                "copyright": "Public Domain"
              }
            ]
        """.trimIndent()
        val wrongVersionManifest = SupportedTranslation.JC.translation.copy(version = "0.0.1").toJson()
        val wrongVersionZip = ZipUtil.buildMinimalZip(
            listOf(
                "jc.1.1.txt" to TestFixtures.JC_GENESIS_1_1,
                "jc.43.3.txt" to TestFixtures.john3Jc,
                "jc.40.28.txt" to TestFixtures.matthew28Jc,
                "jc.0.manifest.json" to wrongVersionManifest,
                "index/jc.index.manifest" to "_0.cfs",
                "index/_0.cfs" to "CODEC"
            )
        )
        val httpClient = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath == "${BblVersion.SERVER_RESOURCE_PATH}/bbllist.json" -> respond(
                    content = downloadableTranslationsJson,
                    headers = headersOf(
                        "Content-Type" to listOf("application/json"),
                        "Content-Length" to listOf(downloadableTranslationsJson.encodeToByteArray().size.toString())
                    )
                )

                request.url.encodedPath ==
                    "/nehemiaharchives/bbl/releases/download/${BblVersion.VERSION}/jc.zip" -> respond(
                    content = wrongVersionZip,
                    headers = headersOf(
                        "Content-Type" to listOf("application/zip"),
                        "Content-Length" to listOf(wrongVersionZip.size.toString())
                    )
                )

                else -> error("Unexpected request for path: ${request.url.encodedPath}")
            }
        })
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = fakeFs)
        val versionedBible = Bible(assetManager = assetManager)

        val result = Bbl(bible = versionedBible).test("install jc")

        assertTrue(result.statusCode != 0)
        assertTrue(result.output.contains("pack manifest version 0.0.1 is incompatible with bbl ${BblVersion.VERSION}"))
        val zipPath = versionedBible.assetManager.platform.packDir.toPath() / "jc.zip"
        assertTrue(!fakeFs.exists(zipPath))
    }

    private fun assertInstallResult(
        result: String,
        expectedCodes: List<String>,
        expectedSearchBinaries: List<String> = emptyList(),
        expectedOutputLines: List<String> = expectedCodes.map { "Installed $it" } +
                expectedSearchBinaries.map { "Installed $it" }
    ){
        val expectedOutput = expectedOutputLines.joinToString(separator = "\n", postfix = "\n")
        assertEquals(expectedOutput, result.replace("\r\n", "\n"))

        val packDir = bible.assetManager.platform.packDir.toPath()
        val packFileList = fakeFs.list(packDir).filter { it.name.endsWith(".zip") }
        assertEquals(expectedCodes.size, packFileList.size)
        val actualNames = packFileList.map { it.name }.sorted()
        val expectedNames = expectedCodes.map { "$it.zip" }.sorted()
        assertEquals(expectedNames, actualNames)

        expectedCodes.forEach { code ->
            assertInstalledPack(code)
        }
        expectedSearchBinaries.forEach { name ->
            assertInstalledSearchBinary(name)
        }
    }

    private fun assertInstalledPack(code: String) {
        val zipPath = bible.assetManager.platform.packDir.toPath() / "$code.zip".toPath()
        assertTrue(fakeFs.exists(zipPath))
        fakeFs.metadata(zipPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }

    private fun assertInstalledSearchBinary(name: String) {
        val binPath = bible.assetManager.platform.packDir.toPath().parent!! / "bin".toPath() / name
        assertTrue(fakeFs.exists(binPath))
        fakeFs.metadata(binPath).also { metadata ->
            assertTrue(metadata.isRegularFile)
            assertTrue((metadata.size ?: 0L) > 0L)
        }
    }

    private fun searchHelperName(moduleId: String): String {
        val executableSuffix = if (platform.name == "Windows") ".exe" else ""
        return "bbl-search-$moduleId$executableSuffix"
    }

    @Ignore //Integration test: touches real ~/.bbl/packs and uses network
    @Test
    fun testBblInstallInProductionEnv(){
        val command = Bbl()
        val result = command.test("install ayt")
        println(result.stdout)
    }
}
