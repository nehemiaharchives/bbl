package org.gnit.bible.test

import okio.Path.Companion.toPath
import org.gnit.bible.AnalyzerProvider
import org.gnit.bible.AssetManagerImpl
import org.gnit.bible.Bible
import org.gnit.bible.SearchModule
import org.gnit.bible.SupportedTranslation
import org.gnit.bible.getPlatform
import org.gnit.bible.test.search.person.NTGospelsPersonTest

/**
 * When you add new downloadable bible for testing search, add bbl zip file to `resources/bblpacks`
 */
interface SearchTestBase {
    var bible: Bible

    val translationsToBeTested: List<SupportedTranslation>
        get() = SupportedTranslation.entries.toList()

    fun searchTest() {
        runSearchTests(translationsToBeTested)
    }

    fun searchCommonEmbedded() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.COMMON && it.embedded }
        )
    }

    fun searchCommonDownloaded() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.COMMON && !it.embedded }
        )
    }

    fun searchMorfologik() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.MORFOLOGIK }
        )
    }

    fun searchSmartcn() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.SMARTCN }
        )
    }

    fun searchNori() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.NORI }
        )
    }

    fun searchKuromoji() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.KUROMOJI }
        )
    }

    fun searchExtra() {
        runSearchTests(
            SupportedTranslation.entries.filter { it.searchModule == SearchModule.EXTRA }
        )
    }

    /**
     * Inside of this function we aim to cover:
     * all books Categories (OTPentateuch, OTHistorical, OTPoetry, OTMajorProphets, OTMinorProphets, NTGospels, NTHistorical, NTPaulineEpistles, NTGeneralEpistles, NTApocalyptic)
     * all search Subjects (Person, Place, Event, Object, Concept)
     * all SupportedTranslation
     *
     * As we add a term to the test, `lucene-kmp` problems may arise, and we will go back to `lucene-kmp` to fix each time.
     * Especially problems in the language specific biblical terms in each `Bible{$languageName}Analyzer`
     * so that any Bible specific search term cast by end users will return expected Bible versers which makes sense to the general Christian public.
     *
     * This way we dogfood the `lucene-kmp` analyzers and develop both `lucene-kmp` and `bbl` together.
     */
    fun runSearchTests(translationsToBeTested: List<SupportedTranslation>) {
        //OTPentateuchPersonTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        //OTPentateuchPlaceTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        //OTPentateuchEventTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        //OTPentateuchObjectTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        //OTPentateuchConceptTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        // ...
        NTGospelsPersonTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        //NTGospelsPlaceTest(bible = bible, translationsToBeTested = translationsToBeTested,).runAllTests()
        // ...
    }
}

open class CliSearchTestBase(private val analyzerProvider: AnalyzerProvider) : SearchTestBase {

    override lateinit var bible: Bible

    open fun setup() {
        val platform = getPlatform()
        platform.overridePlatformPackDir = if (platform.fileSystem.exists("resources/bblpacks/".toPath())) {
            "resources/bblpacks/"
        } else if (platform.fileSystem.exists("bbl/resources/bblpacks/".toPath())) {
            "bbl/resources/bblpacks/"
        } else {
            "../../../resources/bblpacks/"
        }

        val am = AssetManagerImpl(platform = platform)

        bible = Bible(am, analyzerProvider)
    }
}
