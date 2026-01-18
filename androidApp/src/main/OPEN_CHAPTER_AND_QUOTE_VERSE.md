# OPEN_CHAPTER

**Overview:**\
The **Open Chapter** feature enables users to jump directly to a
specific Bible chapter in the app via voice (Google Assistant App
Actions) or deep link URLs. We will implement this on Android using App
Actions and deep linking. (iOS can use Siri Shortcuts separately; see
suggestions below.)

The implementation includes: - Declaring an App Action capability in
`shortcuts.xml` for opening a chapter (with parameters for translation,
book, chapter). - Adding an Android `IntentFilter` to handle the custom
deep link URI scheme (`bbl://read?...`). - Kotlin code (in the Android
source set) to parse the deep link intent and navigate to the requested
chapter in the Compose UI.

## Android Implementation

### 1. App Actions Capability (shortcuts.xml)

In your Android resources (e.g.
`composeApp/src/androidMain/res/xml/shortcuts.xml`), declare a
capability for **OPEN_CHAPTER**. We can use a built-in intent if
appropriate (like `actions.intent.OPEN_APP_FEATURE`), or define a custom
intent. Here, we\'ll use the generic `OPEN_APP_FEATURE` BII with
parameters for our use case. This lets users invoke it with phrases like
*"Open Genesis chapter 1 in MyBibleApp"*. The parameters (translation,
book, chapter) will be extracted and used to fulfill the deep link.

    <?xml version="1.0" encoding="utf-8"?>
    <shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
        <!-- Capability: Open a specific Bible chapter -->
        <capability android:name="actions.intent.OPEN_APP_FEATURE">
            <!-- Map built-in 'feature' parameter to our Bible chapter feature -->
            <parameter
                android:name="feature"
                android:key="feature"
                android:entity="featureName">
            </parameter>
            <!-- Additional parameters for translation, book, and chapter -->
            <parameter
                android:name="org.gnit.bible.translation"
                android:key="translation"
                android:entity="BibleTranslation" />
            <parameter
                android:name="org.gnit.bible.book"
                android:key="book"
                android:entity="BibleBook" />
            <parameter
                android:name="org.gnit.bible.chapter"
                android:key="chapter"
                android:entity="Number" />
            <!-- Fulfillment via deep link URL template -->
            <intent 
                android:action="android.intent.action.VIEW"
                android:data="bbl://read?translation={translation}&amp;book={book}&amp;chapter={chapter}" />
        </capability>
    </shortcuts>

**Explanation:** The `<capability>` above uses the `OPEN_APP_FEATURE`
intent to indicate our app can open a specific feature. We add custom
parameters for translation, book, and chapter. The `android:data` with
`bbl://read?...` is a deep link template that Assistant will invoke.
`{translation}`, `{book}`, `{chapter}` placeholders correspond to
user-provided values. We also define `android:entity` for parameters
which can reference an inventory of valid values: - **BibleTranslation**
-- possible translation codes/names (e.g., KJV, NIV, etc.). -
**BibleBook** -- book names (Genesis, Exodus, ...). - **Number** --
built-in entity for numeric values (chapters).

You should provide or reference entity lists for translations and books
so Assistant can better parse queries. For example, you can create JSON
files or inline inventory for `BibleTranslation` and `BibleBook`: -
**BibleBook**: 66 entries mapping spoken book names (and common
abbreviations) to book indices 1--66. - **BibleTranslation**: entries
for available translation names/codes (use `AssetManagerImpl` to get the
list of available translation codes and names at runtime, or maintain a
static list for Assistant's use).

*(If maintaining a static inventory is cumbersome, consider using a web
inventory or dynamic entity binding. For simplicity, ensure common
translations like \"KJV\" ("King James Version"), \"WEB\" ("World
English Bible"), etc., are recognized.)*

### 2. Deep Link Intent Filter (AndroidManifest.xml)

In the Android manifest, add an intent filter for the custom URI scheme
(`bbl://`). This allows the app to be launched via the deep link from
Assistant or other sources. For example, in
`composeApp/src/androidMain/AndroidManifest.xml` inside the
`<activity ... MainActivity>`, add:

    <activity android:name=".MainActivity" ...>
        <!-- ... other activity config ... -->
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <!-- Accept our custom scheme and path pattern for reading a chapter -->
            <data
                android:scheme="bbl"
                android:host="read"
                android:pathPattern="/.*" />
        </intent-filter>
    </activity>

This filter listens for URIs like `bbl://read?...`. We use a broad
pathPattern to capture any parameters after `/read`. (Alternatively,
specify exact query parameter names if needed.) The presence of this
filter means when the Assistant (or a web link, etc.) launches
`bbl://read?...`, Android will open our `MainActivity`.

### 3. Handling the Deep Link in MainActivity

In `MainActivity.kt` (Android source), override `onCreate` to check if
the activity was launched via the deep link and extract the chapter
parameters. Then initialize the Compose UI with the corresponding Bible
state.

Because our UI is in Compose (single activity), we need to pass the
requested translation/book/chapter to the composable. We can achieve
this by providing an initial state to the `BibleApp` or by updating the
state after launch. One approach is to modify the `App` composable to
accept optional deep-link arguments. Another simpler approach: intercept
the deep link in `MainActivity` and store the parameters in a global or
`platform.settings` so that `rememberBibleState()` can pick it up. Here,
we\'ll directly supply an initial `BibleState` to the UI.

**Step 3a:** Modify `App.kt` to accept an optional `initialState`. For
example, update `BibleApp` composable to take
`initialBibleState: BibleState?` (default null). If provided, use that
instead of loading from saved prefs. For instance:

    @Composable
    fun BibleApp(
        platformContext: Any? = null,
        initialChromeVisible: Boolean = true,
        initialBibleState: BibleState? = null
    ) {
        platform = getPlatform(platformContext)
        bible()  // initialize Bible singleton if needed

        // Determine the initial Bible state
        val initialState = initialBibleState ?: rememberBibleState()
        var bibleState by rememberSaveable(stateSaver = BibleStateSaver) {
            mutableStateOf(initialState)
        }
        ...
        // (rest of UI unchanged, using bibleState)
    }

**Step 3b:** In `MainActivity.onCreate`, parse the intent and create the
initial state if a deep link is present:

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for deep link data
        val data: Uri? = intent?.data
        var deepLinkState: BibleState? = null
        if (data != null && data.scheme == "bbl") {
            // Example URI: bbl://read?translation=kjv&book=1&chapter=1
            val params = data.queryParameterNames
            val translationCode = data.getQueryParameter("translation") ?: "kjv"
            val bookNum = data.getQueryParameter("book")?.toIntOrNull() ?: 1
            val chapterNum = data.getQueryParameter("chapter")?.toIntOrNull() ?: 1
            // Construct initial state (use default values for other fields)
            // Find Translation object by code (from embedded list or availableTranslations)
            val mainTrans = Translation.embeddedTranslations.find { it.code == translationCode }
                             ?: Translation.webus  // fallback to WEB if not found
            deepLinkState = BibleState(
                mainTranslation = mainTrans,
                book = bookNum,
                chapter = chapterNum
            )
        }

        setContent {
            // Pass applicationContext for Platform and deepLinkState for initial content
            App(platformContext = this.applicationContext, initialBibleState = deepLinkState)
        }
    }

Now, when the app is launched via a deep link (from Assistant or
elsewhere), it will jump directly to the specified
translation/book/chapter. The `BibleState` is initialized accordingly,
so the Compose UI (`SingleBible` view) will display the requested
chapter.

**Note:** If the deep link includes a `verse` parameter (for future
extension), you could similarly capture it and possibly scroll or
highlight that verse. Currently, the **Open Chapter** feature opens at
the start of the chapter.

### 4. Verify App Actions in Assistant

With the above in place, test the integration: - Build and install the
app on an Android device. - Use the App Actions test tool (Android
Studio Plugin or Google Assistant) to invoke the capability. For
example, try voice commands like: - "Hey Google, open **Genesis chapter
1** in **MyBibleApp**" - "Hey Google, open **John chapter 3** in
**MyBibleApp** in **KJV**" - The Assistant should parse the query, match
our `OPEN_APP_FEATURE` capability, and launch the app via the deep link.
The app should navigate directly to the specified chapter.

**Parameter Parsing:** We rely on Assistant to interpret the book name,
chapter number, and optionally translation name: - The `BibleBook`
entity should cover variations of book names (e.g., \"1 Samuel\" vs
\"First Samuel\"). You might create a mapping of common aliases in the
shortcuts.xml or an associated JSON. - The `BibleTranslation` entity
should include at least the codes/names of translations in
`Bible.availableTranslations()`. Use `AssetManagerImpl` and
`Bible.availableTranslations()` to get the list of translations (code
and names). You can then provide these in the App Actions test as an
inline inventory or just ensure the most likely ones are recognized.

### 5. iOS Considerations (Siri Shortcut)

Apple's iOS doesn't support App Actions, but you can achieve similar
"Open Chapter" functionality using Siri Shortcuts or SiriKit: -
**NSUserActivity / Siri Shortcut:** Define a shortcut like \"Open
\[Book\] \[Chapter\]\" in the iOS app. For example, when a user opens a
chapter in-app, donate an `NSUserActivity` with a title like \"Open
(BookName) Chapter (N)\" to Siri. This lets users later say "Hey Siri,
open (BookName) chapter (N) in MyBibleApp" to deep-link into the iOS
app. You'd handle this via `SceneDelegate` or `AppDelegate` by checking
for the activity and navigating to the requested content. - **Intents
Extension:** For a more structured approach, you could create a SiriKit
Intent (e.g., a custom intent \"OpenChapterIntent\" with parameters for
book and chapter). Implement an Intents extension to handle it, and use
`INInteraction` to donate intents as the user navigates. This is a bit
more involved and platform-specific. - Since this logic cannot be easily
shared via Kotlin Multiplatform, it's acceptable to implement it
directly in Swift/Objective-C on the iOS side (inside the Xcode project
under `iosApp`). You do not need to use `expect/actual` unless you want
a common interface; instead, treat it as a separate feature on iOS.

**Summary:** The **Open Chapter** feature on Android uses App Actions
and deep links (`bbl://read?...`). We declared the capability in
`shortcuts.xml`, added an intent filter, and updated `MainActivity` and
Compose initialization to handle incoming chapter requests. On iOS, a
comparable user experience can be achieved with Siri Shortcuts,
implemented natively. This separation (Android in `androidMain`, iOS in
`iosMain`) keeps platform-specific code isolated, as preferred for KMP
projects.

# QUOTE_VERSE

**Overview:**\
The **Quote Verse** feature allows users to ask the Assistant for a
specific Bible verse and have the app respond with that verse's text. We
will implement this using an **Assistant Widget fulfillment** on
Android. When the user invokes the action (e.g., \"Hey Google, ask
MyBibleApp to quote John 3:16\"), Google Assistant will display an
**interactive widget** (a remote views app widget) showing the verse
text. We'll also provide a fallback deep link so the app can open
directly to the verse if needed.

Key implementation steps: - Define an App Actions capability in
`shortcuts.xml` for quoting a verse, with **widget fulfillment**
(AppWidget) to display the verse text in Assistant. Include parameters
for translation (optional), book, chapter, and verse. - Implement an
Android App Widget (`AppWidgetProvider`) that can render the verse text
and handle user interaction (like tapping to open the app). - Declare
the widget in the manifest and provide a layout for it. - In the widget
provider code, fetch the verse text using the `Bible` class
(`Bible.verses` and the `splitChapterToVerses` helper) and update the
widget UI. - Set up a fallback intent in `shortcuts.xml` to handle cases
where Assistant cannot fully fulfill via widget (e.g., open the app to
that verse).

All Android-specific code will reside in the `androidMain` source set.
(There is no direct iOS equivalent to Assistant widgets; on iOS one
might use Siri Suggestions or an Intents UI extension, which would be a
separate implementation.)

## 1. App Actions Setup for Quote Verse (shortcuts.xml)

We add a new capability in the same `shortcuts.xml` (Android res/xml)
for the **QuoteVerse** intent. This will use a **custom intent name**
since there is no built-in intent specifically for Bible verses. We'll
specify that the primary fulfillment is via an `<app-widget>` and
include a fallback `<intent>`.

    <capability android:name="org.gnit.bible.QUOTE_VERSE">
        <!-- Define parameters for translation, book, chapter, verse -->
        <app-widget
            android:identifier="QUOTE_VERSE_WIDGET"
            android:targetClass="org.gnit.bible.QuoteVerseAppWidgetProvider">
            <!-- Book name (e.g. "John") parameter -->
            <parameter
                android:name="verseQuery.book"
                android:key="book"
                android:entity="BibleBook"
                android:required="true" />
            <!-- Chapter number parameter -->
            <parameter
                android:name="verseQuery.chapter"
                android:key="chapter"
                android:entity="Number"
                android:required="true" />
            <!-- Verse number parameter -->
            <parameter
                android:name="verseQuery.verse"
                android:key="verse"
                android:entity="Number"
                android:required="true" />
            <!-- (Optional) Translation parameter -->
            <parameter
                android:name="verseQuery.translation"
                android:key="translation"
                android:entity="BibleTranslation"
                android:required="false" />
        </app-widget>
        <!-- Fallback: if parameters are incomplete or widget not supported, open app -->
        <intent 
            android:action="android.intent.action.VIEW"
            android:data="bbl://read?translation={translation}&amp;book={book}&amp;chapter={chapter}&amp;verse={verse}" 
            android:targetClass="org.gnit.bible.MainActivity" />
    </capability>

**Explanation:**\
- We use a custom intent name `org.gnit.bible.QUOTE_VERSE` to represent
the \"quote a verse\" action. The `<app-widget>` element declares that
this capability is fulfilled by an App Widget UI. We reference the
AppWidgetProvider class (`QuoteVerseAppWidgetProvider`) which we will
implement. Each `<parameter>` corresponds to part of the verse
reference: - `book` (Bible book name, required) - `chapter` (chapter
number, required) - `verse` (verse number, required) - `translation`
(Bible translation, optional; if not provided, we can default to the
app's main translation or a default like KJV) - We mark translation as
not required so that if the user doesn't specify a version, the action
can still fulfill (we'll use a default). - The `android:entity`
attributes point to predefined or custom entities: - **BibleBook** --
same entity as before listing the 66 book names (and aliases). -
**Number** -- built-in numeric recognition for chapter and verse. -
**BibleTranslation** -- list of translations (codes/names). - The
`<intent>` element after `<app-widget>` is the **fallback fulfillment**.
It has no required parameters (we didn't nest `<parameter>` inside it),
meaning it can handle even incomplete queries. This fallback uses the
deep link URL approach: it will open `MainActivity` via `bbl://read?...`
with the provided parameters. We include placeholders for all parameters
(`{translation}`, `{book}`, `{chapter}`, `{verse}`) so that if Assistant
did capture them, they'll be in the URI. If some are missing (say the
user just said \"quote a verse\" without specifics), the deep link would
be incomplete; in such cases, you might handle it by opening the app to
a generic screen or prompt the user. (Our code can default missing
translation as needed.)

This setup means: - If the Assistant fully understands the query (book,
chapter, verse, etc.), it will try **widget fulfillment**, calling our
`QuoteVerseAppWidgetProvider` to render the verse. - If the device
doesn't support widgets in Assistant, or the query was
incomplete/ambiguous, Assistant can fall back to launching the app via
the deep link.

## 2. Android Widget Implementation

### a. Widget Layout (quote_verse_widget.xml)

Create a layout for the verse widget in
`composeApp/src/androidMain/res/layout/quote_verse_widget.xml`. This
layout defines how the verse will appear in the Assistant response card.
It should be a simple RemoteViews layout (since App Widgets use
RemoteViews). For example:

    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/widget_root"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@color/white">

        <!-- Verse Text -->
        <TextView
            android:id="@+id/verse_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Verse text will appear here"
            android:textColor="@color/black"
            android:textSize="18sp"
            android:lineSpacingExtra="4sp"
            android:maxLines="10" 
            android:ellipsize="end"/>

        <!-- Reference (Book Chapter:Verse, Translation) -->
        <TextView
            android:id="@+id/verse_reference"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="John 3:16 (KJV)"
            android:textColor="@color/black"
            android:textSize="14sp"
            android:paddingTop="8dp"
            android:gravity="end"/>
    </LinearLayout>

This layout contains: - `verse_text`: a TextView for the verse
content. - `verse_reference`: a smaller TextView for the reference
(e.g., \"John 3:16 (KJV)\") displayed below the verse text. (We include
translation here if provided.)

We keep the design simple (black text on white background, some
padding). The Assistant will render this card in its UI. Ensure the IDs
match what we use in the RemoteViews update code.

### b. AppWidgetProvider Class (QuoteVerseAppWidgetProvider.kt)

Now implement the widget provider in
`composeApp/src/androidMain/kotlin/org/gnit/bible/QuoteVerseAppWidgetProvider.kt`:

    package org.gnit.bible

    import android.app.PendingIntent
    import android.appwidget.AppWidgetManager
    import android.appwidget.AppWidgetProvider
    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.widget.RemoteViews

    class QuoteVerseAppWidgetProvider : AppWidgetProvider() {

        override fun onReceive(context: Context, intent: Intent) {
            super.onReceive(context, intent)
            // Our App Action invocation may arrive as a broadcast with extras
            if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == "android.appwidget.action.APPWIDGET_UPDATE") {
                // Extract parameters from intent extras
                val bookName = intent.getStringExtra("book")
                val chapterNum = intent.getIntExtra("chapter", -1)
                val verseNum = intent.getIntExtra("verse", -1)
                val translationCode = intent.getStringExtra("translation") ?: Translation.kjv.code  // default to KJV if not provided

                if (!bookName.isNullOrEmpty() && chapterNum != -1 && verseNum != -1) {
                    // Fetch the verse text using shared Bible logic
                    val platform = getPlatform(context)  // get Platform for Android with context
                    val assetManager = AssetManagerImpl(platform = platform)
                    val bible = Bible(assetManager)
                    bible.bibleResourcesReader = ComposeBibleResourcesReader()  // use Compose reader for embedded texts

                    // Determine the translation object (if translationCode not found, default to KJV)
                    val translationObj = bible.availableTranslations().find { it.code.equals(translationCode, ignoreCase = true) }
                                            ?: Translation.kjv

                    // Get the chapter text and split into verses
                    val chapterText = bible.verses(translationObj.code, book = bookNameToIndex(bookName), chapter = chapterNum)
                    val versesArray = splitChapterToVerses(chapterText)
                    val verseIndex = verseNum - 1  // zero-based index
                    val verseText = if (verseIndex in versesArray.indices) versesArray[verseIndex] else ""

                    // Prepare the RemoteViews for the widget UI
                    val views = RemoteViews(context.packageName, R.layout.quote_verse_widget)
                    // Set the verse text (and optionally trim or add quotes)
                    views.setTextViewText(R.id.verse_text, verseText.trim())
                    // Set the reference text (Book Chapter:Verse (Translation))
                    val reference = "$bookName $chapterNum:$verseNum (${translationObj.code.uppercase()})"
                    views.setTextViewText(R.id.verse_reference, reference)

                    // Make the whole widget clickable: tapping opens the app to this verse
                    val uri = Uri.parse("bbl://read?translation=${translationObj.code}&book=${bookNameToIndex(bookName)}&chapter=${chapterNum}&verse=${verseNum}")
                    val openIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        `package` = context.packageName
                    }
                    val pending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_root, pending)

                    // Update the widget (use app context with the specific ComponentName)
                    AppWidgetManager.getInstance(context).updateAppWidget(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS), views)
                }
            }
        }

        // Helper to convert book name to index (1-66). Could use a map of name->number.
        private fun bookNameToIndex(book: String): Int {
            // Ideally, have a map of canonical book names to their index. For simplicity:
            return Translation.webus.books().entries.find { (_, name) -> name.equals(book, ignoreCase = true) }?.key ?: 1
        }
    }

**Key points in the provider code:** - We override `onReceive` rather
than just `onUpdate` because Assistant might send a specific broadcast
with the extras. The `shortcuts.xml` setup will trigger an update for
our widget with the provided parameters. - We check the intent's extras
for `"book"`, `"chapter"`, `"verse"`, `"translation"`. These keys
correspond to the `android:key` values we set in shortcuts.xml. The
Assistant will include them when invoking the widget. - We default the
translation to KJV (or any default) if none was provided. - We use the
shared logic to get the verse text: - Create a `Platform` for Android
using the context (`getPlatform(context)` returns an AndroidPlatform). -
Instantiate `AssetManagerImpl` with that platform (for file access if
needed). - Instantiate `Bible` with the AssetManager and set its
`bibleResourcesReader` to `ComposeBibleResourcesReader()` so we can read embedded
translations. - Find the `Translation` object for the given code (or
default to KJV). We use `bible.availableTranslations()` which merges
embedded and downloaded translations. (This uses `AssetManager` to
include any downloaded ones.) - Call
`bible.verses(translationCode, bookIndex, chapterNum)` to get the full
chapter text. We need the **book index** (1--66) for the `verses`
function. The `bookNameToIndex` helper function demonstrates converting
a book name to its index. In practice, you'd have a comprehensive
mapping (Genesis -\> 1, Exodus -\> 2, \..., Revelation -\> 66). We can
utilize `Translation.books()` or `Language.bookNames()` to assist. - Use
the provided `splitChapterToVerses()` function in class Bible to break the chapter text
into an array of verse strings. (This function is defined in the shared
code, and we can directly use it here since it's in the common source.
It splits on the verse number markers. We ensure to trim or remove any
leading number artifacts.) - Extract the specific verse text by index
(note: verse numbers are 1-indexed, array is 0-indexed). - We then
populate the RemoteViews: - Set the verse text in the `R.id.verse_text`
TextView. - Set the reference (book name, chapter:verse, translation
code) in the `R.id.verse_reference` TextView. This gives context to the
quote. - We add a click handler: if the user taps the widget card, it
should open the app to that verse. We create an `Intent.ACTION_VIEW`
with our deep link (`bbl://read?...`) including the exact
translation/book/chapter/verse, and wrap it in a `PendingIntent`. We
attach this to the root layout (`widget_root`) via
`setOnClickPendingIntent`. - This way, the user can tap the Assistant
card and it will launch `MainActivity` at the specified verse for
further reading or context. - Finally, we call
`AppWidgetManager.updateAppWidget` to apply our RemoteViews to the
widget. We obtain the widget IDs from the intent (`EXTRA_APPWIDGET_IDS`)
that Assistant provided. Alternatively, we could update by finding the
component (since this widget likely is not placed on home screen,
Assistant uses it transiently).

**Note:** The `bookNameToIndex` method is simplified. In production,
maintain a map of book names (and abbreviations) to their number
(1--66). You could use the data in `Language.bookNames()` or an internal
lookup table. This ensures even if Assistant passes \"Song of Solomon\",
you map it correctly to the index.

### c. Registering the Widget in AndroidManifest

In the AndroidManifest (inside the `<application>` tag), add the widget
provider declaration:

    <receiver 
        android:name="org.gnit.bible.QuoteVerseAppWidgetProvider"
        android:exported="false">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent-filter>
        <meta-data
            android:name="android.appwidget.provider"
            android:resource="@xml/quote_verse_widget_info" />
    </receiver>

Also create the `quote_verse_widget_info.xml` in `res/xml` to describe
the widget (even if it's primarily used by Assistant, we need this
metadata):

    <?xml version="1.0" encoding="utf-8"?>
    <appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
        android:minWidth="200dp"
        android:minHeight="100dp"
        android:updatePeriodMillis="0"
        android:autoAdvanceViewId="@+id/verse_text"
        android.initialLayout="@layout/quote_verse_widget" />

This `appwidget-provider` XML specifies the initial layout and some
required properties. We set no automatic update
(`updatePeriodMillis="0"`) because updates are only on-demand when
Assistant triggers it. The `minWidth/Height` can be adjusted (they don't
matter much for Assistant's display, but are required fields).

### d. Using Bible.verses and splitChapterToVerses

As shown in the provider code, we leverage the existing `Bible` class
and its `verses` function to get scripture text. The
`splitChapterToVerses()` utility (already in shared code) is used to
separate the chapter string into individual verses. **This ensures we
preserve verse numbering correctly**. The function essentially removes
the chapter header and splits on the verse number regex. For reference,
the implementation is:

    fun splitChapterToVerses(aChapter: String): Array<String> {
        return aChapter.substring(2).split("\\n\\d{1,3} ".toRegex()).toTypedArray()
    }

We call `substring(2)` to drop the leading chapter number and space,
then split on newline followed by a number and space (which is how
verses are delineated in the text). The result is an array where index 0
corresponds to verse 1's text, index 1 to verse 2's text, etc. By using
this function, we don't have to manually parse verse text; we rely on
the format coming from `Bible.verses()`.

**Important:** Ensure the `BibleResourcesReader` is set (as we did with
`ComposeBibleResourcesReader()`) before calling `Bible.verses`, otherwise for
embedded translations the call will fail (since `bibleResourcesReader` is
lateinit in `Bible`). We handle that in the widget provider.

### 3. Testing the Quote Verse Action

With everything in place, test the flow: - Install the updated app on an
Android device. - Use the App Actions test tool or voice to trigger the
query, for example:\
"Hey Google, ask MyBibleApp to **quote John 3:16**"\
"Hey Google, what does **Genesis 1:1** say in **WEB**" (World English
Bible)\
- The Assistant should invoke our `QUOTE_VERSE` capability. If all
parameters are understood, you will see a card appear with the verse
text and reference. (Assistant might say it verbally as well, using
text-to-speech of the card text.) - Tap on the card to verify it
launches the app to the correct chapter/verse (the deep link should
include the verse parameter so we could extend `MainActivity` to
highlight or scroll to that verse in the future).

**Fallback behavior:** Try a query like "Ask MyBibleApp to quote a
verse" (without specifying which). Assistant may not have the required
parameters (book, chapter, verse). In this case, our shortcuts.xml
defines a fallback `<intent>` with no required parameters. Assistant
should then launch the app (via the deep link). The deep link might be
incomplete (missing book/chapter), so in `MainActivity` you would
receive `verse=null, book=null` etc. You can handle this by opening a
default screen or maybe prompting the user. This ensures the action is
robust to partial input.

### 4. Additional Notes on Implementation

- **Translation handling:** If the user specifies a translation by name
  (e.g., "NIV" or "New International Version"), the Assistant might pass
  the full name. Our implementation expects a translation **code** (like
  \"niv\") via the `translation` extra. We may need to enhance entity
  handling: the `BibleTranslation` entity could list common names and
  map them to codes. Alternatively, after receiving a translation
  string, use a lookup: e.g., if `translationCode` length \> 3 or
  contains spaces, try to find a Translation whose englishName or
  nativeName matches (case-insensitive). Then use its code. For
  simplicity, we default to KJV if we can't map it.
- **App compatibility:** The App Widget we created is primarily for
  Assistant. It doesn't need to be added to the home screen by the user.
  We declared it in the manifest so Assistant can instantiate it on the
  fly. It's marked `exported=false` (since Assistant invokes it within
  the same package context).
- **Security:** Because the widget can be invoked by Assistant, ensure
  you do not expose sensitive data. In our case, we're only showing
  Bible text, which is fine. The `exported=false` on the receiver means
  only our app/Assistant (as it uses an internal binding) should be
  calling it.
- **Multiplatform considerations:** The widget and App Actions are
  Android-specific. We do not use `expect/actual` here; we simply
  implement them in `androidMain`. The shared code (Bible, Translation
  data, etc.) is leveraged to avoid duplicating logic (e.g., verse
  retrieval). On iOS, you might separately implement a Siri Intent to
  "quote verse" which could use the shared logic for fetching verses
  (via common code) but the invocation and UI would be different
  (possibly just showing a notification or launching the app with the
  verse).

**Summary:** The **Quote Verse** feature uses Google Assistant App
Actions with widget fulfillment. We declared a capability for quoting
verses, implemented a dedicated AppWidgetProvider to render the verse
text in an Assistant card, and provided a deep link fallback. When
invoked, the user gets a spoken and visual response with the verse, and
can tap it to jump into the app. This enhances the voice
interoperability of *bbl-kmp*, making scripture retrieval hands-free and
integrated with Assistant.

------------------------------------------------------------------------
