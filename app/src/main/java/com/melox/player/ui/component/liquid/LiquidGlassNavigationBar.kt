// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component.liquid

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.melox.player.ui.component.animation.DampedDragAnimation
import com.melox.player.ui.component.animation.InteractiveHighlight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform


private val LocalIosTabScale = staticCompositionLocalOf { { 1f } }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

// Mirrors HighlightStyle.kt's LIGHT_REF — keep in sync.
private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f // |g_xy| > 0.1, ≈ 6° tilt

// 3° quantization step for the gravity direction: finer changes are imperceptible.
private const val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

/**
 * In-screen-plane gravity direction angle (radians, quantized to 3° steps).
 *
 * Returned as [State] so the read can be deferred to the draw phase: the sensor writes tilt
 * state unthrottled (~50Hz), and a composition-time read would recompose the whole caller
 * scope on every tick. The derivedStateOf equality check then drops draw invalidations to
 * quantization-step crossings.
 */
@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val gx = tilt.gravityX
            val gy = tilt.gravityY
            val gMagSq = gx * gx + gy * gy
            if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                // Near-flat: the in-plane gravity direction is unstable, pin to (0, -1).
                (-PI / 2).toFloat()
            }
        }
    }
}

/**
 * [base] with its `dualPeak` primary light rotated to the gravity angle plus [extraDegrees].
 * Read `.value` only at draw time (see [rememberQuantizedGravityAngle]); the rotated copy is
 * cached, re-allocating only when the angle crosses a quantization step.
 */
@Composable
private fun rememberGravityRotatedHighlight(
    base: Highlight,
    extraDegrees: Float,
): State<Highlight> {
    val gravityAngle = rememberQuantizedGravityAngle()
    return remember(gravityAngle, base, extraDegrees) {
        derivedStateOf {
            val baseStyle = base.style as BloomStroke
            val basePrimary = baseStyle.primaryLight
            val rad = gravityAngle.value + (extraDegrees * PI / 180.0).toFloat()
            base.copy(
                style = baseStyle.copy(
                    primaryLight = basePrimary.copy(
                        position = LightPosition(
                            x = LIGHT_REF_X + cos(rad),
                            y = LIGHT_REF_Y + sin(rad),
                            z = basePrimary.position.z,
                        ),
                    ),
                ),
            )
        }
    }
}

@Composable
internal fun rememberIosLikeNavigationBarWidth(
    items: List<NavigationItem>,
    maxWidth: Dp,
): Dp {
    if (items.isEmpty()) return 0.dp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember { TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal) }
    val widestLabelWidth = remember(items, density, textMeasurer, labelStyle) {
        items.maxOf { item ->
            with(density) {
                textMeasurer.measure(
                    text = AnnotatedString(item.label),
                    style = labelStyle,
                    maxLines = 1,
                ).size.width.toDp()
            }
        }
    }
    val desiredTabWidth = (maxOf(widestLabelWidth, 22.dp) + 40.dp).coerceAtLeast(100.dp)
    return (desiredTabWidth * items.size + 8.dp).coerceAtMost(maxWidth)
}

@Composable
internal fun rememberFloatingBarHighlight(active: Boolean): State<Highlight>? = if (active) {
    rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
} else {
    null
}

@Composable
fun LiquidGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isBlurActive: Boolean,
    isLiquidGlassActive: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    containerHighlight: State<Highlight>? = null,
    bottomPadding: Dp? = null,
    badge: (Int) -> (@Composable () -> Unit)? = { null },
    vertical: Boolean = false,
) {
    if (items.isEmpty()) return
    val pillShape = remember { CircleShape }
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val liquidGlassActive = isLiquidGlassActive && backdrop != null
    val samplingBackdrop = remember(backdrop, vertical) {
        if (vertical && backdrop != null) object : Backdrop {
            override val isCoordinatesDependent = true
            override val offsetResidualX: Float get() = backdrop.offsetResidualX
            override val offsetResidualY: Float get() = backdrop.offsetResidualY
            override fun DrawScope.drawBackdrop(
                density: Density, coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?, downscaleFactor: Int,
            ) {
                // LayerBackdrop cancels this rotation before sampling the screen-space scene.
                // The parent then rotates the control, leaving the sampled background upright.
                with(backdrop) {
                    drawBackdrop(density, coordinates, {
                        layerBlock?.invoke(this)
                        rotationZ += 90f
                    }, downscaleFactor)
                }
            }
        } else backdrop
    }
    val blurBackdrop = samplingBackdrop.takeIf { isBlurActive }
    val glassBackdrop = samplingBackdrop.takeIf { liquidGlassActive }
    val containerColor = if (isBlurActive || liquidGlassActive) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val containerBlur = rememberGlassContainerBlur()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size
    val resolvedSelectedIndex = selectedIndex.coerceIn(0, tabsCount - 1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember(tabsCount) { mutableIntStateOf(resolvedSelectedIndex) }
    val onItemClickUpdated by rememberUpdatedState(onItemClick)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = resolvedSelectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position ->
                position.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onItemClickUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(resolvedSelectedIndex, dampedDrag) {
        if (currentIndex != resolvedSelectedIndex) {
            currentIndex = resolvedSelectedIndex
            dampedDrag.animateToValue(resolvedSelectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onItemClickUpdated(index)
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    // Keyed on dampedDrag: the position lambda captures it; a stale capture would freeze the press spot.
    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    // Read .value only inside highlight lambdas (draw phase), never in composition.
    val baseHighlight = if (liquidGlassActive) {
        containerHighlight ?: rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    } else {
        null
    }
    val pillHighlight = if (liquidGlassActive) {
        rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = 90f)
    } else {
        null
    }

    val combinedBackdrop = glassBackdrop?.let { rememberCombinedBackdrop(it, tabsBackdrop) }

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = bottomPadding ?: when (platform()) {
        Platform.IOS -> 20.dp

        else -> {
            floatingNavigationBarBottomPadding(navBarBottomPadding)
        }
    }

    val tabsContent: @Composable RowScope.() -> Unit = {
        val tabScale = LocalIosTabScale.current
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = index == currentIndex
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val isActivationKey = event.key == Key.Enter ||
                            event.key == Key.NumPadEnter ||
                            event.key == Key.Spacebar
                        if (isActivationKey) {
                            if (event.type == KeyEventType.KeyUp) activateTab(index)
                            true
                        } else {
                            false
                        }
                    }
                    .focusable()
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = tabScale()
                        scaleX = s
                        scaleY = s
                        rotationZ = if (vertical) -90f else 0f
                    },
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                BadgedBox(badge = { badge(index)?.invoke() }) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = item.icon,
                        // Decorative: the adjacent label names the item; avoids TalkBack double-read.
                        contentDescription = null,
                    )
                }
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    val orientationModifier = if (vertical) Modifier.layout { measurable, constraints ->
        val length = (items.size * 72.dp.toPx() + 8.dp.toPx()).roundToInt().coerceAtMost(constraints.maxHeight)
        val thickness = 64.dp.roundToPx()
        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(length, thickness))
        layout(thickness, length) {
            placeable.placeWithLayer(-(length - thickness) / 2, (length - thickness) / 2) { rotationZ = 90f }
        }
    } else Modifier
    Column(modifier = modifier.then(orientationModifier).fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .onSizeChanged { coords ->
                            totalWidthPx = coords.width.toFloat()
                            val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                            tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                        }
                        .graphicsLayer { translationX = panelOffset }
                        .dropShadow(
                            shape = pillShape,
                            shadow = Shadow(
                                radius = 10.dp,
                                color = Color.Black,
                                // Lighter in light theme to avoid a visible gray fringe.
                                alpha = if (isDark) 0.2f else 0.1f,
                            ),
                        )
                        .then(
                            if (liquidGlassActive && glassBackdrop != null && baseHighlight != null) {
                                Modifier.drawBackdrop(
                                    backdrop = glassBackdrop,
                                    shape = { pillShape },
                                    effects = {
                                        // Reserve the lens and press-scale sampling reach at full resolution.
                                        padding = maxOf(padding, 40.dp.toPx())
                                        vibrancy()
                                        effect(containerBlur)
                                        lens(
                                            refractionHeight = 24.dp.toPx(),
                                            refractionAmount = 24.dp.toPx(),
                                        )
                                    },
                                    highlight = { baseHighlight.value.copy(alpha = 0.75f) },
                                    layerBlock = {
                                        val width = size.width.coerceAtLeast(1f)
                                        val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDrag.pressProgress)
                                        scaleX = s
                                        scaleY = s
                                    },
                                    onDrawSurface = { drawRect(containerColor) },
                                )
                            } else if (blurBackdrop != null) {
                                Modifier.textureBlur(
                                    backdrop = blurBackdrop,
                                    shape = pillShape,
                                    blurRadius = 25f,
                                    colors = BlurDefaults.blurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(surfaceContainer.copy(alpha = 0.6f)),
                                        ),
                                    ),
                                    highlight = null,
                                )
                            } else {
                                Modifier.background(containerColor, pillShape)
                            },
                        )
                        .then(
                            if (liquidGlassActive) {
                                interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                            } else {
                                Modifier
                            },
                        )
                        .then(dampedDrag.modifier)
                        .height(64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tabsContent,
                )
            }

            if (liquidGlassActive && glassBackdrop != null) {
                CompositionLocalProvider(
                    LocalIosTabScale provides { lerp(1f, 1.2f, dampedDrag.pressProgress) },
                    LocalContentColor provides accentColor,
                ) {
                    Row(
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .graphicsLayer { translationX = panelOffset }
                            .drawBackdrop(
                                backdrop = glassBackdrop,
                                shape = { pillShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx(), 4.dp.toPx())
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 24.dp.toPx(),
                                    )
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                            .then(interactiveHighlight.modifier)
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = tabsContent,
                    )
                }
            }

            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                if (liquidGlassActive && combinedBackdrop != null && pillHighlight != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .graphicsLayer {
                                val singleTabWidth = tabWidthPx
                                val progressOffset = dampedDrag.value * singleTabWidth
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            }
                            .drawBackdrop(
                                backdrop = combinedBackdrop,
                                shape = { pillShape },
                                effects = {
                                    val progress = dampedDrag.pressProgress
                                    lens(
                                        refractionHeight = 10.dp.toPx() * progress,
                                        refractionAmount = 14.dp.toPx() * progress,
                                        depthEffect = true,
                                        chromaticAberration = 0.5f,
                                    )
                                },
                                highlight = { pillHighlight.value.copy(alpha = dampedDrag.pressProgress) },
                                layerBlock = {
                                    scaleX = dampedDrag.scaleX
                                    scaleY = dampedDrag.scaleY
                                    val v = dampedDrag.velocity / 10f
                                    scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = dampedDrag.pressProgress
                                    drawRect(
                                        color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                        alpha = 1f - progress,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                },
                            )
                            .innerShadow(shape = pillShape) {
                                InnerShadow(
                                    radius = 8.dp * dampedDrag.pressProgress,
                                    color = Color.Black.copy(alpha = 0.15f),
                                    alpha = dampedDrag.pressProgress,
                                )
                            }
                            .height(56.dp)
                            .width(tabWidthDp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .graphicsLayer {
                                val progressOffset = dampedDrag.value * tabWidthPx
                                translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            }
                            .clip(pillShape)
                            .background(accentColor.copy(alpha = 0.15f), pillShape)
                            .height(56.dp)
                            .width(tabWidthDp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides accentColor) {
                            Row(
                                modifier = Modifier
                                    .clearAndSetSemantics {}
                                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                    .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                    .height(56.dp)
                                    .graphicsLayer {
                                        val progressOffset = dampedDrag.value * tabWidthPx
                                        translationX = if (isLtr) -progressOffset else progressOffset
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                content = tabsContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun floatingNavigationBarBottomPadding(navigationBarBottomInset: Dp): Dp =
    if (navigationBarBottomInset > 0.dp) {
        navigationBarBottomInset + 8.dp
    } else {
        FLOATING_NAVIGATION_BAR_BOTTOM_PADDING_WITHOUT_SYSTEM_NAVIGATION
    }

private val FLOATING_NAVIGATION_BAR_BOTTOM_PADDING_WITHOUT_SYSTEM_NAVIGATION = 24.dp
