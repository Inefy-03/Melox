// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarLayout
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.internal.currentProgressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** Default values for [GlassTopAppBar] and [GlassIconButton]. */
object GlassTopAppBarDefaults {
    /** Official progressive band height, independent of the expanded title height. */
    fun progressiveBandHeight(bottomContentHeight: Int, topInset: Int, density: Density): Int = with(density) {
        ((topInset + TopAppBarDefaults.CollapsedHeight.toPx() + bottomContentHeight + 20.dp.toPx()) * 1.2f).roundToInt()
    }

    /** Start/end inset of the glass controls, aligned with the page's cards and tabs. */
    val HorizontalPadding: Dp = 12.dp

/** Scroll threshold for the retained shadow ramp. */
    val RampStart: Dp = 0.dp

/** Scroll distance for the retained shadow ramp. Material transitions use their own timing. */
    val RampDistance: Dp = 32.dp

    /** Maximum blur radius applied to titles during collapse/expand transitions. */
    val TitleBlurRadius: Dp = 8.dp

    /** Diameter of the glass pill behind a bar button. */
    val ButtonSize: Dp = 44.dp

    /** Maximum radius of the progressive background band. */
    val ProgressiveBlurRadius: Dp = 80.dp

    /** Scroll distance over which the progressive band fades in. */
    val ProgressiveBlurRampDistance: Dp = 48.dp

    /** Icon opacity while an action-bar button is held. */
    val PressedContentAlpha: Float = 0.6f

    /**
     * Visual scroll progress in `[0, 1]`, including elastic displacement.
     * [visualContentOffset] overrides the behavior's visual offset; no source means full strength.
     */
    @Composable
    fun collapseRamp(
        scrollBehavior: ScrollBehavior?,
        visualContentOffset: (() -> Float)? = null,
    ): State<Float> {
        val density = LocalDensity.current
        val startPx = with(density) { RampStart.toPx() }
        val rampPx = with(density) { RampDistance.toPx() }
        val currentOffset by rememberUpdatedState(visualContentOffset)
        return remember(scrollBehavior, startPx, rampPx) {
            derivedStateOf {
                val offset = currentOffset?.invoke() ?: scrollBehavior?.state?.contentOffset
                if (offset == null) 1f else ((-offset - startPx) / rampPx).coerceIn(0f, 1f)
            }
        }
    }

/** Fallback button fill when backdrop material is unavailable. */
    @Composable
    fun buttonFill(): Color = when {
        MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.surfaceContainerHigh
        isDarkTheme() -> Color(0xFF2C2C2C)
        else -> Color.White
    }

/** Default material for action-bar buttons. */
    @Composable
    fun buttonMaterial(): GlassMaterial = dynamicGlassMaterial(GlassMaterials.puredThinGlass(isDarkTheme()))

/** Material applied beneath child buttons and tabs. */
    @Composable
    internal fun actionBarUnderlayMaterial(): GlassMaterial = GlassMaterials.actionBarMask(isDarkTheme())

    /** The small bloom stroke paired with [buttonMaterial]. */
    @Composable
    fun buttonStroke(): GlassStroke = GlassStrokes.forTheme(
        isDark = isDarkTheme(),
        light = GlassStrokes.SmallLight,
        dark = GlassStrokes.SmallDark,
    )

    /** The state overlay painted behind a held action-bar icon. */
    @Composable
    fun buttonPressedOverlay(): Color = if (isDarkTheme()) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

/** Gradient that dims content passing beneath the bar.
     *
     * @param color Page background colour.
     */
    @Stable
    fun bandBrush(color: Color): Brush = TopAppBarBandBrush(color)

    /** How far the band reaches past the bottom of the bar. */
    val BandOverhang: Dp = 36.dp

    internal const val ProgressiveBandCurve = 0.25f

    internal fun fadeBandBrush(brush: Brush, overhang: Dp, density: Density): Brush? =
        with(density) {
            val fadeHeight = overhang.roundToPx().coerceAtLeast(0) + 36.dp.toPx()
            (brush as? TopAppBarBandBrush)?.let { TopAppBarFadeBrush(it, fadeHeight) }
        }

    @Composable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

/** Starts fading inside the collapsed bar so the title area stays legible while avoiding an opaque shelf. */
private data class TopAppBarBandBrush(val color: Color) : ShaderBrush() {
    private val colors = opacities.map { color.copy(alpha = color.alpha * it) }

    override fun createShader(size: Size): Shader = createBandShader(size, 0f, size.height)

    fun createBandShader(size: Size, fadeStart: Float, fadeHeight: Float): Shader {
        val height = size.height.coerceAtLeast(1f)
        val start = fadeStart.coerceIn(0f, height)
        val actualFadeHeight = fadeHeight.coerceAtLeast(1f)
        val fadeStops = stops.drop(1).map { stop ->
            (start + actualFadeHeight * stop).coerceAtMost(height) / height
        }
        val topColor = colors.first()
        val colorStops = if (start > 0f) listOf(0f, start / height) + fadeStops else listOf(0f) + fadeStops
        val gradientColors = if (start > 0f) listOf(topColor, topColor) + colors.drop(1) else colors
        return LinearGradientShader(
            from = Offset.Zero,
            to = Offset(0f, height),
            colors = gradientColors,
            colorStops = colorStops,
        )
    }

    private companion object {
        const val MAX_ALPHA = 0.90f
        val stops = List(17) { it / 16f }
        val opacities = stops.map { t ->
            val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
            MAX_ALPHA * (1f - fade)
        }
    }
}

private data class TopAppBarFadeBrush(
    val band: TopAppBarBandBrush,
    val fadeHeight: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val fadeStart = (size.height - fadeHeight).coerceAtLeast(0f)
        return band.createBandShader(size, fadeStart, fadeHeight)
    }
}

internal data class GlassTopAppBarContext(
    val backdrop: Backdrop?,
    val buttonBackdrop: Backdrop?,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val buttonUnderlayMaterial: GlassMaterial?,
    val floating: Boolean,
    val alpha: Float,
    val ramp: State<Float>,
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
    val style: GlassStyle,
    val size: Dp,
    val shape: GlassShape,
    val fill: Color,
    val stroke: GlassStroke?,
    val shadow: GlassShadow?,
    val onButtonBackdropConsumerChange: ((Int) -> Unit)? = null,
)

internal val LocalGlassTopAppBarContext = staticCompositionLocalOf<GlassTopAppBarContext?> { null }

/**
 * Top app bar whose material visibility follows [ScrollBehavior].
 * Use the overload with [isContentScrolled] when the page can report its exact overlap state.
 *
 * @param title Compact title, centered when the bar collapses.
 * @param backdrop Background sampled by the buttons. `null` uses [fill].
 * @param modifier Modifier applied to the bar.
 * @param largeTitle Large title shown before content scrolls. Defaults to [title].
 * @param expandable Whether the large title can expand. False keeps the compact title visible.
 * @param subtitle Optional subtitle below the title.
 * @param titleBlurRadius Maximum blur radius applied to titles during collapse/expand transitions.
 * @param scrollBehavior Behavior driving collapse and material visibility from actual content scroll.
 * @param visualContentOffset Content position in pixels, negative upward, including elastic displacement.
 *   Overrides band/shadow progress; pair with [isContentScrolled] for programmatic scrolling.
 * @param style Glass style forwarded to the renderer.
 * @param alpha Opacity multiplier for button pills.
 * @param buttonShape Button-pill silhouette.
 * @param buttonSize Button-pill diameter.
 * @param buttonShadow Shadow beneath button pills; `null` disables it.
 * @param fill Fallback button fill when glass rendering support is unavailable.
 * @param stroke Optional bloom stroke on button pills.
 * @param defaultWindowInsetsPadding Whether to apply horizontal display-cutout and navigation-bar insets.
 * @param contentModifier Modifier for the foreground bar; the background band remains stationary.
 * @param progressiveBlur Draw a progressive background and let buttons sample that rendered scene.
 *   Requires a supported, non-null [backdrop]. The background excludes buttons and other bar content.
 * @param material Button material, applied over the rendered progressive band when enabled.
 * @param backgroundAlpha Draw-phase opacity multiplier for the background band.
 * @param titleAlpha Draw-phase opacity for title containers.
 * @param compact Use the pinned small-title layout without an expandable title region.
 * @param navigationIcon Leading control slot. Use [GlassIconButton] for a glass button.
 * @param actions Trailing control slot. Use [GlassIconButton] for glass buttons.
 * @param bottomContent Content pinned below the title inside the bar.
 * @param expandedContent Optional custom title region that collapses above [bottomContent] in compact layout.
 * @param showTitleWhileExpanded Keep the compact title visible during contextual actions.
 */
@Composable
fun GlassTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    visualContentOffset: (() -> Float)? = null,
    progressiveBlur: Boolean = false,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    expandable: Boolean = true,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    titleBlurRadius: Dp = GlassTopAppBarDefaults.TitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    material: GlassMaterial = GlassTopAppBarDefaults.buttonMaterial(),
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titleAlpha: () -> Float = { 1f },
    backgroundAlpha: () -> Float = { 1f },
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    compact: Boolean = false,
    expandedContent: (@Composable () -> Unit)? = null,
    showTitleWhileExpanded: Boolean = false,
) {
    val currentOffset by rememberUpdatedState(visualContentOffset)
    val isContentScrolled by remember(scrollBehavior) {
        derivedStateOf {
            val offset = scrollBehavior?.state?.contentOffset ?: currentOffset?.invoke()
            offset?.let { it < 0f } ?: true
        }
    }
    GlassTopAppBar(
        title = title,
        isContentScrolled = isContentScrolled,
        compact = compact,
        expandedContent = expandedContent,
        showTitleWhileExpanded = showTitleWhileExpanded,
        modifier = modifier,
        visualContentOffset = visualContentOffset,
        progressiveBlur = progressiveBlur,
        backdrop = backdrop,
        largeTitle = largeTitle,
        expandable = expandable,
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        bandBrush = bandBrush,
        bandOverhang = bandOverhang,
        titleBlurRadius = titleBlurRadius,
        style = style,
        material = material,
        alpha = alpha,
        buttonSize = buttonSize,
        buttonShape = buttonShape,
        fill = fill,
        stroke = stroke,
        buttonShadow = buttonShadow,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        contentModifier = contentModifier,
        titleAlpha = titleAlpha,
        backgroundAlpha = backgroundAlpha,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
    )
}

/**
 * A glass top bar with an explicit page-overlap state.
 *
 * @param title Compact title.
 * @param isContentScrolled Whether page content has left its resting position. For a lazy list,
 *   pass `listState.canScrollBackward`.
 */
@Composable
fun GlassTopAppBar(
    title: String,
    isContentScrolled: Boolean,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    visualContentOffset: (() -> Float)? = null,
    progressiveBlur: Boolean = false,
    backdrop: Backdrop? = null,
    largeTitle: String = title,
    expandable: Boolean = true,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    bandBrush: Brush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
    bandOverhang: Dp = GlassTopAppBarDefaults.BandOverhang,
    titleBlurRadius: Dp = GlassTopAppBarDefaults.TitleBlurRadius,
    style: GlassStyle = GlassDefaults.Style,
    material: GlassMaterial = GlassTopAppBarDefaults.buttonMaterial(),
    alpha: Float = 1f,
    buttonSize: Dp = GlassTopAppBarDefaults.ButtonSize,
    buttonShape: GlassShape = GlassShape(buttonSize / 2),
    fill: Color = GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = GlassTopAppBarDefaults.buttonStroke(),
    buttonShadow: GlassShadow? = GlassShadows.Regular,
    defaultWindowInsetsPadding: Boolean = true,
    titleAlpha: () -> Float = { 1f },
    backgroundAlpha: () -> Float = { 1f },
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    compact: Boolean = false,
    expandedContent: (@Composable () -> Unit)? = null,
    showTitleWhileExpanded: Boolean = false,
) {
    val ramp = GlassTopAppBarDefaults.collapseRamp(scrollBehavior, visualContentOffset)
    val floating = isContentScrolled
    val transition = updateTransition(floating, label = "glassTopBarFloat")
    val maskProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarMask() },
        label = "glassTopBarMask",
    ) { if (it) 1f else 0f }
    val scrolledMaterialProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarButtonFloat() },
        label = "glassTopBarMaterial",
    ) { if (it) 1f else 0f }
    val currentVisualOffset by rememberUpdatedState(visualContentOffset)
    val materialProgress = remember(scrolledMaterialProgress, scrollBehavior, ramp) {
        derivedStateOf {
            // The pinned UI lacks overscrollOffset; the page supplies the native displacement.
            val visualOffset = currentVisualOffset?.invoke()
            val elasticProgress = if (scrollBehavior != null && visualOffset != null &&
                visualOffset != scrollBehavior.state.contentOffset) ramp.value else 0f
            maxOf(scrolledMaterialProgress.value, elasticProgress)
        }
    }
    val keepMaterial by remember(floating, materialProgress) {
        derivedStateOf { floating || materialProgress.value > 0f }
    }
    val underlayMaterial = GlassTopAppBarDefaults.actionBarUnderlayMaterial().takeIf { keepMaterial && backdrop != null }
    val rawBackdrop = backdrop?.takeIf { isRuntimeShaderSupported() }
    var backgroundCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var buttonBackdropConsumers by remember { mutableIntStateOf(0) }
    val hasButtonBackdropConsumers = buttonBackdropConsumers > 0
    val onButtonBackdropConsumerChange: (Int) -> Unit = remember {
        { delta -> buttonBackdropConsumers = (buttonBackdropConsumers + delta).coerceAtLeast(0) }
    }
    val progressiveBackdrop = if (progressiveBlur && rawBackdrop != null) {
        rememberLayerBackdrop {
            val coordinates = backgroundCoordinates?.takeIf { it.isAttached }
            with(rawBackdrop) { drawBackdrop(this@rememberLayerBackdrop, coordinates) }
            drawContent()
        }
    } else {
        null
    }
    val buttonUnderlay = underlayMaterial.takeIf { progressiveBackdrop == null }
    val resolvedMaterial = material.withUnderlayBlur(buttonUnderlay)

    val measurements = remember { GlassTopAppBarMeasurements() }
    val measuredBottomContent: @Composable () -> Unit = {
        Box(Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            measurements.bottomContentHeight = placeable.height
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }) { bottomContent() }
    }
    GlassTopAppBarLayout(
        modifier = modifier,
        progressiveBlur = progressiveBackdrop != null && rawBackdrop != null,
        measurements = measurements,
        gradientOverhang = if (bandBrush is TopAppBarBandBrush) bandOverhang else 0.dp,
        hasExpandedContent = compact && expandedContent != null,
    ) {
        if (progressiveBackdrop != null && rawBackdrop != null) {
            val surfaceColor = MiuixTheme.colorScheme.surface
            val blendColors = remember(surfaceColor) {
                listOf(BlendColorEntry(surfaceColor.copy(alpha = 0.3f)))
            }
            val blurColors = BlurDefaults.blurColors(blendColors = blendColors)
            val currentBgAlpha by rememberUpdatedState(backgroundAlpha)
            val rampDistancePx = with(LocalDensity.current) { GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx() }
            val bandActive by remember(scrollBehavior, maskProgress, rampDistancePx) {
                derivedStateOf {
                    val offset = currentVisualOffset?.invoke() ?: scrollBehavior?.state?.contentOffset
                    val rampVal = offset?.let { (-it / rampDistancePx).coerceIn(0f, 1f) } ?: maskProgress.value
                    rampVal * currentBgAlpha().coerceIn(0f, 1f) > 0.001f
                }
            }
            val backdropCaptureModifier = if (hasButtonBackdropConsumers) {
                Modifier
                    .onGloballyPositioned { backgroundCoordinates = it }
                    .layerBackdrop(progressiveBackdrop)
            } else {
                Modifier
            }
            Box(
                modifier = backdropCaptureModifier
                    .graphicsLayer {
                        val offset = visualContentOffset?.invoke() ?: scrollBehavior?.state?.contentOffset
                        val ramp = offset?.let {
                            (-it / GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx()).coerceIn(0f, 1f)
                        } ?: maskProgress.value
                        this.alpha = ramp * backgroundAlpha().coerceIn(0f, 1f)
                    }
                    .currentProgressiveTextureBlur(
                        backdrop = rawBackdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = GlassTopAppBarDefaults.ProgressiveBandCurve),
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = blurColors,
                        enabled = bandActive,
                    ),
            )
        } else {
            val overhangPx = with(LocalDensity.current) { bandOverhang.roundToPx().coerceAtLeast(0).toFloat() }
            val density = LocalDensity.current
            val fadeBrush = remember(bandBrush, bandOverhang, density) {
                GlassTopAppBarDefaults.fadeBandBrush(bandBrush, bandOverhang, density)
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val progress = if (visualContentOffset != null || scrollBehavior != null) ramp.value else maskProgress.value
                        this.alpha = progress * backgroundAlpha().coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleY = if (bandBrush !is TopAppBarBandBrush && size.height > 0f) (size.height + overhangPx) / size.height else 1f
                    }
                    .then(
                        if (fadeBrush != null) {
                            Modifier.drawBehind {
                                drawRect(fadeBrush, size = size)
                            }
                        } else {
                            Modifier.background(bandBrush)
                        },
                    ),
            )
        }
        CompositionLocalProvider(
            LocalGlassTopAppBarContext provides GlassTopAppBarContext(
                backdrop = rawBackdrop,
                buttonBackdrop = progressiveBackdrop ?: rawBackdrop,
                material = resolvedMaterial,
                underlayMaterial = underlayMaterial,
                buttonUnderlayMaterial = buttonUnderlay,
                floating = floating,
                alpha = alpha,
                ramp = ramp,
                materialProgress = materialProgress,
                keepMaterial = keepMaterial,
                style = style,
                size = buttonSize,
                shape = buttonShape,
                fill = fill,
                stroke = stroke,
                shadow = buttonShadow,
                onButtonBackdropConsumerChange = if (progressiveBackdrop != null) onButtonBackdropConsumerChange else null,
            ),
        ) {
            if (compact) {
                GlassCompactTopAppBar(
                    title = title, modifier = contentModifier,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha()),
                    subtitle = subtitle, scrollBehavior = scrollBehavior,
                    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                    navigationIcon = navigationIcon, actions = actions, bottomContent = measuredBottomContent,
                    expandedContent = expandedContent,
                    showTitleWhileExpanded = showTitleWhileExpanded,
                    titleBlurRadius = titleBlurRadius,
                )
            } else BlurTopAppBar(
                title = title,
                modifier = contentModifier,
                titleAlpha = titleAlpha,
                largeTitle = largeTitle,
                expandable = expandable,
                titleBlurRadius = titleBlurRadius,
                subtitle = subtitle,
                color = Color.Transparent,
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                navigationIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
                actionIconPadding = GlassTopAppBarDefaults.HorizontalPadding,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = measuredBottomContent,
            )
        }
    }
}

private class GlassTopAppBarMeasurements {
    var bottomContentHeight: Int = 0
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlassTopAppBarLayout(
    progressiveBlur: Boolean,
    measurements: GlassTopAppBarMeasurements,
    gradientOverhang: Dp,
    hasExpandedContent: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val topInsets = WindowInsets.statusBarsIgnoringVisibility
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        // Measure the foreground first: its bottom slot and the band share this measure pass.
        val foreground = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = constraints.constrainWidth(foreground.width)
        val height = constraints.constrainHeight(foreground.height)
        val bandHeight = if (progressiveBlur && hasExpandedContent) {
            ((height + 20.dp.toPx()) * 1.2f).roundToInt()
        } else if (progressiveBlur) {
            GlassTopAppBarDefaults.progressiveBandHeight(
                bottomContentHeight = measurements.bottomContentHeight,
                topInset = topInsets.getTop(this),
                density = this,
            )
        } else {
            height + gradientOverhang.roundToPx().coerceAtLeast(0)
        }
        val background = measurables[0].measure(Constraints.fixed(width, bandHeight))
        layout(width, height) {
            background.placeRelative(0, 0)
            foreground.placeRelative(0, 0)
        }
    }
}

@Composable
internal fun BlurTopAppBar(
    title: String,
    titleBlurRadius: Dp,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitle: String = title,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    titleAlpha: () -> Float = { 1f },
    bottomContent: @Composable () -> Unit = {},
) {
    val actionsRow =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }

    TopAppBarLayout(
        title = title,
        titleAlpha = titleAlpha,
        expandable = expandable,
        color = color,
        titleColor = titleColor,
        largeTitle = largeTitle,
        largeTitleColor = largeTitleColor,
        titleBlurRadius = titleBlurRadius,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        navigationIcon = navigationIcon,
        actions = actionsRow,
        titlePadding = titlePadding,
        navigationIconPadding = navigationIconPadding,
        actionIconPadding = actionIconPadding,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        bottomContent = bottomContent,
    )
}
