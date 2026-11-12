// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component.os4

import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.State
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.LayerOutsets
import kotlin.math.abs
import top.yukonga.miuix.kmp.glass.*
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.systemBars
import top.yukonga.miuix.kmp.glass.GlassShadows
import top.yukonga.miuix.kmp.glass.GlassDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.melox.player.ui.navigation.LocalWindowBackDispatcherOwner
import com.melox.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.melox.player.ui.component.LocalWideTopBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import top.yukonga.miuix.kmp.glass.GlassShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.melox.player.ui.component.os4.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// Map one second of spring motion onto the 400ms search transition.
private val SearchModeEasing: Easing = run {
    val omega = 2.0 * PI / 0.75f
    val stiffness = (omega * omega).toFloat()
    val damping = (0.98f * 2.0 * omega).toFloat()
    val decay = -damping / 2f
    val frequency = sqrt(-(damping.toDouble() * damping - stiffness.toDouble() * 4)).toFloat() / 2f
    val sineCoefficient = decay / frequency
    Easing { fraction ->
        (
            1.0 + exp((decay * fraction).toDouble()) *
                (-cos((frequency * fraction).toDouble()) + sineCoefficient * sin((frequency * fraction).toDouble()))
            ).toFloat()
    }
}

/** Search header and motion from the pinned Miuix Glass fullscreen-search example. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlassSearchHeader(
    session: GlassSearchSession,
    page: GlassSearchPage,
    modifier: Modifier = Modifier,
    expanded: Boolean = session.expanded.value,
) {
    val wide = com.melox.player.ui.LocalOs4LandscapeNavigation.current != null
    val onDismissRequest = session.onDismiss.value
    var fieldSlotBounds by remember { mutableStateOf<Rect?>(null) }
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded && (!wide || (fieldSlotBounds != null && page.buttonBounds != null))
    val transition = rememberTransition(transitionState, label = "glassSearch")
    val progress = transition.animateFloat(
        transitionSpec = {
            if (wide) GlassMotion.transformBounds(targetState)
            else tween(400, easing = SearchModeEasing)
        },
        label = "glassSearchGeometry",
    ) { if (it) 1f else 0f }
    val iconProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.transformIconMaterial(targetState) },
        label = "glassSearchAnchorIcon",
    ) { if (it) 1f else 0f }
    val contentProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.transformContentMaterial(targetState) },
        label = "glassSearchContent",
    ) { if (it) 1f else 0f }
    val cancelAlpha = transition.animateFloat(
        transitionSpec = { tween(150, delayMillis = if (targetState) 100 else 0, easing = LinearEasing) },
        label = "glassSearchCancelMaterial",
    ) { if (it) 1f else 0f }
    val searchBackdrop = LocalWideTopBackdrop.current.takeIf { LocalTopBarBlurSettings.current.blurEnabled }
    val searchVisuals = GlassPopupDefaults.visuals()
    val fraction = progress.value.coerceIn(0f, 1f)
    val searchShape = GlassShape(22.dp)
    val query = session.query.value
    val showResults = showGlassSearchResults(expanded, query)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val wideSearchWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() } * 0.21f
    val statusBarHeight = WindowInsets.statusBarsIgnoringVisibility.getTop(density)
    val active = expanded || transition.currentState || transition.isRunning
    val overlayPresentation = remember(page, transition) {
        derivedStateOf {
            (transition.targetState || transition.currentState || transition.isRunning) &&
                fieldSlotBounds != null && page.buttonBounds != null
        }
    }
    SideEffect { if (wide) page.overlayPresentation = overlayPresentation }
    LaunchedEffect(expanded, session) {
        if (expanded) {
            session.onFocusedChange.value(true)
            focusRequester.requestFocus()
            keyboard?.show()
        } else if (active) {
            session.onFocusedChange.value(false)
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    androidx.compose.runtime.DisposableEffect(page) {
        onDispose {
            page.transitionActive = false
            page.overlayPresentation = null
        }
    }
    SideEffect { page.transitionActive = active }
    val backOwner = LocalWindowBackDispatcherOwner.current ?: LocalNavigationEventDispatcherOwner.current
    if (backOwner != null) CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides backOwner) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = expanded,
            onBackCompleted = onDismissRequest,
        )
    }
    if (!active) return
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .then(if (wide) Modifier.fillMaxWidth().graphicsLayer { alpha = if (overlayPresentation.value) 1f else 0f } else Modifier.fillMaxSize())
            .onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            },
    ) {
        if (!wide && !showResults) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null) { keyboard?.hide() },
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(if (wide) Modifier.padding(end = 52.dp).then(Modifier.widthIn(max = wideSearchWidth.coerceAtMost(320.dp))) else Modifier)
                .fillMaxWidth()
                .graphicsLayer { alpha = if (wide) 1f else progress.value }
                .windowInsetsPadding(if (wide) WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.systemBars) else WindowInsets.statusBarsIgnoringVisibility)
                .then(if (wide) Modifier else Modifier.imePadding()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding, vertical = 4.dp)
                    .onPlaced { fieldSlotBounds = Rect(it.positionInRoot(), Size(it.size.width.toFloat(), it.size.height.toFloat())) }
                    .graphicsLayer { translationY = if (wide) 0f else -statusBarHeight * (1f - progress.value) },
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = session.onQueryChange.value,
                    enabled = expanded,
                    singleLine = true,
                    textStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val fraction = progress.value.coerceIn(0f, 1f)
                            val endSpace = if (wide) 0 else (54.dp.toPx() * fraction).toInt()
                            val targetWidth = (constraints.maxWidth - endSpace).coerceAtLeast(0)
                            val button = page.buttonBounds
                            val startWidth = button?.width ?: GlassTopAppBarDefaults.ButtonSize.toPx()
                            val width = if (wide) {
                                (startWidth + (targetWidth - startWidth) * fraction).roundToInt().coerceIn(0, targetWidth)
                            } else targetWidth
                            val height = 44.dp.roundToPx()
                            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
                            layout(constraints.maxWidth, placeable.height) {
                                val slot = fieldSlotBounds
                                if (wide && button != null && slot != null) {
                                    val x = if (direction == LayoutDirection.Ltr) button.right - slot.left - width else button.left - slot.left
                                    val y = button.center.y - slot.top - placeable.height / 2f
                                    placeable.place(x.roundToInt(), y.roundToInt())
                                } else {
                                    placeable.placeRelative(if (wide) constraints.maxWidth - width else 0, 0)
                                }
                            }
                        }
                        .height(44.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (wide) session.onFocusedChange.value(it.isFocused) }
                        .onGloballyPositioned { page.fieldBounds = it.boundsInRoot() },
                    decorationBox = { field ->
                        Box(Modifier.fillMaxSize()) {
                            if (wide) {
                                val anchor = page.buttonAnchor.surface
                                Box(
                                    Modifier.matchParentSize().graphicsLayer {
                                        alpha = transformPanelAlpha(
                                            visualAlpha = 1f,
                                            floating = page.buttonAnchor.surfaceFloating || page.buttonAnchor.surfaceAlpha > 0f,
                                            anchorAlpha = page.buttonAnchor.surfaceAlpha,
                                            geometryProgress = fraction,
                                            iconMaterial = iconProgress.value.coerceIn(0f, 1f),
                                        )
                                    }.glassShadow(searchShape, searchVisuals.shadow),
                                ) {
                                    if (anchor != null && fraction < 1f) GlassPopupBackground(
                                        backdrop = anchor.backdrop,
                                        shape = searchShape,
                                        visuals = searchVisuals.copy(
                                            style = anchor.style, material = anchor.material, stroke = anchor.stroke,
                                            containerColor = anchor.fill, alpha = 1f,
                                        ),
                                        underlayMaterial = anchor.underlayMaterial,
                                    )
                                    GlassPopupBackground(
                                        backdrop = searchBackdrop ?: anchor?.backdrop,
                                        shape = searchShape,
                                        visuals = searchVisuals,
                                        modifier = Modifier.graphicsLayer { alpha = if (anchor != null) fraction else 1f },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (wide) {
                                            val content = contentProgress.value.coerceIn(0f, 1f)
                                            alpha = content
                                            val blur = GlassMotion.TRANSFORM_BLUR_PX * (1f - content)
                                            renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                                        }
                                    }
                                    .clip(if (wide) searchShape else CircleShape)
                                    .then(if (wide) Modifier else Modifier.background(colors.surfaceContainerHigh, CircleShape))
                                    .padding(horizontal = if (wide) (10f + 2f * progress.value.coerceIn(0f, 1f)).dp else 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(glassSearchIcon, contentDescription = null, modifier = Modifier.size(if (wide) 20.dp else 28.dp), tint = colors.disabledOnSurface)
                                Box(Modifier.weight(1f)) {
                                    if (query.isEmpty()) Text(session.label.value, color = colors.disabledOnSurface, style = MiuixTheme.textStyles.body1)
                                    field()
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        session.onQueryChange.value("")
                                        focusRequester.requestFocus()
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(glassCloseIcon, contentDescription = stringResource(R.string.search_clear), modifier = Modifier.size(16.dp * (7f / 6f)), tint = colors.disabledOnSurface)
                                    }
                                }
                            }
                            if (wide) Icon(
                                glassSearchIcon,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
                                    .size(24.dp).graphicsLayer {
                                        val icon = iconProgress.value.coerceIn(0f, 1f)
                                        alpha = 1f - icon
                                        val blur = GlassMotion.TRANSFORM_BLUR_PX * icon
                                        renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                                    },
                                tint = colors.onSurface,
                            )
                        }
                    },
                )
                if (!wide) Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            val fraction = progress.value
                            translationX = 66.dp.toPx() * (1f - fraction) * if (direction == LayoutDirection.Ltr) 1f else -1f
                            scaleX = fraction
                            scaleY = fraction
                            alpha = cancelAlpha.value
                            val blur = 50f * (1f - cancelAlpha.value)
                            renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                        },
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        enabled = expanded,
                        modifier = Modifier.size(44.dp).background(colors.surfaceContainerHigh, CircleShape),
                    ) {
                        Icon(glassCloseIcon, contentDescription = stringResource(R.string.search_cancel), modifier = Modifier.size(24.dp), tint = colors.onSurface)
                    }
                }
            }
        }
    }
}


private const val SEARCH_TOP_BAR_BLUR_RADIUS = 50f

/** Share the existing bar fade with its independently rendered progressive-blur background. */
@Composable
internal fun rememberGlassSearchBarAlpha(expanded: Boolean): State<Float> {
    val transition = updateTransition(expanded, label = "glassSearchTopBarOpacity")
    return transition.animateFloat(
        transitionSpec = {
            tween(150, easing = if (targetState) CubicBezierEasing(0f, 0f, 0.67f, 1f) else CubicBezierEasing(0.26f, 0f, 0.6f, 0.2f))
        },
        label = "glassSearchTopBarAlpha",
    ) { if (it) 0f else 1f }
}

/** ActionBarView fades, shrinks to 0.9, and blurs while entering search mode. */
@Composable
internal fun Modifier.glassSearchBarTransition(expanded: Boolean, barAlpha: () -> Float): Modifier {
    val density = LocalDensity.current
    val layerOutsets = remember(density) {
        val shadow = GlassShadows.Regular
        val shadowOutset = ((shadow.radius + maxOf(abs(shadow.offsetX), abs(shadow.offsetY))) / GlassDefaults.SourceDensity).dp
        // Keep a fixed halo for the shadows and maximum blur throughout the offscreen fade.
        val outset = shadowOutset + with(density) { (SEARCH_TOP_BAR_BLUR_RADIUS * 3f).toDp() }
        LayerOutsets(left = outset, top = outset, right = outset, bottom = outset)
    }
    val transition = updateTransition(expanded, label = "glassSearchTopBar")
    val scale = transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchModeEasing) },
        label = "glassSearchTopBarScale",
    ) { if (it) 0.9f else 1f }
    val blur = transition.animateFloat(
        transitionSpec = { tween(if (targetState) 150 else 120, easing = LinearEasing) },
        label = "glassSearchTopBarBlur",
    ) { if (it) SEARCH_TOP_BAR_BLUR_RADIUS else 0f }
    // Idle bars draw directly so their button shadows are not bounded by a fade layer.
    if (scale.value == 1f && blur.value == 0f && barAlpha() == 1f) return this
    return graphicsLayer {
        outsets = layerOutsets
        scaleX = scale.value
        scaleY = scale.value
        alpha = barAlpha()
        renderEffect = if (blur.value > 0.5f) BlurEffect(blur.value, blur.value, TileMode.Decal) else null
    }
}

/** ActionBarView reveals the title container after exit progress crosses 0.8. */
@Composable
internal fun rememberGlassSearchTitleAlpha(expanded: Boolean, collapsed: Boolean): () -> Float {
    val transition = updateTransition(expanded, label = "glassSearchTitlePhase")
    val progress = transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchModeEasing) },
        label = "glassSearchTitleExitProgress",
    ) { if (it) 0f else 1f }
    val reveal by remember(expanded, progress) { derivedStateOf { expanded || progress.value > 0.8f } }
    val response = if (collapsed) 0.15f else 0.6f
    val titleAlpha = animateFloatAsState(
        targetValue = if (reveal) 1f else 0f,
        animationSpec = if (reveal && !expanded) {
            spring(dampingRatio = 1f, stiffness = (2 * PI / response).let { (it * it).toFloat() }, visibilityThreshold = 0.001f)
        } else {
            snap()
        },
        label = "glassSearchTitleAlpha",
    )
    return remember(titleAlpha) { { titleAlpha.value } }
}
