package com.melox.player.ui.screen.settings

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melox.player.ui.component.adjustPadding
import com.melox.player.MeloxApplication
import com.melox.player.R
import com.melox.player.data.playback.ListenedSong
import com.melox.player.data.playback.ListeningAmount
import com.melox.player.data.playback.ListeningPeriod
import com.melox.player.data.playback.ListeningRange
import com.melox.player.data.playback.ListeningStatistics
import com.melox.player.data.playback.ListeningStatisticsStore
import com.melox.player.model.MusicTrack
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.IconButton
import com.melox.player.ui.component.LazyColumn
import com.melox.player.ui.component.OverlayIconDropdownMenu
import com.melox.player.ui.component.PageCard
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.TopAppBar
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.navigationBackIcon
import com.melox.player.ui.component.overScrollVertical
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.glass.GlassTopAppBarDefaults
import com.melox.player.ui.icon.ChevronForwardDemibold
import com.melox.player.ui.icon.GlassIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToLong

private enum class ListeningMetric(val label: Int) {
    COUNT(R.string.listening_statistics_count),
    DURATION(R.string.listening_statistics_duration);

    fun value(amount: ListeningAmount) = if (this == COUNT) amount.count else amount.durationMs

    fun format(context: Context, value: Long, locale: Locale, splitHours: Boolean = false): String = when (this) {
        COUNT -> context.getString(
            if (value == 1L) R.string.listening_statistics_play else R.string.listening_statistics_plays,
            NumberFormat.getIntegerInstance(locale).format(value),
        )
        DURATION -> if (value in 1L until 60_000L) {
            context.getString(R.string.listening_statistics_less_than_minute)
        } else {
            val totalMinutes = value / 60_000L
            val hours = if (splitHours) totalMinutes / 60L else 0L
            val minutes = if (hours > 0L) totalMinutes % 60L else totalMinutes
            val resources = context.resources
            val numbers = NumberFormat.getIntegerInstance(locale)
            val minutesText = resources.getQuantityString(
                R.plurals.listening_statistics_minutes,
                minutes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                numbers.format(minutes),
            )
            if (hours == 0L) {
                minutesText
            } else {
                val hoursText = resources.getQuantityString(
                    R.plurals.listening_statistics_hours,
                    hours.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    numbers.format(hours),
                )
                if (minutes == 0L) hoursText
                else context.getString(R.string.listening_statistics_hours_minutes, hoursText, minutesText)
            }
        }
    }

    fun formatDailyAverage(context: Context, value: Double, locale: Locale): String {
        if (this == DURATION) return format(context, value.toLong(), locale, splitHours = true)
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
        return context.getString(
            if (value == 1.0) R.string.listening_statistics_play else R.string.listening_statistics_plays,
            number,
        )
    }
}

@Composable
internal fun ListeningStatisticsScreen(
    tracks: List<MusicTrack>,
    currentUri: String?,
    hiddenUris: Set<String>,
    showDuration: Boolean,
    onShowDurationChange: (Boolean) -> Unit,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPlay: (List<MusicTrack>, Int) -> Unit,
) {
    val context = LocalContext.current
    val store = (context.applicationContext as MeloxApplication).listeningStatistics
    val locale = LocalConfiguration.current.locales[0]
    val (zone, today) = produceState(listeningCalendar()) {
        while (true) {
            value = listeningCalendar()
            delay(15_000L)
        }
    }.value
    val historyStart = key(store, zone, hiddenUris) {
        val updates = remember { store.observeOldestHistoryDate(zone, hiddenUris) }
        updates.collectAsStateWithLifecycle(initialValue = null).value
    }
    val metric = if (showDuration) ListeningMetric.DURATION else ListeningMetric.COUNT
    val ranges = ListeningRange.entries
    val pagerState = rememberPagerState { ranges.size }
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val listStates = ranges.map { rememberLazyListState() }
    val recordedSongStates = ranges.map { range ->
        val songPeriod = range.period(today, offset = 0)
        key(store, range, zone, hiddenUris) {
            val updates = remember(songPeriod) {
                store.observe(range, songPeriod, null, zone, hiddenUris).map { it.songs }
            }
            updates.collectAsStateWithLifecycle(initialValue = emptyList())
        }
    }
    val activeListState = listStates[pagerState.currentPage]
    val contentUnderTopBar by remember(activeListState, scrollBehavior) {
        derivedStateOf { activeListState.canScrollBackward || scrollBehavior.state.overscrollOffset < -1f }
    }
    var captureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { captureCoordinates })
    val fallbackBackdrop = rememberLayerBackdrop()
    val tracksByUri = remember(tracks) { tracks.associateBy(MusicTrack::contentUri) }

    PageScaffold(
        topBar = {
            TopBarBackdrop(backdrop = backdrop) {
                TopAppBar(
                    title = stringResource(R.string.listening_statistics_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(navigationBackIcon(), stringResource(R.string.back))
                        }
                    },
                    actions = {
                        OverlayIconDropdownMenu(
                            entries = listOf(DropdownEntry(ListeningMetric.entries.map { option ->
                                DropdownItem(
                                    text = stringResource(option.label),
                                    selected = option == metric,
                                    onClick = { onShowDurationChange(option == ListeningMetric.DURATION) },
                                )
                            })),
                            collapseOnSelection = true,
                        ) {
                            Icon(MiuixIcons.Tune, stringResource(R.string.listening_statistics_tune), Modifier.size(24.dp))
                        }
                    },
                    bottomContent = {
                        GlassSegmentedTabRow(
                            tabs = listOf(
                                stringResource(R.string.listening_statistics_day),
                                stringResource(R.string.listening_statistics_week),
                                stringResource(R.string.listening_statistics_month),
                            ),
                            selectedIndex = pagerState.targetPage,
                            onSelect = { index -> scope.launch { pagerState.springAnimateToPage(index) } },
                            backdrop = backdrop ?: fallbackBackdrop,
                            shadow = if (backdrop != null) GlassShadows.Float else null,
                            surfaceAlpha = if (backdrop != null && contentUnderTopBar) 1f else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                                .padding(top = 6.dp, bottom = 12.dp),
                        )
                    },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().then(
                backdrop?.let { Modifier.onGloballyPositioned { captureCoordinates = it }.layerBackdrop(it) }
                    ?: Modifier,
            ),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { ranges[it].name },
            ) { page ->
                val range = ranges[page]
                val activeScrollBehavior = scrollBehavior.takeIf { page == pagerState.currentPage }
                val recordedSongs = recordedSongStates[page].value
                val songs = remember(recordedSongs, metric) {
                    recordedSongs
                        .filter { metric.value(it.amount) > 0 }
                        .sortedWith(compareByDescending<ListenedSong> {
                            metric.value(it.amount)
                        }.thenBy { it.title }.thenBy { it.uri })
                }
                val playable = remember(songs, tracksByUri) { songs.mapNotNull { tracksByUri[it.uri] } }
                val playableIndices = remember(playable) {
                    playable.withIndex().associate { (index, track) -> track.contentUri to index }
                }
                LazyColumn(
                    state = listStates[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical(scrollBehavior = activeScrollBehavior)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = padding.adjustPadding(
                        bottom = { maxOf(it, bottomContentPadding) + 16.dp },
                    ),
                    overscrollEffect = null,
                ) {
                    item(key = "chart") {
                        ListeningStatisticsCard(
                            store = store,
                            range = range,
                            today = today,
                            zone = zone,
                            hiddenUris = hiddenUris,
                            historyStart = historyStart,
                            metric = metric,
                            locale = locale,
                        )
                    }
                    itemsIndexed(songs, key = { _, song -> song.uri }) { rowIndex, song ->
                        val index = playableIndices[song.uri]
                        val track = tracksByUri[song.uri] ?: remember(song.uri, song.title, song.artist, song.album) {
                            MusicTrack(
                                id = 0L,
                                title = song.title,
                                artist = song.artist,
                                album = song.album,
                                durationMs = 0L,
                                dateAddedEpochSeconds = 0L,
                                dateModifiedEpochSeconds = 0L,
                                fileName = null,
                                fileSizeBytes = 0L,
                                contentUri = song.uri,
                                titleSectionKey = "",
                                titleSortKey = "",
                            )
                        }
                        // Only the ends of the shared surface are rounded. Rows remain lazy.
                        val topCorner = if (rowIndex == 0) CardDefaults.CornerRadius else 0.dp
                        val bottomCorner = if (rowIndex == songs.lastIndex) CardDefaults.CornerRadius else 0.dp
                        val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        PageCard(
                            modifier = if (topCorner == 0.dp && bottomCorner == 0.dp) cardModifier else {
                                cardModifier.squircleClip(
                                    topStart = topCorner, topEnd = topCorner,
                                    bottomEnd = bottomCorner, bottomStart = bottomCorner,
                                )
                            },
                            cornerRadius = 0.dp,
                            insideMargin = PaddingValues(0.dp),
                        ) {
                            MusicTrackRow(
                                track = track,
                                isCurrent = currentUri == track.contentUri,
                                enabled = index != null,
                                onClick = { if (index != null) onPlay(playable, index) },
                                onMoreClick = null,
                                trailingText = metric.format(context, metric.value(song.amount), locale),
                                contentPadding = PaddingValues(
                                    start = 12.dp, top = 12.dp,
                                    end = TopAppBarDefaults.ActionIconPadding, bottom = 12.dp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun listeningCalendar(): Pair<ZoneId, LocalDate> {
    val zone = ZoneId.systemDefault()
    return zone to LocalDate.now(zone)
}

private data class LoadedListeningPeriod(
    val period: ListeningPeriod,
    val offset: Int,
    val statistics: ListeningStatistics,
)

@Composable
private fun ListeningStatisticsCard(
    store: ListeningStatisticsStore,
    range: ListeningRange,
    today: LocalDate,
    zone: ZoneId,
    hiddenUris: Set<String>,
    historyStart: LocalDate?,
    metric: ListeningMetric,
    locale: Locale,
) {
    var periodOffset by rememberSaveable { mutableIntStateOf(0) }
    val period = range.period(today, periodOffset)
    val previousPeriod = range.previousComparisonPeriod(period, periodOffset)
        .takeIf { historyStart != null && it.endExclusive > historyStart }
    val headline = key(store, range, zone, hiddenUris) {
        val updates = remember(period, periodOffset, previousPeriod) {
            store.observe(range, period, previousPeriod, zone, hiddenUris).map {
                LoadedListeningPeriod(period, periodOffset, it)
            }
        }
        updates.collectAsStateWithLifecycle(initialValue = null).value
    }
    val displayedPeriod = headline?.period ?: period
    val displayedOffset = headline?.offset ?: periodOffset
    val context = LocalContext.current
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale) }
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMM"), locale) }
    val periodLabel = when (range) {
        ListeningRange.DAY -> when (periodOffset) {
            0 -> stringResource(R.string.listening_statistics_today)
            -1 -> stringResource(R.string.listening_statistics_yesterday)
            else -> dateFormat.format(period.start)
        }
        ListeningRange.WEEK -> when (periodOffset) {
            1 -> stringResource(R.string.listening_statistics_this_week)
            0 -> stringResource(R.string.listening_statistics_recent_days, range.rollingDays)
            -1 -> stringResource(R.string.listening_statistics_last_week)
            else -> stringResource(
                R.string.listening_statistics_period,
                dateFormat.format(period.start),
                dateFormat.format(period.endExclusive.minusDays(1)),
            )
        }
        ListeningRange.MONTH -> if (periodOffset == 0) {
            stringResource(R.string.listening_statistics_recent_days, range.rollingDays)
        } else monthFormat.format(period.start)
    }
    val metricLabel = stringResource(
        if (metric == ListeningMetric.COUNT) R.string.listening_statistics_period_count
        else R.string.listening_statistics_period_duration,
    )
    val values = remember(headline, metric, range, displayedPeriod) {
        headline?.statistics?.buckets?.map(metric::value) ?: List(range.size(displayedPeriod)) { 0L }
    }
    val headlineValue = remember(headline, range, metric) {
        headline?.let { range.headlineValue(values) }
    }
    val summary = if (headlineValue == null) {
        ""
    } else if (range == ListeningRange.DAY) {
        metric.format(context, headlineValue.toLong(), locale, splitHours = true)
    } else {
        stringResource(
            R.string.listening_statistics_daily_average,
            metric.formatDailyAverage(context, headlineValue, locale),
        )
    }
    val comparisonText = if (headline != null && headlineValue != null) {
        headline.statistics.previousBuckets?.let { previous ->
            val comparisonRange = when {
                range == ListeningRange.DAY && headline.offset == 0 -> stringResource(R.string.listening_statistics_yesterday)
                range == ListeningRange.DAY -> {
                    val shortDate = remember(locale) {
                        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "Md"), locale)
                    }
                    shortDate.format(headline.period.start.minusDays(1))
                }
                headline.offset == 0 -> stringResource(R.string.listening_statistics_previous_cycle_label)
                range == ListeningRange.WEEK -> stringResource(R.string.listening_statistics_last_week)
                else -> stringResource(R.string.listening_statistics_previous_month_label)
            }
            val delta = headlineValue - range.headlineValue(previous.map(metric::value))
            if (delta == 0.0 || (metric == ListeningMetric.COUNT && (abs(delta) * 10).roundToLong() == 0L)) {
                stringResource(R.string.listening_statistics_comparison_equal, comparisonRange)
            } else {
                val amount = if (metric == ListeningMetric.DURATION) {
                    metric.format(context, abs(delta).roundToLong().coerceAtLeast(1L), locale, splitHours = true)
                } else metric.formatDailyAverage(context, abs(delta), locale)
                val message = when {
                    range == ListeningRange.DAY && delta > 0 -> R.string.listening_statistics_comparison_more
                    range == ListeningRange.DAY -> R.string.listening_statistics_comparison_less
                    delta > 0 -> R.string.listening_statistics_comparison_average_more
                    else -> R.string.listening_statistics_comparison_average_less
                }
                stringResource(message, comparisonRange, amount)
            }
        } ?: stringResource(R.string.listening_statistics_no_comparison)
    } else null
    val axisNumbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val headerSpace = with(LocalDensity.current) { headerHeight.toDp() }
    var showPrompt by remember(range, metric, displayedPeriod, displayedOffset) { mutableStateOf(false) }
    val headerAlpha by animateFloatAsState(
        targetValue = if (showPrompt) 0f else 1f,
        animationSpec = if (showPrompt) folmeSpring(damping = 0.95f, response = 0.18f)
        else tween(500, easing = Easing { ((1.0 - cos(Math.PI * it)) / 2.0).toFloat() }),
    )
    val cardTopPadding = 12.dp
    val cardHorizontalPadding = 16.dp
    val darkSurface = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val supportingTextColor = if (darkSurface) MiuixTheme.colorScheme.onSurfaceVariantSummary
    else MiuixTheme.colorScheme.onSurfaceVariantActions
    PageCard(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        insideMargin = PaddingValues(
            start = cardHorizontalPadding, end = cardHorizontalPadding, top = cardTopPadding, bottom = 20.dp,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            key(range, metric, displayedPeriod, displayedOffset) {
                ListeningStatisticsChart(
                    range = range,
                    start = displayedPeriod.start,
                    today = today,
                    values = values,
                    locale = locale,
                    chartWidth = maxWidth,
                    headerSpace = headerSpace,
                    cardTopPadding = cardTopPadding,
                    cardHorizontalPadding = cardHorizontalPadding,
                    onPromptVisible = { showPrompt = it },
                    formatAxis = {
                        if (metric == ListeningMetric.COUNT) axisNumbers.format(it)
                        else metric.format(context, it, locale, splitHours = true)
                    },
                    formatValue = { metric.format(context, it, locale, splitHours = true) },
                    promptLabel = { context.getString(R.string.listening_statistics_period_metric, it, metricLabel) },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeight = it.height }
                    .alpha(headerAlpha.coerceIn(0f, 1f))
                    .then(if (showPrompt) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp - cardTopPadding)
                        .height(34.dp)
                        .squircleBorder(
                            width = 0.76.dp,
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (darkSurface) 0.2f else 0.08f),
                            cornerRadius = 17.dp,
                        )
                        .padding(horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListeningPeriodButton(
                        label = stringResource(R.string.listening_statistics_previous),
                        enabled = range.canGoPrevious(today, periodOffset, historyStart) && !showPrompt,
                        onClick = { periodOffset-- },
                    )
                    Text(
                        text = stringResource(R.string.listening_statistics_period_metric, periodLabel, metricLabel),
                        modifier = Modifier.weight(1f, fill = false),
                        style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ListeningPeriodButton(
                        label = stringResource(R.string.listening_statistics_next),
                        enabled = !showPrompt && periodOffset < if (range == ListeningRange.DAY) 0 else 1,
                        onClick = { periodOffset++ },
                        forward = true,
                    )
                }
                Text(
                    text = summary,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MiuixTheme.textStyles.title2.copy(fontWeight = FontWeight.ExtraBold),
                )
                Text(
                    text = comparisonText.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                    color = supportingTextColor,
                )
            }
        }
    }
}

@Composable
private fun ListeningPeriodButton(label: String, enabled: Boolean, onClick: () -> Unit, forward: Boolean = false) {
    val buttonSize = 28.dp
    val viewConfiguration = LocalViewConfiguration.current
    val periodButtonConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize = DpSize(buttonSize, buttonSize)
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides periodButtonConfiguration) {
        Box(
            modifier = Modifier.size(buttonSize).clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = GlassIcons.ChevronForwardDemibold,
                contentDescription = label,
                modifier = Modifier.size(13.dp).scale(scaleX = if (forward) 1f else -1f, scaleY = 1f),
                tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            )
        }
    }
}
