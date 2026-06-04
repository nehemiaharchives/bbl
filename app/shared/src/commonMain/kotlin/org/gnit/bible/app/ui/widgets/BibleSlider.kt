package org.gnit.bible.app.ui.widgets

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 *  for reference below is the source code from material 3 lib:
 *
 *```kotlin
 *
 *     fun Track(
 *         sliderState: SliderState,
 *         modifier: Modifier = Modifier,
 *         enabled: Boolean = true,
 *         colors: SliderColors = colors(),
 *         drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
 *             drawStopIndicator(
 *                 offset = it,
 *                 color = colors.trackColor(enabled, active = true),
 *                 size = TrackStopIndicatorSize,
 *             )
 *         },
 *         drawTick: DrawScope.(Offset, Color) -> Unit = { offset, color ->
 *             drawStopIndicator(offset = offset, color = color, size = TickSize)
 *         },
 *         thumbTrackGapSize: Dp = ThumbTrackGapSize,           //val ActiveHandleLeadingSpace = 6.0.dp
 *         trackInsideCornerSize: Dp = TrackInsideCornerSize,   //val TrackInsideCornerSize: Dp = 2.dp
 *     ) {
 *         TrackImpl(
 *             sliderState = sliderState,
 *             trackCornerSize = Dp.Unspecified,
 *             modifier = modifier,
 *             enabled = enabled,
 *             colors = colors,
 *             drawStopIndicator = drawStopIndicator,
 *             drawTick = drawTick,
 *             thumbTrackGapSize = thumbTrackGapSize,
 *             trackInsideCornerSize = trackInsideCornerSize,
 *             enableCornerShrinking = false,
 *             isCentered = false,
 *         )
 *     }
 *
 *
 * // Internal to be referred to in tests
 * internal val TrackHeight = SliderTokens.InactiveTrackHeight
 * internal val ThumbWidth = SliderTokens.HandleWidth            //val HandleWidth = 4.0.dp
 * private val ThumbHeight = SliderTokens.HandleHeight           //val HandleHeight = 44.0.dp
 * private val ThumbSize = DpSize(ThumbWidth, ThumbHeight)
 * private val VerticalThumbSize = DpSize(ThumbHeight, ThumbWidth)
 * private val ThumbTrackGapSize: Dp = SliderTokens.ActiveHandleLeadingSpace
 * private val TrackInsideCornerSize: Dp = 2.dp
 *
 *
 *     @Composable
 *     fun Thumb(
 *         interactionSource: MutableInteractionSource,
 *         modifier: Modifier = Modifier,
 *         colors: SliderColors = colors(),
 *         enabled: Boolean = true,
 *         thumbSize: DpSize = ThumbSize,
 *     ) {
 *         val interactions = remember { mutableStateListOf<Interaction>() }
 *         LaunchedEffect(interactionSource) {
 *             interactionSource.interactions.collect { interaction ->
 *                 when (interaction) {
 *                     is PressInteraction.Press -> interactions.add(interaction)
 *                     is PressInteraction.Release -> interactions.remove(interaction.press)
 *                     is PressInteraction.Cancel -> interactions.remove(interaction.press)
 *                     is DragInteraction.Start -> interactions.add(interaction)
 *                     is DragInteraction.Stop -> interactions.remove(interaction.start)
 *                     is DragInteraction.Cancel -> interactions.remove(interaction.start)
 *                 }
 *             }
 *         }
 *
 *         val size =
 *             if (interactions.isNotEmpty()) {
 *                 thumbSize.copy(width = thumbSize.width / 2)
 *             } else {
 *                 thumbSize
 *             }
 *         Spacer(
 *             modifier
 *                 .size(size)
 *                 .hoverable(interactionSource = interactionSource)
 *                 .background(colors.thumbColor(enabled), SliderTokens.HandleShape.value)
 *         )
 *     }
 *
 *    @OptIn(ExperimentalMaterial3Api::class)
 *    @Composable
 *    fun Slider(
 *        value: Float,
 *        onValueChange: (Float) -> Unit,
 *        modifier: Modifier = Modifier,
 *        enabled: Boolean = true,
 *        valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
 *        @IntRange(from = 0) steps: Int = 0,
 *        onValueChangeFinished: (() -> Unit)? = null,
 *        colors: SliderColors = SliderDefaults.colors(),
 *        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
 *    ) {
 *        Slider(
 *            value = value,
 *            onValueChange = onValueChange,
 *            modifier = modifier,
 *            enabled = enabled,
 *            onValueChangeFinished = onValueChangeFinished,
 *            colors = colors,
 *            interactionSource = interactionSource,
 *            steps = steps,
 *            thumb = {
 *                SliderDefaults.Thumb(
 *                    interactionSource = interactionSource,
 *                    colors = colors,
 *                    enabled = enabled,
 *                )
 *            },
 *            track = { sliderState ->
 *                SliderDefaults.Track(colors = colors, enabled = enabled, sliderState = sliderState)
 *            },
 *            valueRange = valueRange,
 *        )
 *    }
 *
 *
 *```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors: SliderColors = SliderDefaults.colors()
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    // Single vertical space used by both thumb container and track container
    val thumbDiameter = 14.dp
    val trackHeight = 8.dp

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onValueChangeFinished = null,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps,
        thumb = {
            // Center the thumb by centering its container; no .align() needed
            Box(
                modifier = Modifier.size(thumbDiameter),
                contentAlignment = Alignment.Center
            ) {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = enabled,
                    thumbSize = DpSize(width = thumbDiameter, height = thumbDiameter)
                )
            }
        },
        track = { sliderState: SliderState ->
            // Make the track share the same vertical space and center it
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbDiameter)
            ) {
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = (thumbDiameter - trackHeight) / 2),
                    enabled = enabled,
                    colors = colors,
                    thumbTrackGapSize = 1.dp,
                    trackInsideCornerSize = 5.dp
                )
            }
        },
        valueRange = valueRange
    )
}
