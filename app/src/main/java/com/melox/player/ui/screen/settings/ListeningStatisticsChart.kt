package com.melox.player.ui.screen.settings

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.playback.ListeningRange
import com.melox.player.data.playback.recordedDayAverage
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val ChartDeceleration = Easing { 1f - (1f - it) * (1f - it) }

@Composable
internal fun ListeningStatisticsChart(
    range: ListeningRange,
    start: LocalDate,
    today: LocalDate,
    values: List<Long>,
    locale: Locale,
    chartWidth: Dp,
    headerSpace: Dp,
    cardTopPadding: Dp,
    cardHorizontalPadding: Dp,
    onPromptVisible: (Boolean) -> Unit,
    formatAxis: (Long) -> String,
    formatValue: (Long) -> String,
    promptLabel: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val color = MiuixTheme.colorScheme
    val axisStyle = MiuixTheme.textStyles.footnote2
    val darkSurface = color.surface.luminance() < 0.5f
    val promptNeutral = if (darkSurface) Color.White else Color.Black
    val promptDateStyle = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)
    val promptValueStyle = MiuixTheme.textStyles.main.copy(fontWeight = FontWeight.Medium)
    val text = rememberTextMeasurer()
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentValues by rememberUpdatedState(values)
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val nonZeroCount = values.count { it > 0 }
    val average = if (range != ListeningRange.DAY && nonZeroCount > 1) {
        values.recordedDayAverage()
    } else null
    val animation = remember { Animatable(0f) }
    LaunchedEffect(nonZeroCount > 0) {
        if (nonZeroCount > 0) {
            animation.animateTo(1f, tween(500, easing = ChartDeceleration))
        }
    }

    var selected by remember { mutableIntStateOf(-1) }
    var pressed by remember { mutableStateOf(false) }
    var promptVisible by remember { mutableStateOf(false) }
    var promptDismissedImmediately by remember { mutableStateOf(false) }
    val onPromptChanged by rememberUpdatedState(onPromptVisible)
    LaunchedEffect(promptVisible) { onPromptChanged(promptVisible) }
    LaunchedEffect(selected, pressed, promptVisible) {
        if (promptVisible && !pressed) {
            delay(1_600L)
            promptVisible = false
        }
    }
    val promptOpacity = remember { Animatable(0f) }
    LaunchedEffect(promptVisible, promptDismissedImmediately) {
        if (!promptVisible && promptDismissedImmediately) {
            promptOpacity.snapTo(0f)
        } else {
            promptOpacity.animateTo(if (promptVisible) 1f else 0f, tween(500, easing = ChartDeceleration))
        }
    }
    val selectedValue = values.getOrNull(selected)?.takeIf { it > 0 }
    val shortDate = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "Md"), locale) }
    val weekday = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val hour = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val todayLabel = stringResource(R.string.listening_statistics_today)
    val axisLabels = values.indices.map { index ->
        if (range == ListeningRange.DAY) {
            hour.format(LocalTime.of(index, 0))
        } else {
            val date = start.plusDays(index.toLong())
            when {
                date == today -> todayLabel
                range == ListeningRange.WEEK -> weekday.format(date)
                else -> shortDate.format(date)
            }
        }
    }
    fun bucketLabel(index: Int): String {
        val label = axisLabels[index]
        return promptLabel(
            if (range == ListeningRange.DAY) "$label-${axisLabels[(index + 1) % axisLabels.size]}"
            else label,
        )
    }
    fun showPrompt(index: Int) {
        selected = index
        promptDismissedImmediately = false
        promptVisible = true
    }

    val promptY = with(density) { (16.dp - cardTopPadding).toPx() }
    val horizontalInset = with(density) { 16.dp.toPx() }
    val promptEdge = with(density) { (16.dp - cardHorizontalPadding).coerceAtLeast(0.dp).toPx() }
    val verticalInset = with(density) { 12.dp.toPx() }
    val textGap = with(density) { 2.dp.toPx() }
    val promptConstraints = Constraints(
        maxWidth = (with(density) { chartWidth.toPx() } - 2 * (horizontalInset + promptEdge)).toInt().coerceAtLeast(1),
    )
    val promptTexts = values.mapIndexed { index, value ->
        if (value > 0) bucketLabel(index) to formatValue(value) else null
    }
    val promptLayouts = remember(text, promptTexts, promptConstraints, promptDateStyle, promptValueStyle) {
        promptTexts.map { strings ->
            strings?.let { (date, value) ->
                text.measure(date, promptDateStyle, constraints = promptConstraints) to
                    text.measure(value, promptValueStyle, constraints = promptConstraints)
            }
        }
    }
    val tallestPromptText = promptLayouts.maxOfOrNull { layout ->
        layout?.let { (date, value) -> date.size.height + value.size.height } ?: 0
    } ?: 0
    val reservedHeader = if (tallestPromptText > 0) {
        maxOf(headerSpace, with(density) { (promptY + tallestPromptText + textGap + 2 * verticalInset).toDp() })
    } else headerSpace
    val chartStart = maxOf(headerSpace + 22.dp, reservedHeader + 14.dp)

    val tickMax = values.maxOrNull() ?: 0L
    val ticks = listOf(tickMax, tickMax / 2, 0L)
        .map { it to formatAxis(it) }
        .distinctBy { (_, label) -> label }
        .asReversed()
        .map { (value, label) -> value to text.measure(label, axisStyle) }
    val averageText = if (average != null) {
        text.measure(stringResource(R.string.listening_statistics_average), axisStyle.copy(fontWeight = FontWeight.Medium))
    } else null
    val gutter = maxOf(ticks.maxOf { it.second.size.width }, averageText?.size?.width ?: 0) +
        with(density) { 8.dp.toPx() }
    val axisLabelHeight = text.measure("00:00", axisStyle).size.height.toFloat()
    val halfYAxisLabelHeight = axisLabelHeight / 2f
    val top = with(density) { chartStart.toPx() } + halfYAxisLabelHeight
    val bottomSpace = axisLabelHeight + halfYAxisLabelHeight
    val previousLabel = stringResource(R.string.listening_statistics_previous_bar)
    val nextLabel = stringResource(R.string.listening_statistics_next_bar)
    val chartLabel = stringResource(R.string.listening_statistics_chart)
    val axisDivisor = when (range) {
        ListeningRange.DAY -> 6
        ListeningRange.WEEK -> 1
        ListeningRange.MONTH -> 7
    }
    val axisIndices = values.indices.filter { it % axisDivisor == 0 || it == values.lastIndex }

    val touchGutter by rememberUpdatedState(gutter)
    val touchTop by rememberUpdatedState(top)
    fun indexAt(x: Float, width: Float): Int {
        val plotWidth = width - touchGutter
        if (plotWidth <= 0f) return -1
        val position = (if (rtl) width - x else x) / plotWidth
        if (position !in 0f..<1f) return -1
        val slot = position * currentValues.size
        val index = slot.toInt()
        return if (abs(slot - index - 0.5f) <= 0.44f && currentValues[index] > 0) index else -1
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(chartStart + 235.dp)
            .semantics {
                contentDescription = if (promptVisible && selectedValue != null) {
                    "${bucketLabel(selected)}, ${formatValue(selectedValue)}"
                } else chartLabel
                if (nonZeroCount > 0) {
                    customActions = listOf(
                        CustomAccessibilityAction(previousLabel) {
                            val index = values.indices.lastOrNull { it < selected && values[it] > 0 }
                                ?: values.indexOfLast { it > 0 }
                            showPrompt(index)
                            true
                        },
                        CustomAccessibilityAction(nextLabel) {
                            val index = values.indices.firstOrNull { it > selected && values[it] > 0 }
                                ?: values.indexOfFirst { it > 0 }
                            showPrompt(index)
                            true
                        },
                    )
                }
            }
            .pointerInput(rtl) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val hit = if (down.position.y in touchTop..size.height.toFloat()) {
                        indexAt(down.position.x, size.width.toFloat())
                    } else -1
                    if (hit < 0) {
                        promptDismissedImmediately = true
                        promptVisible = false
                        return@awaitEachGesture
                    }
                    down.consume()
                    pressed = true
                    showPrompt(hit)
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed || change.isConsumed) break
                            if (change.position.y in touchTop..size.height.toFloat()) {
                                val index = indexAt(change.position.x, size.width.toFloat())
                                if (index >= 0 && index != selected) showPrompt(index)
                            }
                            change.consume()
                        }
                    } finally {
                        pressed = false
                    }
                }
            },
    ) {
        val width = size.width - gutter
        val bottom = size.height - bottomSpace
        val height = bottom - top
        if (width <= 0f || height <= 0f) return@Canvas
        val left = if (rtl) gutter else 0f
        val right = left + width
        val slot = width / values.size
        val halfBar = slot * (1.5625f / (2f * (1f + 1.5625f)))
        fun center(index: Int) = if (rtl) right - slot * (index + 0.5f) else left + slot * (index + 0.5f)
        fun groupBoundary(index: Int) = if (rtl) right - index * slot else left + index * slot
        fun y(value: Double) = bottom - height * (value / maxValue).toFloat()
        val axisTextTop = bottom + halfYAxisLabelHeight
        val guideBottom = axisTextTop + axisLabelHeight
        val barCorner = 4.dp.toPx()
        fun barHeight(value: Long) = height.coerceAtLeast(0f) *
            (value.toDouble() / maxValue).toFloat() * animation.value
        val grid = if (darkSurface) Color(0x33FFFFFF) else Color(0x1A000000)
        val axis = if (darkSurface) color.onSurfaceVariantSummary else color.onSurfaceVariantActions
        val gridStroke = 0.5.dp.toPx()
        val dash = 2.dp.toPx()
        val verticalDash = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
        val verticalGrid = if (darkSurface) Color(0x33FFFFFF) else Color(0x4D000000)
        for (boundary in 0..values.size) {
            if (boundary % axisDivisor != 0 && boundary != values.size) continue
            val x = groupBoundary(boundary)
            drawLine(verticalGrid, Offset(x, top), Offset(x, guideBottom), gridStroke, pathEffect = verticalDash)
        }
        val averageY = average?.let(::y)
        for ((value, label) in ticks) {
            val lineY = y(value.toDouble())
            drawLine(grid, Offset(left, lineY), Offset(right, lineY), gridStroke)
            if (averageY == null || averageText == null ||
                abs(averageY - lineY) >= (averageText.size.height + label.size.height) / 2f
            ) {
                val labelX = if (rtl) left - 8.dp.toPx() - label.size.width else right + 8.dp.toPx()
                drawText(label, color = axis, topLeft = Offset(labelX, lineY - label.size.height / 2f))
            }
        }
        val barPath = Path()
        values.forEachIndexed { index, value ->
            if (value <= 0 || animation.value <= 0f) return@forEachIndexed
            val cx = center(index)
            val barHeight = barHeight(value)
            val radius = CornerRadius(minOf(barCorner, halfBar, barHeight))
            barPath.rewind()
            barPath.addRoundRect(
                RoundRect(
                    rect = Rect(cx - halfBar, bottom - barHeight, cx + halfBar, bottom),
                    topLeft = radius,
                    topRight = radius,
                ),
            )
            drawPath(barPath, color.primary)
        }
        if (averageY != null && averageText != null) {
            drawLine(
                color.primary.copy(alpha = 0.7f), Offset(left, averageY), Offset(right, averageY),
                strokeWidth = 2.dp.toPx(),
            )
            val labelX = if (rtl) left - 8.dp.toPx() - averageText.size.width else right + 8.dp.toPx()
            drawText(
                averageText,
                color = if (darkSurface) axis else color.onSurface,
                topLeft = Offset(labelX, averageY - averageText.size.height / 2f),
            )
        }
        val axisGap = 4.dp.toPx()
        val guideClearance = 2.dp.toPx()
        var lastLabelEnd = -axisGap
        for (index in if (rtl) axisIndices.asReversed() else axisIndices) {
            val label = text.measure(axisLabels[index], axisStyle)
            val groupStart = index - index % axisDivisor
            val groupEnd = minOf(groupStart + axisDivisor, values.size)
            val startX = groupBoundary(groupStart)
            val endX = groupBoundary(groupEnd)
            val minX = minOf(startX, endX) + guideClearance
            val maxX = maxOf(startX, endX) - guideClearance - label.size.width
            if (minX > maxX) continue
            // Daily and monthly labels follow the leading edge of their bar group, mirrored in RTL.
            val labelX = when {
                axisDivisor == 1 -> center(index) - label.size.width / 2f
                index == values.lastIndex -> if (rtl) minX else maxX
                rtl -> center(index) + halfBar - label.size.width
                else -> center(index) - halfBar
            }.coerceIn(minX, maxX)
            if (labelX >= lastLabelEnd + axisGap) {
                drawText(label, color = axis, topLeft = Offset(labelX, axisTextTop))
                lastLabelEnd = labelX + label.size.width
            }
        }
        val selectedPrompt = promptLayouts.getOrNull(selected)
        val promptAlpha = promptOpacity.value
        if (selectedValue != null && selectedPrompt != null && promptAlpha > 0f) {
            val (label, value) = selectedPrompt
            val cx = center(selected)
            val promptWidth = maxOf(value.size.width, label.size.width) + 2 * horizontalInset
            val promptHeight = label.size.height + textGap + value.size.height + 2 * verticalInset
            val promptBottom = promptY + promptHeight
            val promptX = (cx - promptWidth / 2f)
                .coerceIn(promptEdge, (size.width - promptWidth - promptEdge).coerceAtLeast(promptEdge))
            val barTop = bottom - barHeight(selectedValue)
            drawLine(
                promptNeutral.copy(alpha = 0.2f * promptAlpha), Offset(cx, promptBottom),
                Offset(cx, maxOf(promptBottom, barTop)), 1.dp.toPx(),
            )
            drawRoundRect(
                promptNeutral.copy(alpha = (15f / 255f) * promptAlpha),
                topLeft = Offset(promptX, promptY),
                size = Size(promptWidth, promptHeight),
                cornerRadius = CornerRadius(12.dp.toPx()),
            )
            val labelY = promptY + verticalInset
            val textX = if (rtl) promptX + promptWidth - horizontalInset else promptX + horizontalInset
            val promptDateColor = if (darkSurface) color.onSurfaceVariantSummary
            else promptNeutral.copy(alpha = 102f / 255f)
            drawText(
                label,
                promptDateColor.copy(alpha = promptDateColor.alpha * promptAlpha),
                Offset(if (rtl) textX - label.size.width else textX, labelY),
            )
            drawText(
                value,
                promptNeutral.copy(alpha = (if (darkSurface) 242f else 255f) / 255f * promptAlpha),
                Offset(if (rtl) textX - value.size.width else textX, labelY + label.size.height + textGap),
            )
        }
    }
}
