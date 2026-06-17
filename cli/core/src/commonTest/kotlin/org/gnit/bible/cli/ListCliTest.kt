package org.gnit.bible.cli

import org.gnit.bible.SupportedTranslation

import okio.SYSTEM
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.InMemorySettings
import org.gnit.bible.Platform
import org.gnit.bible.Translation
import org.gnit.bible.test.ResourcesTestBase
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var platform: Platform
    private var originalPackDir: String? = null
    private var originalFileSystem: okio.FileSystem? = null
    private var originalSettings: com.russhwolf.settings.Settings? = null

    @BeforeTest
    fun setup(){
        platform = createTestPlatform()
        originalPackDir = platform.overridePlatformPackDir
        originalFileSystem = platform.overrideFileSystem
        originalSettings = platform.overrideSettings
        platform.overridePlatformPackDir = "/tmp/bbl_cli_list_cli_test_dir"
        platform.overrideFileSystem = null
        platform.overrideSettings = InMemorySettings()

        val installed = SupportedTranslation.KTTV.translation

        bible = Bible(
            assetManager = FakeAssetManager(
                platform = platform,
                downloaded = listOf(installed)
            )
        )
    }

    @AfterTest
    fun restorePlatformOverrides() {
        platform.settings.clear()
        platform.overridePlatformPackDir = originalPackDir
        platform.overrideFileSystem = originalFileSystem
        platform.overrideSettings = originalSettings
    }

    val expectedTranslationList = """WEBUS  | World English Bible                        | World English Bible              | English    | 2000 | Available | Public Domain
KJV    | King James Version                         | King James Version               | English    | 1611 | Available | Public Domain
RVR09  | Reina-Valera                               | Reina-Valera                     | Spanish    | 1909 | Available | Public Domain
TB     | Brazilian Translation                      | Tradução Brasileira              | Portuguese | 1917 | Available | Public Domain
DELUT  | Luther Bible                               | Lutherbibel                      | German     | 1912 | Available | Public Domain
LSG    | Louis Segond                               | Bible Segond                     | French     | 1910 | Available | Public Domain
SINOD  | Russian Synodal Bible                      | Синодальный перевод              | Russian    | 1876 | Available | Public Domain
SVRJ   | Statenvertaling Jongbloed edition          | Statenvertaling Jongbloed-editie | Dutch      | 1888 | Available | Public Domain
RDV24  | Revised Diodati Version                    | Versione Diodati Riveduta        | Italian    | 1924 | Available | Public Domain
UBG    | Updated Gdansk Bible                       | Uwspółcześniona Biblia gdańska   | Polish     | 2017 | Available | © 2017 Fundacja Wrota Nadziei (Non-commercial & unaltered text)
UBIO   | Ukrainian Bible, Ivan Ogienko              | Біблія в пер. Івана Огієнка      | Ukrainian  | 1962 | Available | CC BY-SA 4.0 © 1962 Українське Біблійне Товариство
SVEN   | Svenska 1917                               | 1917 års kyrkobibel              | Swedish    | 1917 | Available | Public Domain
CUNP   | Chinese Union Version with New Punctuation | 新標點和合本                     | Chinese    | 1919 | Available | Public Domain
KRV    | Korean Revised Version                     | 개역한글                         | Korean     | 1961 | Available | Public Domain
JC     | Japanese Colloquial Bible                  | 口語訳                           | Japanese   | 1955 | Available | Public Domain
ABTAG  | Ang Biblia                                 | Ang Biblia                       | Tagalog    | 1905 | Available | Public Domain
AYT    | The Opened Bible                           | Alkitab Yang Terbuka             | Indonesian | 2024 | Available | CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT
KTTV   | Vietnamese Bible 1925                      | Kinh Thánh Tiếng Việt            | Vietnamese | 1925 | Installed | Public Domain
TH1971 | Thai Bible 1925                            | พระคริสตธรรมคัมภีร์ ฉบับ1971          | Thai       | 1971 | Available | Public Domain
IRVHIN | Indian Revised Version - Hindi             | इंडियन रिवाइज्ड वर्जन (IRV) हिंदी    | Hindi      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVBEN | Indian Revised Version - Bengali           | ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী| Bengali    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVMAR | Indian Revised Version - Marathi           | इंडियन रीवाइज्ड वर्जन (IRV) मराठी   | Marathi    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVTEL | Indian Revised Version - Telugu            | ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు    | Telugu     | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVTAM | Indian Revised Version - Tamil             | இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ் | Tamil      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVGUJ | Indian Revised Version - Gujarati          | ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી        | Gujarati   | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVURD | Indian Revised Version - Urdu              | इंडियन रिवाइज्ड वर्जन (IRV) उर्दू     | Urdu       | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
NPIULB | Nepali language, Unlocked Literal Bible    | पवित्र बाइबल                      | Nepali     | 2019 | Available | CC BY-SA 4.0 © 2019 Door43 World Missions Community
"""

    @Test
    fun testBblList() {
        val bbl = Bbl(bible = bible)

        val targets = arrayOf("bible", "bibles", "translation", "translations", "version", "versions")
        val commands = targets.map { "list $it" } + targets.map { "ls $it" } + listOf("list", "ls")
        commands.forEach { argv ->
            assertEquals(expectedTranslationList, bbl.test(argv = argv).stdout)
        }
    }

    @Test
    fun testBblListBooks(){
        val bbl = Bbl(bible = bible)

        arrayOf("list book", "list books", "ls book", "ls books").forEach { argv ->
            val out = bbl.test(argv).stdout
            assertEquals(66, out.lines().filter { it.isNotBlank() }.size)
            assertTrue(out.contains("genesis"))
            assertTrue(out.contains("revelation"))
        }
    }

    @Test
    fun testBblListCategories() {
        val bbl = Bbl(bible = bible)

        arrayOf("list category", "list categories", "ls category", "ls categories").forEach { argv ->
            val out = bbl.test(argv).stdout
            val lines = out.lines().filter { it.isNotBlank() }
            assertEquals(Books.Category.entries.size - 1, lines.size)
            assertTrue(out.contains("OLD_TESTAMENT"))
            assertTrue(out.contains("NEW_TESTAMENT"))
            assertTrue(!out.contains("ALL"))
        }
    }

    @Test
    @Ignore
    fun testBblListTranslationsInProductionEnv(){
        val command = Bbl()
        val result = command.test("list translation")
        println(result.stdout)
    }

    @Test
    @Ignore
    fun testBblListBooksInProductionEnv(){
        val command = Bbl()
        val result = command.test("list books")
        println(result.stdout)
    }

    @Test
    @Ignore
    fun testBblListCategoryInProductionEnv(){
        val command = Bbl()
        val result = command.test("list category")
        println(result.stdout)
    }

    private class FakeAssetManager(
        override val platform: Platform,
        private val downloaded: List<Translation>
    ) : org.gnit.bible.AssetManager {

        override val fileSystem: okio.FileSystem = okio.FileSystem.SYSTEM

        override suspend fun download(baseUrl: String, fileName: String) {
            // No-op for list-only tests.
        }

        override suspend fun downloadTo(baseUrl: String, fileName: String, destinationDir: String) {
            // No-op for list-only tests.
        }

        override fun downloadedTranslationCodes(): List<String> = downloaded.map { it.code }

        override fun downloadedTranslations(): List<Translation> = downloaded

        override fun delete(translationCode: String) {
            // No-op for list-only tests.
        }
    }
}
