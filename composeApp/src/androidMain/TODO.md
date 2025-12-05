# OPEN_CHAPTER_AND_QUOTE_VERSE progress checklist

- [x] Add deep link intent filter for `bbl://read` in `AndroidManifest.xml`.
- [x] Wire `MainActivity` to parse deep link params and pass `initialBibleState` to Compose.
- [x] Allow `BibleApp/App` to accept optional `initialBibleState`.
- [x] Declare App Actions shortcuts (open chapter + quote verse) in `res/xml/shortcuts.xml`.
- [x] Provide widget metadata `quote_verse_widget_info.xml` and layout `quote_verse_widget.xml`.
- [x] Implement `QuoteVerseAppWidgetProvider` to render verse text and open app on tap.
- [x] Register widget receiver and shortcuts metadata in manifest.
- [ ] Expand book-name lookup to cover aliases/abbreviations for Quote Verse.
- [ ] Add robust translation name-to-code mapping (names, aliases) for Assistant inputs.
- [ ] Handle optional `verse` deep link in app UI (scroll/highlight).
- [ ] Manual Assistant testing with App Actions test tool on device.
