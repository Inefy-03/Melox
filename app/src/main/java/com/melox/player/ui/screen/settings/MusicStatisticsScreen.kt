package com.melox.player.ui.screen.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.os4.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.MusicLibraryStatistics
import com.melox.player.data.library.buildMusicLibraryStatistics
import com.melox.player.model.AudioQuality
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.library.MusicLibraryEmptyMessage
import java.util.Locale
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private enum class StatisticsGroup {
    QUALITY,
    FORMAT,
}

private enum class StatisticsMetric {
    COUNT,
    SIZE,
}

private data class CylinderStatisticItem(
    val id: String,
    val label: String,
    val valueText: String,
    val weight: Long,
    val color: Color,
)

private data class CylinderSegment(
    val index: Int,
    val top: Float,
    val bottom: Float,
) {
    val centerY: Float
        get() = (top + bottom) / 2f
}

@Composable
fun MusicStatisticsScreen(
    tracks: List<MusicTrack>,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
) {
    val statistics = remember(tracks) { buildMusicLibraryStatistics(tracks) }
    val layoutDirection = LocalLayoutDirection.current
    var group by remember { mutableStateOf(StatisticsGroup.QUALITY) }
    var metric by remember { mutableStateOf(StatisticsMetric.COUNT) }
    val scrollBehavior = MiuixScrollBehavior()
    var topBarBackdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topBarBackdrop = rememberBlurBackdrop(captureCoordinates = { topBarBackdropCaptureCoordinates })

    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = topBarBackdrop,
                blurEnabled = topBarBackdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.music_statistics_page_title),
                    color = topBarBackdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = navigationBackIcon(),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        StatisticsTuneButton(
                            group = group,
                            metric = metric,
                            onGroupChange = { group = it },
                            onMetricChange = { metric = it },
                        )
                    },
                )
            }
        },
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.onGloballyPositioned { coordinates -> topBarBackdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            StatisticsPage(
                statistics = statistics,
                group = group,
                metric = metric,
                startPadding = padding.calculateStartPadding(layoutDirection),
                topPadding = padding.calculateTopPadding(),
                endPadding = padding.calculateEndPadding(layoutDirection),
                bottomPadding = maxOf(
                    padding.calculateBottomPadding(),
                    bottomContentPadding,
                ),
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

@Composable
private fun StatisticsTuneButton(
    group: StatisticsGroup,
    metric: StatisticsMetric,
    onGroupChange: (StatisticsGroup) -> Unit,
    onMetricChange: (StatisticsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPopup by remember { mutableStateOf(false) }
    val qualityLabel = stringResource(R.string.music_statistics_group_quality)
    val formatLabel = stringResource(R.string.music_statistics_group_format)
    val countLabel = stringResource(R.string.music_statistics_metric_count)
    val sizeLabel = stringResource(R.string.music_statistics_metric_size)

    if (LocalOs4Style.current) {
        OverlayIconDropdownMenu(
            modifier = modifier,
            entries = listOf(
                DropdownEntry(listOf(
                    DropdownItem(text = qualityLabel, selected = group == StatisticsGroup.QUALITY,
                        onClick = { onGroupChange(StatisticsGroup.QUALITY) }),
                    DropdownItem(text = formatLabel, selected = group == StatisticsGroup.FORMAT,
                        onClick = { onGroupChange(StatisticsGroup.FORMAT) }),
                )),
                DropdownEntry(listOf(
                    DropdownItem(text = countLabel, selected = metric == StatisticsMetric.COUNT,
                        onClick = { onMetricChange(StatisticsMetric.COUNT) }),
                    DropdownItem(text = sizeLabel, selected = metric == StatisticsMetric.SIZE,
                        onClick = { onMetricChange(StatisticsMetric.SIZE) }),
                )),
            ),
            collapseOnSelection = true,
        ) {
            Icon(MiuixIcons.Tune, stringResource(R.string.music_statistics_tune_action), Modifier.size(24.dp))
        }
        return
    }
    Box(modifier = modifier) {
        OverlayListPopup(
            show = showPopup,
            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showPopup = false },
        ) {
            ListPopupColumn {
                listOf(
                    StatisticsGroup.QUALITY to qualityLabel,
                    StatisticsGroup.FORMAT to formatLabel,
                ).forEachIndexed { index, (option, label) ->
                    DropdownImpl(
                        text = label,
                        optionSize = STATISTICS_OPTION_COUNT,
                        isSelected = group == option,
                        index = index,
                        onSelectedIndexChange = {
                            onGroupChange(option)
                            showPopup = false
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    thickness = 1.5.dp,
                )

                listOf(
                    StatisticsMetric.COUNT to countLabel,
                    StatisticsMetric.SIZE to sizeLabel,
                ).forEachIndexed { index, (option, label) ->
                    DropdownImpl(
                        text = label,
                        optionSize = STATISTICS_OPTION_COUNT,
                        isSelected = metric == option,
                        index = index + 2,
                        onSelectedIndexChange = {
                            onMetricChange(option)
                            showPopup = false
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = { showPopup = true },
            holdDownState = showPopup,
        ) {
            Icon(
                imageVector = MiuixIcons.Tune,
                contentDescription = stringResource(R.string.music_statistics_tune_action),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun StatisticsPage(
    statistics: MusicLibraryStatistics,
    group: StatisticsGroup,
    metric: StatisticsMetric,
    startPadding: Dp,
    topPadding: Dp,
    endPadding: Dp,
    bottomPadding: Dp,
    scrollBehavior: ScrollBehavior,
) {
    val darkSurface = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val otherLabel = stringResource(R.string.music_statistics_other)
    val useSize = metric == StatisticsMetric.SIZE
    val items = when (group) {
        StatisticsGroup.QUALITY -> statistics.quality.map { qualityGroup ->
            val quality = qualityGroup.quality
            CylinderStatisticItem(
                id = quality?.name ?: "quality_other",
                label = when (quality) {
                    AudioQuality.RAW -> stringResource(R.string.music_quality_raw)
                    AudioQuality.HI_RES -> stringResource(R.string.music_quality_hi_res)
                    AudioQuality.SQ -> stringResource(R.string.music_quality_sq)
                    AudioQuality.HQ -> stringResource(R.string.music_quality_hq)
                    null -> otherLabel
                },
                valueText = if (useSize) {
                    formatStatisticsSize(qualityGroup.totalBytes)
                } else {
                    pluralStringResource(
                        R.plurals.music_statistics_song_count,
                        qualityGroup.trackCount,
                        qualityGroup.trackCount,
                    )
                },
                weight = if (useSize) {
                    qualityGroup.totalBytes
                } else {
                    qualityGroup.trackCount.toLong()
                },
                color = qualityColor(quality, darkSurface),
            )
        }

        StatisticsGroup.FORMAT -> {
            val formats = if (useSize) statistics.formatsBySize else statistics.formats
            formats.mapIndexed { index, formatGroup ->
                CylinderStatisticItem(
                    id = formatGroup.format ?: "format_other",
                    label = formatGroup.format ?: otherLabel,
                    valueText = if (useSize) {
                        formatStatisticsSize(formatGroup.totalBytes)
                    } else {
                        pluralStringResource(
                            R.plurals.music_statistics_song_count,
                            formatGroup.trackCount,
                            formatGroup.trackCount,
                        )
                    },
                    weight = if (useSize) {
                        formatGroup.totalBytes
                    } else {
                        formatGroup.trackCount.toLong()
                    },
                    color = formatGroup.format?.let { formatColor(index, darkSurface) }
                        ?: qualityColor(null, darkSurface),
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = startPadding,
                top = topPadding + 32.dp,
                end = endPadding,
                bottom = bottomPadding + 16.dp,
            ),
            overscrollEffect = null,
        ) {
            item(key = "${group.name}_${metric.name}") {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CYLINDER_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusicLibraryEmptyMessage(
                            icon = MiuixIcons.Music,
                            text = stringResource(R.string.music_empty_after_scan),
                        )
                    }
                } else {
                    CylinderStatistics(
                        items = items,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CYLINDER_HEIGHT),
                    )
                }
            }
        }
    }
}

@Composable
private fun CylinderStatistics(
    items: List<CylinderStatisticItem>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var selectedIndex by remember(items) { mutableIntStateOf(-1) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var chartOrigin by remember { mutableStateOf(Offset.Zero) }
    val legendCenters = remember(items) { mutableStateMapOf<Int, Offset>() }
    val lineProgress = remember { Animatable(0f) }
    val groupWidthPx = min(
        chartSize.width * CHART_GROUP_WIDTH_RATIO,
        chartSize.height * CHART_GROUP_HEIGHT_RATIO,
    )
    val cylinderWidthPx = groupWidthPx * CYLINDER_WIDTH_SHARE
    val legendGapPx = groupWidthPx * LEGEND_GAP_SHARE
    val legendWidthPx = groupWidthPx * LEGEND_WIDTH_SHARE
    val legendWidth = with(density) { legendWidthPx.toDp() }
    val groupLeftPx = ((chartSize.width - groupWidthPx) / 2f).coerceAtLeast(0f)
    val cylinderBodyHeight = (
        with(density) { CYLINDER_HEIGHT.toPx() } -
            cylinderWidthPx * ELLIPSE_HEIGHT_RATIO
    ).coerceAtLeast(items.size.toFloat())
    val cylinderTop = ((chartSize.height - cylinderBodyHeight) / 2f).coerceAtLeast(0f)
    val legendTopPx = (
        cylinderTop - cylinderWidthPx * ELLIPSE_HEIGHT_RATIO / 2f
    ).coerceAtLeast(0f)
    val geometry = remember(items, chartSize, density) {
        calculateCylinderGeometry(
            items = items,
            size = chartSize,
            cylinderLeft = groupLeftPx,
            cylinderWidth = cylinderWidthPx,
            top = cylinderTop,
            bottomInset = chartSize.height - cylinderTop - cylinderBodyHeight,
        )
    }
    val darkSurface = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val itemAlphas = items.mapIndexed { index, item ->
        key(item.id) {
            animateFloatAsState(
                targetValue = if (selectedIndex < 0 || selectedIndex == index) {
                    1f
                } else if (darkSurface) {
                    0.18f
                } else {
                    0.14f
                },
                animationSpec = tween(durationMillis = 140),
                label = "statisticsFocusAlpha",
            ).value
        }
    }

    LaunchedEffect(selectedIndex >= 0) {
        if (selectedIndex < 0) {
            lineProgress.animateTo(0f, tween(durationMillis = 100))
        } else {
            lineProgress.animateTo(1f, tween(durationMillis = 140))
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { chartSize = it }
            .onGloballyPositioned { chartOrigin = it.positionInRoot() },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items, geometry) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val capRadius = geometry.width * 0.09f
                        val startedInCylinder =
                            down.position.x in geometry.left..geometry.right &&
                                down.position.y in
                                (geometry.top - capRadius)..(geometry.bottom + capRadius)
                        if (!startedInCylinder) return@awaitEachGesture

                        selectedIndex = geometry.segmentIndexAt(
                            down.position.y.coerceIn(geometry.top, geometry.bottom),
                        ) ?: -1
                        var accumulatedDrag = Offset.Zero
                        var gestureAxis = GestureAxis.UNDECIDED
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: break
                                if (!change.pressed) break
                                accumulatedDrag += change.position - change.previousPosition
                                if (gestureAxis == GestureAxis.UNDECIDED &&
                                    maxOf(
                                        abs(accumulatedDrag.x),
                                        abs(accumulatedDrag.y),
                                    ) > viewConfiguration.touchSlop
                                ) {
                                    gestureAxis = if (
                                        abs(accumulatedDrag.y) > abs(accumulatedDrag.x)
                                    ) {
                                        GestureAxis.VERTICAL
                                    } else {
                                        GestureAxis.HORIZONTAL
                                    }
                                }
                                when (gestureAxis) {
                                    GestureAxis.VERTICAL -> {
                                        geometry.segmentIndexAt(
                                            change.position.y.coerceIn(
                                                geometry.top,
                                                geometry.bottom,
                                            ),
                                        )
                                            ?.let { selectedIndex = it }
                                        change.consume()
                                    }

                                    GestureAxis.HORIZONTAL -> selectedIndex = -1
                                    GestureAxis.UNDECIDED -> Unit
                                }
                            }
                        } finally {
                            selectedIndex = -1
                        }
                    }
                },
        ) {
            if (geometry.segments.isEmpty()) return@Canvas
            val ellipseHeight = geometry.width * ELLIPSE_HEIGHT_RATIO
            val leftLighten = if (darkSurface) 0.03f else 0.04f
            val rightDarken = if (darkSurface) 0.06f else 0.05f
            val capDarken = if (darkSurface) 0.08f else 0.06f

            geometry.segments.asReversed().forEach { segment ->
                val item = items[segment.index]
                val alpha = itemAlphas[segment.index]
                val bodyTop = segment.top
                val curveDepth = ellipseHeight / 2f
                val centerX = (geometry.left + geometry.right) / 2f
                val radiusX = geometry.width / 2f
                val bodyPath = Path().apply {
                    moveTo(geometry.left, bodyTop)
                    lineTo(geometry.left, segment.bottom)
                    cubicTo(
                        geometry.left,
                        segment.bottom + ELLIPSE_CONTROL_POINT * curveDepth,
                        centerX - ELLIPSE_CONTROL_POINT * radiusX,
                        segment.bottom + curveDepth,
                        centerX,
                        segment.bottom + curveDepth,
                    )
                    cubicTo(
                        centerX + ELLIPSE_CONTROL_POINT * radiusX,
                        segment.bottom + curveDepth,
                        geometry.right,
                        segment.bottom + ELLIPSE_CONTROL_POINT * curveDepth,
                        geometry.right,
                        segment.bottom,
                    )
                    lineTo(geometry.right, bodyTop)
                    cubicTo(
                        geometry.right,
                        bodyTop + ELLIPSE_CONTROL_POINT * curveDepth,
                        centerX + ELLIPSE_CONTROL_POINT * radiusX,
                        bodyTop + curveDepth,
                        centerX,
                        bodyTop + curveDepth,
                    )
                    cubicTo(
                        centerX - ELLIPSE_CONTROL_POINT * radiusX,
                        bodyTop + curveDepth,
                        geometry.left,
                        bodyTop + ELLIPSE_CONTROL_POINT * curveDepth,
                        geometry.left,
                        bodyTop,
                    )
                    close()
                }
                drawPath(
                    path = bodyPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            lerp(item.color, Color.White, leftLighten).copy(alpha = alpha),
                            lerp(item.color, Color.White, leftLighten * 0.55f)
                                .copy(alpha = alpha),
                            item.color.copy(alpha = alpha),
                            lerp(item.color, Color.Black, rightDarken * 0.45f)
                                .copy(alpha = alpha),
                            lerp(item.color, Color.Black, rightDarken)
                                .copy(alpha = alpha),
                        ),
                        startX = geometry.left,
                        endX = geometry.right,
                    ),
                )
                if (segment.index == 0) {
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                lerp(item.color, Color.Black, capDarken * 0.55f)
                                    .copy(alpha = alpha),
                                lerp(item.color, Color.Black, capDarken * 0.72f)
                                    .copy(alpha = alpha),
                                lerp(item.color, Color.Black, capDarken)
                                    .copy(alpha = alpha),
                            ),
                            startX = geometry.left,
                            endX = geometry.right,
                        ),
                        topLeft = Offset(geometry.left, segment.top - ellipseHeight / 2f),
                        size = Size(geometry.width, ellipseHeight),
                    )
                }
            }

            val selectedSegment = geometry.segments.getOrNull(selectedIndex)
            val legendCenter = legendCenters[selectedIndex]
            if (selectedSegment != null && legendCenter != null && lineProgress.value > 0f) {
                val start = Offset(geometry.right, selectedSegment.centerY)
                val end = legendCenter
                val elbow = Offset(
                    x = (end.x - 26.dp.toPx()).coerceAtLeast(start.x + 10.dp.toPx()),
                    y = end.y,
                )
                val diagonalProgress = (lineProgress.value / 0.72f).coerceIn(0f, 1f)
                val horizontalProgress =
                    ((lineProgress.value - 0.72f) / 0.28f).coerceIn(0f, 1f)
                val diagonalEnd = lerp(start, elbow, diagonalProgress)
                drawLine(
                    color = items[selectedIndex].color,
                    start = start,
                    end = diagonalEnd,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                if (diagonalProgress >= 1f) {
                    drawLine(
                        color = items[selectedIndex].color,
                        start = elbow,
                        end = lerp(elbow, end, horizontalProgress),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (groupLeftPx + cylinderWidthPx + legendGapPx).roundToInt(),
                        y = legendTopPx.roundToInt(),
                    )
                }
                .width(legendWidth),
        ) {
            items.forEachIndexed { index, item ->
                val itemDescription = "${item.label}, ${item.valueText}"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(itemAlphas[index])
                        .semantics(mergeDescendants = true) {
                            contentDescription = itemDescription
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInRoot() - chartOrigin
                                    legendCenters[index] = position + Offset(
                                        coordinates.size.width / 2f,
                                        coordinates.size.height / 2f,
                                    )
                                }
                                .background(item.color, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.valueText,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private enum class GestureAxis {
    UNDECIDED,
    VERTICAL,
    HORIZONTAL,
}

private const val ELLIPSE_CONTROL_POINT = 0.5522848f
private const val ELLIPSE_HEIGHT_RATIO = 0.19f
private const val CHART_GROUP_WIDTH_RATIO = 0.78f
private const val CHART_GROUP_HEIGHT_RATIO = 0.56f
private const val CYLINDER_WIDTH_SHARE = 0.39726028f
private const val LEGEND_GAP_SHARE = 0.12328767f
private const val LEGEND_WIDTH_SHARE = 0.47945205f
private const val MIN_SEGMENT_HEIGHT_TO_WIDTH_RATIO = 0.05f
private const val STATISTICS_OPTION_COUNT = 4
private val CYLINDER_HEIGHT = 539.dp

private data class CylinderGeometry(
    val left: Float,
    val right: Float,
    val width: Float,
    val segments: List<CylinderSegment>,
) {
    val top: Float
        get() = segments.firstOrNull()?.top ?: 0f

    val bottom: Float
        get() = segments.lastOrNull()?.bottom ?: 0f

    fun segmentIndexAt(y: Float): Int? = segments
        .firstOrNull { y in it.top..it.bottom }
        ?.index
}

private fun calculateCylinderGeometry(
    items: List<CylinderStatisticItem>,
    size: IntSize,
    cylinderLeft: Float,
    cylinderWidth: Float,
    top: Float,
    bottomInset: Float,
): CylinderGeometry {
    if (items.isEmpty() || size.width <= 0 || size.height <= 0) {
        return CylinderGeometry(0f, 0f, 0f, emptyList())
    }
    val width = min(cylinderWidth, size.width.toFloat()).coerceAtLeast(1f)
    val left = cylinderLeft.coerceIn(
        minimumValue = 0f,
        maximumValue = (size.width - width).coerceAtLeast(0f),
    )
    val bottom = (size.height - bottomInset).coerceAtLeast(top + items.size)
    val availableHeight = (bottom - top).coerceAtLeast(items.size.toFloat())
    val minimumSegmentHeight = min(
        width * MIN_SEGMENT_HEIGHT_TO_WIDTH_RATIO,
        availableHeight / items.size,
    )
    val proportionalHeight = (
        availableHeight - minimumSegmentHeight * items.size
    ).coerceAtLeast(0f)
    val summedWeight = items.sumOf { it.weight.coerceAtLeast(0L) }
    val hasWeight = summedWeight > 0L
    val totalWeight = summedWeight
        .takeIf { hasWeight }
        ?: items.size.toLong()
    var currentTop = top
    val segments = items.mapIndexed { index, item ->
        val weight = if (hasWeight) {
            item.weight.coerceAtLeast(0L)
        } else {
            1L
        }
        val height = if (index == items.lastIndex) {
            bottom - currentTop
        } else {
            minimumSegmentHeight +
                proportionalHeight * (weight.toFloat() / totalWeight.toFloat())
        }
        CylinderSegment(
            index = index,
            top = currentTop,
            bottom = (currentTop + height).coerceAtMost(bottom),
        ).also { currentTop = it.bottom }
    }
    return CylinderGeometry(
        left = left,
        right = left + width,
        width = width,
        segments = segments,
    )
}

private fun qualityColor(quality: AudioQuality?, darkSurface: Boolean): Color = when (quality) {
    AudioQuality.RAW -> STATISTICS_COLUMN_RED
    AudioQuality.HI_RES -> STATISTICS_COLUMN_YELLOW
    AudioQuality.SQ -> STATISTICS_COLUMN_PURPLE
    AudioQuality.HQ -> STATISTICS_COLUMN_BLUE
    null -> statisticsColumnOtherColor(darkSurface)
}

private fun formatColor(index: Int, darkSurface: Boolean): Color = when (
    index.mod(STATISTICS_FORMAT_COLUMN_COLOR_COUNT)
) {
    7 -> statisticsColumnOtherColor(darkSurface)
    8 -> statisticsColumnLightGrayColor(darkSurface)
    else -> STATISTICS_FORMAT_COLUMN_COLORS[index.mod(STATISTICS_FORMAT_COLUMN_COLORS.size)]
}

private fun statisticsColumnOtherColor(darkSurface: Boolean): Color = if (darkSurface) {
    STATISTICS_COLUMN_DARK_OTHER
} else {
    STATISTICS_COLUMN_OTHER
}

private fun statisticsColumnLightGrayColor(darkSurface: Boolean): Color = if (darkSurface) {
    STATISTICS_COLUMN_DARK_LIGHT_GRAY
} else {
    STATISTICS_COLUMN_LIGHT_GRAY
}

private val STATISTICS_COLUMN_LIGHT_GRAY = Color(0xFFC4CBE1)
private val STATISTICS_COLUMN_OTHER = Color(0xFF9DA2BC)
private val STATISTICS_COLUMN_GREEN = Color(0xFF38E191)
private val STATISTICS_COLUMN_BLUE = Color(0xFF3193FB)
private val STATISTICS_COLUMN_PURPLE = Color(0xFFD13FEE)
private val STATISTICS_COLUMN_RED = Color(0xFFFB4D46)
private val STATISTICS_COLUMN_ORANGE = Color(0xFFFF963A)
private val STATISTICS_COLUMN_YELLOW = Color(0xFFFCD231)
private val STATISTICS_COLUMN_CYAN = Color(0xFF14CBCB)
private val STATISTICS_COLUMN_DARK_LIGHT_GRAY = Color(0xFF9FA4BE)
private val STATISTICS_COLUMN_DARK_OTHER = Color(0xFF666F8E)
private val STATISTICS_FORMAT_COLUMN_COLORS = listOf(
    STATISTICS_COLUMN_YELLOW,
    STATISTICS_COLUMN_ORANGE,
    STATISTICS_COLUMN_RED,
    STATISTICS_COLUMN_PURPLE,
    STATISTICS_COLUMN_BLUE,
    STATISTICS_COLUMN_GREEN,
    STATISTICS_COLUMN_CYAN,
)
private const val STATISTICS_FORMAT_COLUMN_COLOR_COUNT = 9

private fun formatStatisticsSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    val mebibytes = safeBytes / (1_024.0 * 1_024.0)
    return if (mebibytes < 1_024.0) {
        String.format(Locale.ROOT, "%.2fMB", mebibytes)
    } else {
        String.format(Locale.ROOT, "%.2fGB", mebibytes / 1_024.0)
    }
}
