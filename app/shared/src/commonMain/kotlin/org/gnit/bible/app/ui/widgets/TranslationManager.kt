package org.gnit.bible.app.ui.widgets

import org.gnit.bible.SupportedTranslation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gnit.bible.BblVersion
import org.gnit.bible.TranslationEntry
import org.gnit.bible.InstallationState
import org.gnit.bible.Language
import org.gnit.bible.Translation
import org.gnit.bible.app.currentAssetManager
import org.gnit.bible.app.currentBible
import org.gnit.bible.app.logger
import org.gnit.bible.app.state.BibleState
import org.gnit.bible.app.state.withTranslationVisibility
import androidx.compose.ui.tooling.preview.Preview
import org.gnit.bible.app.ui.theme.BibleTheme

@Composable
fun TranslationManagerScreen(
    bibleState: BibleState,
    onStateChange: (BibleState) -> Unit,
    onClose: () -> Unit
) {
    val assetManager = currentAssetManager()
    val bible = currentBible()
    val scope = rememberCoroutineScope()
    var downloadedCodes by remember(assetManager) { mutableStateOf(downloadedTranslationCodesSafe(assetManager)) }
    var downloadingCodes by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        if (bibleState.translationVisibility.isEmpty()) {
            val allCodes = (SupportedTranslation.embeddedTranslations.map { it.code } + downloadedCodes).distinct()
            val seeded = allCodes.associateWith { true }
            onStateChange(bibleState.copy(translationVisibility = seeded))
        }
    }

    val downloadableList = SupportedTranslation.all
    val entries = remember(bible, downloadedCodes, downloadingCodes) {
        buildTranslationEntries(bible, downloadedCodes, downloadableList)
    }
    val hideableEntries = entries.filter { it.source != InstallationState.DOWNLOADABLE }
    val shownTranslationCount = hideableEntries.count {
        bibleState.translationVisibility[it.translation.code] ?: true
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.translation.code }) { entry ->
                    val isShown = bibleState.translationVisibility[entry.translation.code] ?: true
                    TranslationManagerRow(
                        entry = entry,
                        isShown = isShown,
                        canToggleVisibility = !isShown || shownTranslationCount > 1,
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
                                    val url = "${BblVersion.RELEASE_DOWNLOAD_URL.trimEnd('/')}/"
                                    val fileName = "${entry.translation.code}.zip"
                                    logger.debug {"download button tapped, start download ${entry.translation.code} url=${url}$fileName"}
                                    assetManager.download(url, fileName)
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
                                    downloadedCodes = downloadedTranslationCodesSafe(assetManager)
                                    downloadingCodes = downloadingCodes - entry.translation.code
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                runCatching { assetManager.delete(entry.translation.code) }
                                downloadedCodes = downloadedTranslationCodesSafe(assetManager)
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
                    SupportedTranslation.WEBUS.translation.code to true,
                    SupportedTranslation.KJV.translation.code to true,
                    SupportedTranslation.RVR09.translation.code to false
                )
            ),
            onStateChange = {},
            onClose = {}
        )
    }
}

@Preview
@Composable
private fun TranslationManagerIconRowsPreview() {
    BibleTheme {
        Surface {
            Column {
                TranslationManagerRow(
                    entry = TranslationEntry(
                        SupportedTranslation.WEBUS.translation,
                        InstallationState.EMBEDDED
                    ),
                    isShown = true,
                    canToggleVisibility = true,
                    isDownloading = false,
                    onToggleVisibility = {},
                    onDownload = {},
                    onDelete = {}
                )
                TranslationManagerRow(
                    entry = TranslationEntry(
                        SupportedTranslation.AYT.translation,
                        InstallationState.DOWNLOADED
                    ),
                    isShown = true,
                    canToggleVisibility = true,
                    isDownloading = false,
                    onToggleVisibility = {},
                    onDownload = {},
                    onDelete = {}
                )
            }
        }
    }
}

@Composable
private fun TranslationManagerRow(
    entry: TranslationEntry,
    isShown: Boolean,
    canToggleVisibility: Boolean,
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
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 72.dp)
        .padding(vertical = 8.dp)
        .let { modifier ->
            if (entry.source == InstallationState.DOWNLOADABLE && !isDownloading) {
                modifier.clickable(onClick = onDownload)
            } else {
                modifier
            }
        }

    Column(
        modifier = rowModifier
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
                canToggleVisibility = canToggleVisibility,
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
    canToggleVisibility: Boolean,
    isDownloading: Boolean,
    onToggleVisibility: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.width(ACTION_BAR_WIDTH.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (source) {
            InstallationState.EMBEDDED -> {
                Spacer(modifier = Modifier.width((ACTION_BUTTON_WIDTH + ACTION_ICON_SPACER).dp))
                ShowHideIcon(
                    isShown = isShown,
                    enabled = canToggleVisibility,
                    onToggle = onToggleVisibility,
                    buttonSize = ACTION_BUTTON_WIDTH
                )
            }

            InstallationState.DOWNLOADABLE -> {
                Spacer(modifier = Modifier.width((ACTION_BUTTON_WIDTH + ACTION_ICON_SPACER).dp))
                DownloadIcon(
                    isDownloading = isDownloading,
                    onDownload = onDownload,
                    buttonSize = ACTION_BUTTON_WIDTH
                )
            }

            InstallationState.DOWNLOADED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeleteIcon(
                        onDelete = onDelete,
                        buttonSize = ACTION_BUTTON_WIDTH
                    )
                    Spacer(modifier = Modifier.width(ACTION_ICON_SPACER.dp))
                    ShowHideIcon(
                        isShown = isShown,
                        enabled = canToggleVisibility,
                        onToggle = onToggleVisibility,
                        buttonSize = ACTION_BUTTON_WIDTH
                    )
                }
            }
        }
    }
}

private fun buildTranslationEntries(
    bible: org.gnit.bible.Bible,
    downloadedCodes: List<String>,
    downloadable: List<Translation>
): List<TranslationEntry> {
    val embeddedCodeSet = SupportedTranslation.embeddedTranslations.map { it.code }.toSet()
    val downloadedCodeSet = downloadedCodes.toSet()
    val zipBibleResourcesReader = bible.obtainZipBibleResourcesReader()
    val downloadedTranslationsByCode = downloadedCodes.mapNotNull { code ->
        runCatching { zipBibleResourcesReader.getTranslationFromManifest(code) }.getOrNull()
    }.associateBy { it.code }

    val catalogCodes = downloadable.map { it.code }.toSet()
    val catalogEntries = downloadable.map { translation ->
        val source = when (translation.code) {
            in embeddedCodeSet -> InstallationState.EMBEDDED
            in downloadedCodeSet -> InstallationState.DOWNLOADED
            else -> InstallationState.DOWNLOADABLE
        }

        TranslationEntry(
            translation = downloadedTranslationsByCode[translation.code] ?: translation,
            source = source
        )
    }

    val extraDownloadedEntries = downloadedCodes.filterNot { it in catalogCodes }.mapNotNull { code ->
        downloadedTranslationsByCode[code]?.let { TranslationEntry(it, InstallationState.DOWNLOADED) }
    }

    return catalogEntries + extraDownloadedEntries
}

private fun downloadedTranslationCodesSafe(assetManager: org.gnit.bible.AssetManager): List<String> =
    runCatching { assetManager.downloadedTranslationCodes() }.getOrElse { emptyList() }

private const val ACTION_BAR_WIDTH = 96
private const val ACTION_BUTTON_WIDTH = 42
private const val ACTION_ICON_SPACER = 2