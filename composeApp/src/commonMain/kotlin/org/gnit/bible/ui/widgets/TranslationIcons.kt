package org.gnit.bible.ui.widgets

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.gnit.bible.cmp.Res
import org.jetbrains.compose.resources.vectorResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import org.gnit.bible.cmp.translation_delete
import org.gnit.bible.cmp.translation_download
import org.gnit.bible.cmp.translation_hide
import org.gnit.bible.cmp.translation_show

/**
 * Small reusable translation-related icons extracted from `App.kt`.
 * These accept an optional iconSize (in dp units) to avoid depending on private constants
 * in `App.kt`. Call sites can pass `ACTION_ICON_SIZE` to preserve previous sizing.
 */

@Composable
fun ShowHideIcon(
    isShown: Boolean,
    onToggle: () -> Unit,
    iconSize: Int = 24
) {
    val icon = if (isShown) Res.drawable.translation_show else Res.drawable.translation_hide
    val description = if (isShown) "Shown" else "Hidden"
    val tint = when {
        isShown -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    Icon(
        imageVector = vectorResource(icon),
        contentDescription = description,
        modifier = Modifier
            .size(iconSize.dp)
            .clickable { onToggle() },
        tint = tint
    )
}

@Composable
fun DownloadIcon(
    isDownloading: Boolean,
    onDownload: () -> Unit,
    iconSize: Int = 24
) {
    if (isDownloading) {
        CircularProgressIndicator(
            modifier = Modifier.size(iconSize.dp),
            strokeWidth = 2.5.dp
        )
    } else {
        Icon(
            imageVector = vectorResource(Res.drawable.translation_download),
            contentDescription = "Download",
            modifier = Modifier
                .size((iconSize - 4).dp)
                .clickable { onDownload() },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DeleteIcon(
    onDelete: () -> Unit,
    iconSize: Int = 24
) {
    Icon(
        imageVector = vectorResource(Res.drawable.translation_delete),
        contentDescription = "Delete",
        modifier = Modifier
            .size((iconSize - 4).dp)
            .clickable { onDelete() },
        tint = MaterialTheme.colorScheme.error
    )
}

