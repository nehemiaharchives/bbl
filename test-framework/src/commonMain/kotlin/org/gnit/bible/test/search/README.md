# org.gnit.bible.test.search

This package includes bbl's search test cases organized by search types. 

Search types are organized by `Subject` of Bible Specific words such as "Person", "Place", "Event", "Object", "Concept", etc.

## sub packages
- `org.gnit.bible.test.search.person` - test cases for searching people in the Bible
- `org.gnit.bible.test.search.place` - test cases for searching places in the Bible
- `org.gnit.bible.test.search.event` - test cases for searching events in the Bible
- `org.gnit.bible.test.search.object` - test cases for searching objects in the Bible
- `org.gnit.bible.test.search.concept` - test cases for searching concepts in the Bible

The packages can be empty but for the git purpose needs to contain a README.md file at least.

### Naming Conventions

`[OT/NT][Category][Subject]Test` is the convention to name specific test file.

`OTPentateuchPersonTest.kt`
`OTPentateuchPlaceTest.kt`
`OTPentateuchEventTest.kt`
`OTPentateuchObjectTest.kt`
`OTPentateuchConceptTest.kt`

where in `org.gnit.bible.test.search.person` following test files are expected to be created:
- `OTPentateuchPersonTest.kt`
- `OTHistoricalPersonTest.kt`
- `OTPoetryPersonTest.kt`
- `OTMajorProfetsPersonTest.kt`
- `OTMinorProfetsPersonTest.kt`
...
- `NTApocalypticPersonTest.kt`
which consists one file per Bible category in a package. This avoids too many files and lets one category test share assertions for books that naturally contain similar words, such as Matthew, Mark, Luke, and John.

## Books Category
`core/src/commonMain/kotlin/org/gnit/bible/Books.kt` contains `enum Category`
The test names starts from `[OT/NT][Category]` prefix. Which is following:
- `OTPentateuch` - Old Testament Pentateuch including Genesis, Exodus, Leviticus, Numbers, Deuteronomy
- `OTHistorical` - Old Testament Historical Books including Joshua, Judges, Ruth, 1 Samuel, 2 Samuel, 1 Kings, 2 Kings, 1 Chronicles, 2 Chronicles, Ezra, Nehemiah, Esther
- `OTPoetry` - Old Testament Poetry Books including Job, Psalms, Proverbs, Ecclesiastes, Song of Solomon
- `OTMajorProfets` - Old Testament Prophecy Books including Isaiah, Jeremiah, Lamentations, Ezekiel, Daniel
- `OTMinorProfets` - Old Testament Prophecy Books including Hosea, Joel, Amos, Obadiah, Jonah, Micah, Nahum, Habakkuk, Zephaniah, Haggai, Zechariah, Malachi
- `NTGospels` - New Testament Gospels including Matthew, Mark, Luke, John
- `NTHistorical` - New Testament Historical Books including Acts
- `NTPaulineEpistles` - New Testament Pauline Epistles including Romans, 1 Corinthians, 2 Corinthians, Galatians, Ephesians, Philippians, Colossians, 1 Thessalonians, 2 Thessalonians, 1 Timothy, 2 Timothy, Titus, Philemon
- `NTGeneralEpistles` - New Testament General Epistles including Hebrews, James, 1 Peter, 2 Peter, 1 John, 2 John, 3 John, Jude
- `NTApocalyptic` - New Testament Apocalyptic Books including Revelation

## Translations
The tests assertions needs to be written for all available officially supported translations.
`core/src/commonMain/kotlin/org/gnit/bible/SupportedTranslation.kt` contains `enum class SupportedTranslation`, which is the compiler-visible source of truth for official translations supported by bbl.
`Translation` remains the runtime and manifest data class because end users can generate their own bbl packs as user translations that are not officially supported by this test framework.

Each subject test function should use an exhaustive `when (supportedTranslation)` over `SupportedTranslation`, with no `else`.
This makes the compiler force us to add test cases whenever a new supported translation is added.

`SearchTestBase` remains the high-level test skeleton. It owns `bible`, `translationsToBeTested`, and delegates to category subject test classes:

```kotlin
interface SearchTestBase {
    var bible: Bible
    val translationsToBeTested: List<SupportedTranslation>

    fun searchTest() {
        NTGospelsPersonTest(
            bible = bible,
            translationsToBeTested = translationsToBeTested,
        ).runAllTests()
    }
}
```

Subject test classes keep the detailed assertions:

```kotlin
class NTGospelsPersonTest(
    private val bible: Bible,
    private val translationsToBeTested: List<SupportedTranslation>,
) {
    fun runAllTests() {
        searchJesusChrist()
    }

    fun searchJesusChrist() {
        translationsToBeTested.forEach { supportedTranslation ->
            when (supportedTranslation) {
                SupportedTranslation.WEBUS -> {
                    val webus = supportedTranslation.translation
                    listOf("Jesus Christ", "Jesus", "Christ").forEach { enTerm ->
                        val actualWebus = bible.search(term = enTerm, translation = webus).first()
                        assertEquals(VersePointer(webus, 40, 1, 1), actualWebus, "Failed on searching: $enTerm")
                    }
                }
                SupportedTranslation.JC -> {
                    // Japanese JC assertions
                }
            }
        }
    }
}
```

## Person
The `org.gnit.bible.test.search.person` package aim to systematically cover all of approximately 3,237 people appear in the Bible.
However, there are our expendable tokens for AI Agents are limited to let them work, so we will split bible by category and book types, then prioritize the most important and frequently mentioned people in the Bible.
A person falls into one category of when first mentioned in the Bible. 

Following examples explains how a test file contains a person:

- `OTPentateuchPersonTest.kt`:
  - God, Lord, Yahweh
  - First priority names: Adam, Eve, Cain, Abel, Noah, Abraham, Sarah, Isaac, Rebekah, Jacob, Esau, Joseph, Moses, Aaron, Miriam, Joshua, Caleb

- `NTGospelsPersonTest.kt`:
  - Jesus Christ, Jesus, Christ
  - First priority names: Mary, Joseph, John the Baptist, Peter, James, John

## Place

The `org.gnit.bible.test.search.place` package aims to systematically cover important places, regions, nations, cities, mountains, rivers, seas, wildernesses, and temples that appear in the Bible.
A place falls into one category of when first mentioned in the Bible.

Place tests should prefer concrete named locations first, then important regional or national names.
When a place has multiple common names, include each name that a user would naturally search for.

Following examples explains how a test file contains a place:

- `OTPentateuchPlaceTest.kt`:
  - Genesis places: Eden, garden of Eden, Nod, Ararat, Babel, Ur, Canaan, Egypt, Sodom, Gomorrah, Bethel, Hebron, Beersheba
  - Regional names: Shinar, Negev, Philistines, Mamre, Machpelah
  - Exodus places: Egypt, Goshen, Midian, Sinai, Horeb, Red Sea, wilderness, Marah, Elim, Rephidim
  - Worship places: tabernacle, tent of meeting

- `NTGospelsPlaceTest.kt`:
  - First priority places: Bethlehem, Nazareth, Galilee, Jerusalem, Jordan, Judea, Samaria, Capernaum, Bethany, Gethsemane, Golgotha
  - Worship places: temple, synagogue

- `NTApocalypticPlaceTest.kt`:
  - First priority places: Patmos, Asia, Ephesus, Smyrna, Pergamum, Thyatira, Sardis, Philadelphia, Laodicea, Babylon, New Jerusalem

## Event

The `org.gnit.bible.test.search.event` package aims to cover important actions, events, miracles, judgments, covenants, wars, journeys, births, deaths, teachings, and visions that appear in the Bible.
An event falls into one category of when it first appears or where the main event is narrated.

Event tests should use search terms that real users would type, not only formal theological names.
For example, include both "crossing the Red Sea" and simple terms such as "red sea", "passed through", or "dry land" when they are useful for search behavior.

Following examples explains how a test file contains an event:

- `OTPentateuchEventTest.kt`:
  - Genesis events: creation, fall, flood, tower of Babel, call of Abraham, covenant with Abraham, destruction of Sodom, sacrifice of Isaac, Jacob wrestles, Joseph sold, Joseph interprets dreams
  - Exodus events: burning bush, ten plagues, Passover, exodus, crossing the Red Sea, manna, water from the rock, giving of the law, golden calf, tabernacle construction

- `NTGospelsEventTest.kt`:
  - First priority events: birth of Jesus, baptism of Jesus, temptation of Jesus, sermon on the mount, feeding the five thousand, transfiguration, triumphal entry, last supper, crucifixion, resurrection, ascension

- `NTApocalypticEventTest.kt`:
  - First priority events: letters to the seven churches, opening of the seals, trumpet judgments, bowl judgments, fall of Babylon, final judgment, new heaven and new earth

## Object

The `org.gnit.bible.test.search.object` package aims to cover important physical objects, animals, plants, foods, tools, weapons, buildings, offerings, measurements, garments, and ritual items that appear in the Bible.
An object falls into one category of when first mentioned in the Bible.

Object tests should prioritize named or repeated objects that users are likely to search for.
When an object is central to a story or command, include nearby common words that should rank the expected passage correctly.

Following examples explains how a test file contains an object:

- `OTPentateuchObjectTest.kt`:
  - Genesis objects: tree of life, tree of knowledge, ark, rainbow, altar, tent, well, birthright, blessing, coat, dream, grain
  - Animals and foods: serpent, sheep, ram, goat, cattle, bread, wine
  - Exodus objects: staff, blood, unleavened bread, manna, quail, tablets, ark of the covenant, mercy seat, lampstand, altar, ephod, incense
  - Worship objects: tabernacle, curtain, veil, basin, priestly garments

- `NTGospelsObjectTest.kt`:
  - First priority objects: manger, cross, crown of thorns, tomb, stone, cup, bread, wine, boat, net, fish, denarius, fig tree
  - Worship objects: temple, scroll, synagogue, Sabbath bread

- `NTApocalypticObjectTest.kt`:
  - First priority objects: scroll, seal, trumpet, bowl, lampstand, crown, white robe, book of life, mark of the beast, throne, lake of fire

## Concept

The `org.gnit.bible.test.search.concept` package aims to cover important abstract words, doctrines, commands, virtues, sins, relationships, offices, promises, blessings, curses, and theological themes that appear in the Bible.
A concept falls into one category of when first mentioned in the Bible, unless the concept is strongly tied to a later book or section.

Concept tests should be strict enough to expose poor analyzer behavior.
If a language-specific analyzer fails to connect important forms of a concept, keep the assertion and fix the analyzer or lucene-kmp behavior instead of weakening the expected result.

Following examples explains how a test file contains a concept:

- `OTPentateuchConceptTest.kt`:
  - Genesis concepts: creation, image of God, sin, curse, blessing, covenant, promise, faith, righteousness, sacrifice, inheritance
  - Exodus concepts: redemption, deliverance, Passover, law, commandment, holiness, priesthood, atonement, worship, Sabbath, presence of God

- `NTGospelsConceptTest.kt`:
  - First priority concepts: kingdom of God, repentance, gospel, faith, forgiveness, mercy, discipleship, eternal life, resurrection, salvation, love

- `NTPaulineEpistlesConceptTest.kt`:
  - First priority concepts: justification, grace, faith, righteousness, reconciliation, adoption, body of Christ, spiritual gifts, resurrection, sanctification

- `NTApocalypticConceptTest.kt`:
  - First priority concepts: testimony, endurance, judgment, worship, victory, wrath, beast, martyrdom, new creation, eternal kingdom
