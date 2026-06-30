package org.gnit.bible.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnit.bible.Bible
import org.gnit.bible.Books
import org.gnit.bible.VersePointer
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.ui.widgets.sansFontFamily
import org.gnit.bible.app.ui.widgets.serifFontFamily

@Composable
fun SearchResultsScreen(
    bibleState: BibleState,
    query: String,
    innerPadding: PaddingValues,
    onResultClick: (VersePointer) -> Unit
) {
    val bible = currentBible()
    var loading by remember(query, bibleState.mainTranslation) { mutableStateOf(query.isNotBlank()) }
    var results by remember(query, bibleState.mainTranslation) { mutableStateOf(emptyList<VersePointer>()) }
    var error by remember(query, bibleState.mainTranslation) { mutableStateOf<String?>(null) }

    LaunchedEffect(query, bibleState.mainTranslation) {
        if (query.isBlank()) {
            loading = false
            results = emptyList()
            error = null
            return@LaunchedEffect
        }

        loading = true
        error = null
        val searchResult = withContext(Dispatchers.Default) {
            runCatching {
                bible.search(
                    term = query,
                    verses = bible.searchResultFromSettings(),
                    translation = bibleState.mainTranslation
                )
            }
        }
        searchResult
            .onSuccess { results = it }
            .onFailure { error = it.message ?: "Search failed." }
        loading = false
    }

    when {
        loading -> SearchMessage(innerPadding, "Searching...")
        error != null -> SearchMessage(innerPadding, error ?: "Search failed.")
        query.isBlank() -> SearchMessage(innerPadding, "Enter a search query.")
        results.isEmpty() -> SearchMessage(innerPadding, "No results found.")
        else -> SearchResultList(
            bibleState = bibleState,
            bible = bible,
            results = results,
            innerPadding = innerPadding,
            onResultClick = onResultClick
        )
    }
}

@Composable
private fun SearchResultList(
    bibleState: BibleState,
    bible: Bible,
    results: List<VersePointer>,
    innerPadding: PaddingValues,
    onResultClick: (VersePointer) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(results) { pointer ->
            SearchResultRow(
                bibleState = bibleState,
                bible = bible,
                pointer = pointer,
                onClick = { onResultClick(pointer) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun SearchResultRow(
    bibleState: BibleState,
    bible: Bible,
    pointer: VersePointer,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val currentFontFamily = if (bibleState.isFontFamilySerif) {
            bibleState.mainTranslation.language.serifFontFamily()
        }else{
            bibleState.mainTranslation.language.sansFontFamily()
        }

        Text(
            text = Books.formatHeader(pointer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = currentFontFamily,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = searchResultText(bible, pointer),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = currentFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchMessage(
    innerPadding: PaddingValues,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (message == "Searching...") {
            CircularProgressIndicator()
        }
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun searchResultText(bible: Bible, pointer: VersePointer): String {
    val chapterText = bible.verses(pointer.translation.code, pointer.book, pointer.chapter)
    val selected = Bible.selectVerses(pointer, chapterText)
    val verse = pointer.startVerse ?: return selected.trim()
    return selected.replaceFirst(Regex("^$verse\\s+"), "").trim()
}
