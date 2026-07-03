package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.gnit.bible.app.Res
import org.jetbrains.compose.resources.vectorResource
import org.gnit.bible.app.translation_delete
import org.gnit.bible.app.translation_download
import org.gnit.bible.app.translation_hide
import org.gnit.bible.app.translation_show

/**
 * Small reusable translation-related icons extracted from `App.kt`.
 * These accept an optional iconSize (in dp units) to avoid depending on private constants
 * in `App.kt`. Call sites can pass `ACTION_ICON_SIZE` to preserve previous sizing.
 */

@Composable
fun ShowHideIcon(
    isShown: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    iconSize: Int = 24,
    buttonSize: Int = 48
) {
    val icon = if (isShown) Res.drawable.translation_show else Res.drawable.translation_hide
    val description = if (isShown) "Shown" else "Hidden"
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isShown -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier.size(buttonSize.dp)
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = description,
            modifier = Modifier.size(iconSize.dp),
            tint = tint
        )
    }
}

@Composable
fun DownloadIcon(
    isDownloading: Boolean,
    onDownload: () -> Unit,
    iconSize: Int = 24,
    buttonSize: Int = 48
) {
    if (isDownloading) {
        Box(
            modifier = Modifier.size(buttonSize.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize.dp),
                strokeWidth = 2.5.dp
            )
        }
    } else {
        IconButton(
            onClick = onDownload,
            modifier = Modifier.size(buttonSize.dp)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.translation_download),
                contentDescription = "Download",
                modifier = Modifier.size((iconSize - 4).dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DeleteIcon(
    onDelete: () -> Unit,
    iconSize: Int = 24,
    buttonSize: Int = 48
) {
    IconButton(
        onClick = onDelete,
        modifier = Modifier.size(buttonSize.dp)
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.translation_delete),
            contentDescription = "Delete",
            modifier = Modifier.size((iconSize - 4).dp),
            tint = MaterialTheme.colorScheme.error
        )
    }
}
