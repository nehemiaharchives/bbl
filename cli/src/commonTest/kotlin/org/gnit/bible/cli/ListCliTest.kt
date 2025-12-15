package org.gnit.bible.cli

import com.github.ajalt.clikt.testing.test
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.getPlatform
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListCliTest : ResourcesTestBase() {

    lateinit var bible: Bible
    private lateinit var systemFileSystem: FileSystem

    @BeforeTest
    fun setup(){
        systemFileSystem = FileSystem.SYSTEM
        val bblPackDir = "/tmp/bbl_kmp_cli_list_cli_test_dir"
        systemFileSystem.createDirectories(bblPackDir.toPath())
        val platform = createTestPlatform().apply { overridePlatformPackDir = bblPackDir }
        val httpClient = HttpClient(TestFixtures.bblInstallMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform, fileSystem = systemFileSystem)
        bible = Bible(assetManager = assetManager)

        Bbl(bible).test("install kttv")
    }

    val expectedTranslationList = """WEBUS  | World English Bible                        | World English Bible              | English    | 2000 | Embedded  | Public Domain
KJV    | King James Version                         | King James Version               | English    | 1611 | Embedded  | Public Domain
RVR09  | Reina-Valera                               | Reina-Valera                     | Spanish    | 1909 | Embedded  | Public Domain
TB     | Brazilian Translation                      | Tradução Brasileira              | Portuguese | 1917 | Embedded  | Public Domain
DELUT  | Luther Bible                               | Lutherbibel                      | German     | 1912 | Embedded  | Public Domain
LSG    | Louis Segond                               | Bible Segond                     | French     | 1910 | Embedded  | Public Domain
SINOD  | Russian Synodal Bible                      | Синодальный перевод              | Russian    | 1876 | Embedded  | Public Domain
SVRJ   | Statenvertaling Jongbloed edition          | Statenvertaling Jongbloed-editie | Dutch      | 1888 | Embedded  | Public Domain
RDV24  | Revised Diodati Version                    | Versione Diodati Riveduta        | Italian    | 1924 | Embedded  | Public Domain
UBG    | Updated Gdansk Bible                       | Uwspółcześniona Biblia gdańska   | Polish     | 2017 | Embedded  | © 2017 Fundacja Wrota Nadziei (Gate of Hope Foundation). Non-commercial use of unaltered text permitted.
UBIO   | Ukrainian Bible, Ivan Ogienko              | Біблія в пер. Івана Огієнка      | Ukrainian  | 1962 | Embedded  | CC BY-SA 4.0 © 1962 Українське Біблійне Товариство / Ukrainian Bible Society
SVEN   | Svenska 1917                               | 1917 års kyrkobibel              | Swedish    | 1917 | Embedded  | Public Domain
CUNP   | Chinese Union Version with New Punctuation | 新標點和合本                     | Chinese    | 1919 | Embedded  | Public Domain
KRV    | Korean Revised Version                     | 개역한글                         | Korean     | 1961 | Embedded  | Public Domain
JC     | Japanese Colloquial Bible                  | 口語訳                           | Japanese   | 1955 | Embedded  | Public Domain
ABTAG  | Ang Biblia                                 | Ang Biblia                       | Tagalog    | 1905 | Available | Public Domain
AYT    | The Opened Bible                           | Alkitab Yang Terbuka             | Indonesian | 2024 | Available | CC BY-NC-SA 4.0 © 2011-2024 YLSA-AYT
KTTV   | Vietnamese Bible 1925                      | Kinh Thánh Tiếng Việt            | Vietnamese | 1925 | Installed | Public Domain
TH1971 | Thai Bible 1925                            | พระคริสตธรรมคัมภีร์ ฉบับ1971          | Thai       | 1971 | Available | Public Domain
IRVHIN | Indian Revised Version - Hindi             | इंडियन रिवाइज्ड वर्जन (IRV) हिंदी         | Hindi      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVBEN | Indian Revised Version - Bengali           | ইন্ডিয়ান রিভাইজড ভার্সন (IRV) - বেঙ্গলী    | Bengali    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVMAR | Indian Revised Version - Marathi           | इंडियन रीवाइज्ड वर्जन (IRV) मराठी        | Marathi    | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVTEL | Indian Revised Version - Telugu            | ఇండియన్ రివైజ్డ్ వెర్షన్ (IRV) - తెలుగు       | Telugu     | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVTAM | Indian Revised Version - Tamil             | இண்டியன் ரிவைஸ்டு வெர்ஸன் (IRV) - தமிழ்      | Tamil      | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVGUJ | Indian Revised Version - Gujarati          | ઇન્ડિયન રીવાઇઝ્ડ વર્ઝન ગુજરાતી            | Gujarati   | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
IRVURD | Indian Revised Version - Urdu              | इंडियन रिवाइज्ड वर्जन (IRV) उर्दू        | Urdu       | 2019 | Available | CC BY-SA 4.0 © 2019 Bridge Connectivity Solutions Pvt. Ltd.
"""


    @Test
    fun testBblList() {
        val bbl = Bbl(bible = bible)

        val packDir = bible.assetManager.platform.packDir.toPath()
        val packFiles = systemFileSystem.list(packDir)
        assertEquals(1, packFiles.size)
        assertEquals("kttv.zip", packFiles.first().name)
        val zipPath = packDir / "kttv.zip".toPath()
        assertTrue(systemFileSystem.exists(zipPath))

        arrayOf("bible", "bibles", "translation", "translations", "version", "versions").map { target -> "list $target" }.plus("list").forEach { argv ->
            assertEquals(expectedTranslationList, bbl.test(argv = argv).stdout)
        }
    }

    @Test
    fun testBblListBooks(){
        val bbl = Bbl(bible = bible)

        arrayOf("list book", "list books").forEach { argv ->
            val out = bbl.test(argv).stdout
            assertEquals(66, out.lines().filter { it.isNotBlank() }.size)
            assertTrue(out.contains("genesis"))
            assertTrue(out.contains("revelation"))
        }
    }

    @Test
    fun testBblListCategories() {
        val bbl = Bbl(bible = bible)

        arrayOf("list category", "list categories").forEach { argv ->
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
}
