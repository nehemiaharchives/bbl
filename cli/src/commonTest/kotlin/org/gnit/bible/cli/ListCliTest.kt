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
import org.gnit.bible.ConfigKey
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.BeforeTest
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
        bible.assetManager.platform.settings.remove(ConfigKey.BORDER.value)

        Bbl(bible).test("install kttv")
    }

    val expectedTranslationList = """ Code    English Name                             Language    Year Status      
 WEBUS   World English Bible                      English     2000 Embedded    
 KJV     King James Version                       English     1611 Embedded    
 RVR09   Reina-Valera                             Spanish     1909 Embedded    
 TB      Brazilian Translation                    Portuguese  1917 Embedded    
 DELUT   Luther Bible                             German      1912 Embedded    
 LSG     Louis Segond                             French      1910 Embedded    
 SINOD   Russian Synodal Bible                    Russian     1876 Embedded    
 SVRJ    Statenvertaling Jongbloed edition        Dutch       1888 Embedded    
 RDV24   Revised Diodati Version                  Italian     1924 Embedded    
 UBG     Updated Gdansk Bible                     Polish      2017 Embedded    
 UBIO    Ukrainian Bible, Ivan Ogienko            Ukrainian   1962 Embedded    
 SVEN    Svenska 1917                             Swedish     1917 Embedded    
 CUNP    Chinese Union Version with New Punctuati Chinese     1919 Embedded    
 KRV     Korean Revised Version                   Korean      1961 Embedded    
 JC      Japanese Colloquial Bible                Japanese    1955 Embedded    
 KTTV    Vietnamese Bible 1925                    Vietnamese  1925 Installed   
 ABTAG   Ang Biblia                               Tagalog     1905 To Download 
 AYT     The Opened Bible                         Indonesian  2024 To Download 
 IRVBEN  Indian Revised Version - Bengali         Bengali     2019 To Download 
 IRVGUJ  Indian Revised Version - Gujarati        Gujarati    2019 To Download 
 IRVHIN  Indian Revised Version - Hindi           Hindi       2019 To Download 
 IRVMAR  Indian Revised Version - Marathi         Marathi     2019 To Download 
 IRVTAM  Indian Revised Version - Tamil           Tamil       2019 To Download 
 IRVTEL  Indian Revised Version - Telugu          Telugu      2019 To Download 
 IRVURD  Indian Revised Version - Urdu            Urdu        2019 To Download 
 TH1971  Thai Bible 1925                          Thai        1971 To Download 
"""

    val expectedTranslationListWithBorder = """
            ┌───────┬──────────────────────────────────────┬───────────┬─────┬────────────┐
            │ Code  │ English Name                         │ Language  │ Year│ Status     │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ WEBUS │ World English Bible                  │ English   │ 2000│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ KJV   │ King James Version                   │ English   │ 1611│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ RVR09 │ Reina-Valera                         │ Spanish   │ 1909│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ TB    │ Brazilian Translation                │ Portuguese│ 1917│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ DELUT │ Luther Bible                         │ German    │ 1912│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ LSG   │ Louis Segond                         │ French    │ 1910│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ SINOD │ Russian Synodal Bible                │ Russian   │ 1876│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ SVRJ  │ Statenvertaling Jongbloed edition    │ Dutch     │ 1888│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ RDV24 │ Revised Diodati Version              │ Italian   │ 1924│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ UBG   │ Updated Gdansk Bible                 │ Polish    │ 2017│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ UBIO  │ Ukrainian Bible, Ivan Ogienko        │ Ukrainian │ 1962│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ SVEN  │ Svenska 1917                         │ Swedish   │ 1917│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ CUNP  │ Chinese Union Version with New Punctu│ Chinese   │ 1919│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ KRV   │ Korean Revised Version               │ Korean    │ 1961│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ JC    │ Japanese Colloquial Bible            │ Japanese  │ 1955│ Embedded   │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ KTTV  │ Vietnamese Bible 1925                │ Vietnamese│ 1925│ Installed  │
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ ABTAG │ Ang Biblia                           │ Tagalog   │ 1905│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ AYT   │ The Opened Bible                     │ Indonesian│ 2024│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVBEN│ Indian Revised Version - Bengali     │ Bengali   │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVGUJ│ Indian Revised Version - Gujarati    │ Gujarati  │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVHIN│ Indian Revised Version - Hindi       │ Hindi     │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVMAR│ Indian Revised Version - Marathi     │ Marathi   │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVTAM│ Indian Revised Version - Tamil       │ Tamil     │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVTEL│ Indian Revised Version - Telugu      │ Telugu    │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ IRVURD│ Indian Revised Version - Urdu        │ Urdu      │ 2019│ To Download│
            ├───────┼──────────────────────────────────────┼───────────┼─────┼────────────┤
            │ TH1971│ Thai Bible 1925                      │ Thai      │ 1971│ To Download│
            └───────┴──────────────────────────────────────┴───────────┴─────┴────────────┘
            
        """.trimIndent()

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

        bible.assetManager.platform.settings[ConfigKey.BORDER.value] = "true"
        arrayOf("bible", "bibles", "translation", "translations", "version", "versions").map { target -> "list $target" }.plus("list").forEach { argv ->
            assertEquals(expectedTranslationListWithBorder, bbl.test(argv = argv).stdout)
        }
    }

    @Test
    fun testBblListBooks(){
        val bbl = Bbl(bible = bible)

        arrayOf("list book", "list books").forEach { argv ->
            val lines = bbl.test(argv).stdout.lines().filter { it.isNotBlank() }
            assertEquals(66, lines.size)
            assertEquals("genesis, gen, ge, gn", lines.first())
            assertEquals("revelation, rev, re, the revelation", lines.last())
        }
    }

    @Test
    fun testBblListCategories() {
        val bbl = Bbl(bible = bible)

        arrayOf("list category", "list categories").forEach { argv ->
            val lines = bbl.test(argv).stdout.lines().filter { it.isNotBlank() }
            assertEquals(Books.Category.entries.size - 1, lines.size)
            assertTrue(lines.none { it.startsWith("ALL:") })

            assertEquals("OLD_TESTAMENT: ot, old testament", lines.first())
            assertTrue(lines.contains("NEW_TESTAMENT: nt, new testament"))
        }
    }
}
