package org.gnit.bible.cli

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import org.gnit.bible.AssetManager
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.Translation
import org.gnit.bible.downloadableTranslations
import org.gnit.bible.getPlatform
import org.gnit.bible.test.ResourcesTestBase
import org.gnit.bible.test.TestFixtures
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ListCliTest : ResourcesTestBase() {

    @Test
    fun testBblList() {
        val platform = createTestPlatform()
        val httpClient = HttpClient(TestFixtures.downloadableTranslationsListMockEngine)
        val assetManager = AssetManagerImpl(httpClient = httpClient, platform = platform)
        val bible = Bible(assetManager = assetManager)
        val result = Bbl(bible = bible).test(argv = "list")
        val expected = """
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
        assertEquals(expected, result.stdout)
    }
}
