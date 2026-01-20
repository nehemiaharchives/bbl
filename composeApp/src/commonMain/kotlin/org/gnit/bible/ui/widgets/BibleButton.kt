package org.gnit.bible.ui.widgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnit.bible.BUTTON_CONTENT_PADDING
import org.gnit.bible.BUTTON_ROUND
import org.gnit.bible.BUTTON_SIZE
import org.gnit.bible.BUTTON_TEXT_FONT_SIZE
import org.gnit.bible.ui.theme.BibleTheme

@Composable
fun BibleButton(
    onClick:() -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String
){
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(BUTTON_ROUND.dp),
        contentPadding = PaddingValues(BUTTON_CONTENT_PADDING.dp),
        modifier = modifier
            .width(BUTTON_SIZE.dp)
            .height(BUTTON_SIZE.dp),
        content = {
            Text(
                text = buttonText,
                fontSize = BUTTON_TEXT_FONT_SIZE.sp,
                textAlign = TextAlign.Center
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BibleButtonPreview() {
    BibleTheme {
        BibleButton(
            onClick = {},
            buttonText = "+"
        )
    }
}