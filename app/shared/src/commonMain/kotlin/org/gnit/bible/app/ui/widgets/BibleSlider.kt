package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.gnit.bible.app.ui.theme.BibleTheme
import kotlin.math.roundToInt

@Composable
fun BibleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    previewInteracting: Boolean = false
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val activeStopColor = MaterialTheme.colorScheme.onPrimary
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    var isInteracting by remember { mutableStateOf(false) }
    val thumbDiameter by animateFloatAsState(
        targetValue = if (isInteracting || previewInteracting) {
            SLIDER_ACTIVE_THUMB_DIAMETER
        } else {
            SLIDER_THUMB_DIAMETER
        },
        animationSpec = spring(),
        label = "BibleSliderThumbDiameter"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(SLIDER_TOUCH_HEIGHT.dp)
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput
                val thumbRadiusPx = SLIDER_THUMB_DIAMETER.dp.toPx() / 2f

                awaitEachGesture {
                    val down = awaitFirstDown()
                    try {
                        isInteracting = true
                        onValueChange(
                            valueForPosition(
                                x = down.position.x,
                                width = size.width.toFloat(),
                                thumbRadius = thumbRadiusPx,
                                valueRange = valueRange,
                                steps = steps
                            )
                        )

                        drag(down.id) { change ->
                            onValueChange(
                                valueForPosition(
                                    x = change.position.x,
                                    width = size.width.toFloat(),
                                    thumbRadius = thumbRadiusPx,
                                    valueRange = valueRange,
                                    steps = steps
                                )
                            )
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                        }
                    } finally {
                        isInteracting = false
                    }
                }
            }
    ) {
        val centerY = size.height / 2f
        val thumbRadius = thumbDiameter.dp.toPx() / 2f
        val restingThumbRadius = SLIDER_THUMB_DIAMETER.dp.toPx() / 2f
        val tickRadius = SLIDER_STOP_INDICATOR_DIAMETER.dp.toPx() / 2f
        val trackHeight = SLIDER_TRACK_HEIGHT.dp.toPx()
        val thumbTrackGap = SLIDER_THUMB_TRACK_GAP.dp.toPx()
        val trackStart = restingThumbRadius
        val trackEnd = size.width - restingThumbRadius
        if (trackEnd <= trackStart) return@Canvas

        val valueFraction = valueFraction(coercedValue, valueRange)
        val thumbCenter = Offset(
            x = trackStart + ((trackEnd - trackStart) * valueFraction),
            y = centerY
        )
        val color = if (enabled) activeColor else disabledColor
        val trackColor = if (enabled) inactiveColor else disabledColor
        val stopColor = if (enabled) activeStopColor else disabledColor
        val trackVisualEnd = (thumbCenter.x - thumbRadius - thumbTrackGap).coerceAtLeast(trackStart)
        val trackVisualStart = (thumbCenter.x + thumbRadius + thumbTrackGap).coerceAtMost(trackEnd)

        if (trackVisualEnd > trackStart) {
            drawRoundRect(
                color = color,
                topLeft = Offset(trackStart, centerY - (trackHeight / 2f)),
                size = Size(trackVisualEnd - trackStart, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )
        }

        if (trackEnd > trackVisualStart) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(trackVisualStart, centerY - (trackHeight / 2f)),
                size = Size(trackEnd - trackVisualStart, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )
        }

        if (steps > 0) {
            drawStopIndicators(
                steps = steps,
                centerY = centerY,
                trackStart = trackStart,
                trackEnd = trackEnd,
                thumbCenterX = thumbCenter.x,
                thumbRadius = thumbRadius,
                thumbTrackGap = thumbTrackGap,
                indicatorRadius = tickRadius,
                trackCornerRadius = trackHeight / 2f,
                activeColor = stopColor,
                inactiveColor = color
            )
        } else if (trackVisualEnd > trackStart) {
            drawLine(
                color = color,
                start = Offset(trackStart, centerY),
                end = Offset(trackVisualEnd, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = color,
            radius = thumbRadius,
            center = thumbCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawStopIndicators(
    steps: Int,
    centerY: Float,
    trackStart: Float,
    trackEnd: Float,
    thumbCenterX: Float,
    thumbRadius: Float,
    thumbTrackGap: Float,
    indicatorRadius: Float,
    trackCornerRadius: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val tickCount = steps + 2
    val lastIndex = tickCount - 1
    val beforeThumbEnd = thumbCenterX - thumbRadius - thumbTrackGap
    val afterThumbStart = thumbCenterX + thumbRadius + thumbTrackGap
    val indicatorStart = (trackStart + trackCornerRadius).coerceAtMost(trackEnd)
    val indicatorEnd = (trackEnd - trackCornerRadius).coerceAtLeast(trackStart)
    repeat(tickCount) { index ->
        val fraction = index.toFloat() / lastIndex.toFloat()
        val x = indicatorStart + ((indicatorEnd - indicatorStart) * fraction)
        val indicatorColor = when {
            x < beforeThumbEnd -> activeColor
            x > afterThumbStart -> inactiveColor
            else -> return@repeat
        }
        drawCircle(
            color = indicatorColor,
            radius = indicatorRadius,
            center = Offset(
                x = x,
                y = centerY
            )
        )
    }
}

private fun valueForPosition(
    x: Float,
    width: Float,
    thumbRadius: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Float {
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    if (width <= 0f || rangeEnd <= rangeStart) return rangeStart

    val thumbRadiusFraction = thumbRadius / width
    val usableEnd = 1f - thumbRadiusFraction
    val fraction = ((x / width) - thumbRadiusFraction) / (usableEnd - thumbRadiusFraction)
    val rawValue = rangeStart + ((rangeEnd - rangeStart) * fraction.coerceIn(0f, 1f))
    return snapValue(rawValue, valueRange, steps)
}

private fun valueFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>
): Float {
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    if (rangeEnd <= rangeStart) return 0f
    return ((value - rangeStart) / (rangeEnd - rangeStart)).coerceIn(0f, 1f)
}

private fun snapValue(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Float {
    if (steps <= 0) return value.coerceIn(valueRange.start, valueRange.endInclusive)
    val intervalCount = steps + 1
    val stepSize = (valueRange.endInclusive - valueRange.start) / intervalCount
    return (valueRange.start + (stepSize * ((value - valueRange.start) / stepSize).roundToInt()))
        .coerceIn(valueRange.start, valueRange.endInclusive)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 48)
@Composable
private fun BibleSliderMovedBookPreview() {
    BibleTheme {
        BibleSlider(
            value = 27f,
            onValueChange = {},
            steps = 64,
            valueRange = 1f..66f,
            modifier = Modifier.fillMaxWidth(),
            previewInteracting = true
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 48)
@Composable
private fun BibleSliderMovedChapterPreview() {
    BibleTheme {
        BibleSlider(
            value = 7f,
            onValueChange = {},
            steps = 11,
            valueRange = 1f..13f,
            modifier = Modifier.fillMaxWidth(),
            previewInteracting = true
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 48)
@Composable
private fun BibleSliderPreview() {
    BibleTheme {
        BibleSlider(
            value = 1f,
            onValueChange = {},
            steps = 64,
            valueRange = 1f..66f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 48)
@Composable
private fun BibleSliderMiddlePreview() {
    BibleTheme {
        BibleSlider(
            value = 33f,
            onValueChange = {},
            steps = 64,
            valueRange = 1f..66f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private const val SLIDER_TOUCH_HEIGHT = 32
private const val SLIDER_THUMB_DIAMETER = 14f
private const val SLIDER_ACTIVE_THUMB_DIAMETER = 10f
private const val SLIDER_STOP_INDICATOR_DIAMETER = 4f
private const val SLIDER_TRACK_HEIGHT = 8f
private const val SLIDER_THUMB_TRACK_GAP = 1f
