package org.gnit.bible.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.gnit.bible.BibleState
import org.gnit.bible.Language
import org.gnit.bible.Translation
import org.gnit.bible.DOWNLOADABLE_BIBLE_BASE_URL
import org.gnit.bible.DOWNLOADABLE_BIBLE_LIST_URL
import org.gnit.bible.TranslationEntry
import org.gnit.bible.InstallationState
import org.gnit.bible.assetManager
import org.gnit.bible.bible
import org.gnit.bible.downloadableTranslations
import org.gnit.bible.logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.gnit.bible.ui.theme.BibleTheme
import org.gnit.bible.withTranslationVisibility

@Composable
fun TranslationManagerScreen(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var downloadedCodes by remember { mutableStateOf(downloadedTranslationCodesSafe()) }
    var downloadingCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadableTranslations by remember { mutableStateOf<List<Translation>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (bibleState.translationVisibility.isEmpty()) {
            val allCodes = (Translation.embeddedTranslations.map { it.code } + downloadedCodes).distinct()
            val seeded = allCodes.associateWith { true }
            onStateChange(bibleState.copy(translationVisibility = seeded))
        }
    }

    LaunchedEffect(Unit) {
        logger.debug {"TranslationManagerScreen called, fetching downloadable translations list"}
        isLoadingList = true
        val listResult = runCatching {
            withTimeout(50_000) {
                assetManager().downloadableTranslationList(DOWNLOADABLE_BIBLE_LIST_URL)
            }
        }.fold(
            onSuccess = { translations ->
                translations.ifEmpty {
                    logger.debug { "TranslationManagerScreen Unable to load online list; showing embedded/cached only." }
                    latestDownloadableTranslations.ifEmpty { org.gnit.bible.downloadableTranslations }
                }
            },
            onFailure = { throwable ->
                logger.debug {
                    when (throwable) {
                        is TimeoutCancellationException -> "TranslationManagerScreen Timed out loading downloadable translations; showing embedded/cached only. (${throwable.message ?: "unknown"})"
                        else -> "TranslationManagerScreen Failed to load downloadable translations (${throwable.message ?: "unknown"})"
                    }
                }
                latestDownloadableTranslations.ifEmpty { org.gnit.bible.downloadableTranslations }
            }
        )
        downloadableTranslations = listResult
        latestDownloadableTranslations = listResult
        isLoadingList = false
    }

    val entries = remember(downloadedCodes, downloadingCodes, downloadableTranslations) {
        buildTranslationEntries(downloadedCodes, downloadableTranslations)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Translations", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (isLoadingList) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                items(entries, key = { it.translation.code }) { entry ->
                    TranslationManagerRow(
                        entry = entry,
                        isShown = bibleState.translationVisibility[entry.translation.code] ?: true,
                        isDownloading = downloadingCodes.contains(entry.translation.code),
                        onToggleVisibility = {
                            onStateChange(
                                bibleState.withTranslationVisibility(
                                    entry.translation.code,
                                    !(bibleState.translationVisibility[entry.translation.code] ?: true)
                                )
                            )
                        },
                        onDownload = {
                            downloadingCodes = downloadingCodes + entry.translation.code
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    val url = if (DOWNLOADABLE_BIBLE_BASE_URL.endsWith("/")) DOWNLOADABLE_BIBLE_BASE_URL else "$DOWNLOADABLE_BIBLE_BASE_URL/"
                                    val fileName = "${entry.translation.code}.zip"
                                    logger.debug {"download button tapped, start download ${entry.translation.code} url=${url}$fileName"}
                                    assetManager().download(url, fileName)
                                    logger.debug {"download success ${entry.translation.code} url=${url}$fileName"}
                                    withContext(Dispatchers.Main) {
                                        downloadedCodes = (downloadedCodes + entry.translation.code).distinct()
                                        onStateChange(
                                            bibleState.withTranslationVisibility(entry.translation.code, true)
                                        )
                                    }
                                }.onFailure {
                                    logger.debug {"download failed ${entry.translation.code}: ${it.message}"}
                                }
                                withContext(Dispatchers.Main) {
                                    downloadedCodes = downloadedTranslationCodesSafe()
                                    downloadingCodes = downloadingCodes - entry.translation.code
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                runCatching { assetManager().delete(entry.translation.code) }
                                downloadedCodes = downloadedTranslationCodesSafe()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TranslationManagerScreenPreview() {
    BibleTheme {
        TranslationManagerScreen(
            bibleState = BibleState(
                translationVisibility = mapOf(
                    Translation.webus.code to true,
                    Translation.kjv.code to true,
                    Translation.rvr09.code to false
                )
            ),
            onStateChange = {},
            onClose = {}
        )
    }
}

@Composable
private fun TranslationManagerRow(
    entry: TranslationEntry,
    isShown: Boolean,
    isDownloading: Boolean,
    onToggleVisibility: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val translation = entry.translation
    val displayName = if (translation.languageCode == Language.en.code) {
        translation.nativeName
    } else {
        "${translation.englishName} / ${translation.nativeName}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = translation.code.uppercase(), style = MaterialTheme.typography.titleMedium)
                Text(text = displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${translation.language.englishName} · ${translation.year} · ${translation.copyright}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TranslationManagerActionBar(
                source = entry.source,
                isShown = isShown,
                isDownloading = isDownloading,
                onToggleVisibility = onToggleVisibility,
                onDownload = onDownload,
                onDelete = onDelete
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun TranslationManagerActionBar(
    source: InstallationState,
    isShown: Boolean,
    isDownloading: Boolean,
    onToggleVisibility: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(ACTION_BAR_WIDTH.dp)
            .wrapContentWidth(Alignment.End),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (source) {
            InstallationState.EMBEDDED -> {
                ShowHideIcon(
                    isShown = isShown,
                    onToggle = onToggleVisibility
                )
            }

            InstallationState.DOWNLOADABLE -> {
                DownloadIcon(
                    isDownloading = isDownloading,
                    onDownload = onDownload
                )
            }

            InstallationState.DOWNLOADED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeleteIcon(onDelete = onDelete)
                    Spacer(modifier = Modifier.width(ACTION_ICON_SPACER.dp))
                    ShowHideIcon(
                        isShown = isShown,
                        onToggle = onToggleVisibility
                    )
                }
            }
        }
    }
}

private fun buildTranslationEntries(
    downloadedCodes: List<String>,
    downloadable: List<Translation>
): List<TranslationEntry> {
    val embedded = Translation.embeddedTranslations.map { TranslationEntry(it, InstallationState.EMBEDDED) }

    val downloadedTranslations = downloadedCodes.mapNotNull { code ->
        runCatching { bible().obtainZipBibleTextReader().getTranslationFromManifest(code) }.getOrNull()
    }.map { TranslationEntry(it, InstallationState.DOWNLOADED) }

    val list = downloadable.ifEmpty { latestDownloadableTranslations.ifEmpty { downloadableTranslations } }

    val notDownloaded = list.filterNot { candidate ->
        downloadedCodes.contains(candidate.code) || Translation.embeddedTranslations.any { it.code == candidate.code }
    }.map { TranslationEntry(it, InstallationState.DOWNLOADABLE) }

    return embedded + downloadedTranslations + notDownloaded
}

private fun downloadedTranslationCodesSafe(): List<String> =
    runCatching { assetManager().downloadedTranslationCodes() }.getOrElse { emptyList() }

private var latestDownloadableTranslations = emptyList<Translation>() // the list will be provided when TranslationManagerScreen() is called

private const val ACTION_BAR_WIDTH = 72
private const val ACTION_ICON_SPACER = 12

