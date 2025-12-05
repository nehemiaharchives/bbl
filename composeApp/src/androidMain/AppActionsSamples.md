# App Actions sample invocations (use in App Actions Test Tool)

Use these as “Sample Queries” or test phrases when creating a preview in the App Actions Test Tool. They exercise both chapter-open and verse-quote flows with and without translation specified.

## Open Chapter (OPEN_APP_FEATURE / bbl://read)
- open {BibleBook} {chapter} {translation}
- open {BibleBook} {chapter}
- open Bible {BibleBook} {chapter} {translation}
- open Bible {BibleBook} {chapter}
- open Bible app {BibleBook} {chapter} {translation}
- open Bible app {BibleBook} {chapter}
- open Bible with {BibleBook} {chapter} {translation}
- open Bible with {BibleBook} {chapter}
- open Bible app with {BibleBook} {chapter} {translation}
- open Bible app with {BibleBook} {chapter}

## Quote Verse (QUOTE_VERSE / widget fulfillment)
- read {BibleBook} {chapter}:{verse}
- quote {BibleBook} {chapter}:{verse}
- what is {BibleBook} {chapter}:{verse}
