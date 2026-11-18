// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.upstream.glass


import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.upstream.blur.Backdrop
import top.yukonga.miuix.kmp.upstream.material.*

@Stable
class GlassPopupAnchor {

    internal var secondaryBackProgressState: State<Float>? by mutableStateOf(null)

    /** Predictive-back progress published by an attached [GlassDropdownPopup]. */
    internal var dropdownBackProgressState: State<Float>? by mutableStateOf(null)

    /** Keeps the restored value visible while its fade outlives the dropdown's popup host. */
    internal var dropdownRetainedValueAlpha: Float by mutableFloatStateOf(0f)

    /**
     * Secondary predictive-back progress, including cancellation recovery and the retained exit
     * fraction. Read inside a draw/layer callback to make custom arrows follow the menu:
     * `arrowRotation = { rotation * (1f - anchor.secondaryBackProgress) }`.
     */
    val secondaryBackProgress: Float get() = secondaryBackProgressState?.value ?: 0f

    /** Material supplied by an attached glass button, including its resolved backdrop. */
    internal var surface: GlassAnchorSurface? by mutableStateOf(null)

    /** Expanded menu appearance shared with a secondary popup. */
    internal var popupVisuals: GlassPopupVisuals? by mutableStateOf(null)

    /** Button surface progress, separate from shadow opacity. */
    internal var surfaceProgress: State<Float>? by mutableStateOf(null)
    internal var surfaceMaterialAlpha: Float by mutableFloatStateOf(1f)
    internal val surfaceVisibility: Float get() = surfaceProgress?.value ?: 1f
    internal var surfaceFloating: Boolean by mutableStateOf(false)

    /** The control's outer bounds in window coordinates. */
    internal var containerBounds: Rect by mutableStateOf(Rect.Zero)

    /** Control content bounds in window coordinates. */
    internal var contentBounds: Rect by mutableStateOf(Rect.Zero)

    /** The dropdown's value and chevron bounds in window coordinates. */
    internal var dropdownValueBounds: Rect? by mutableStateOf(null)

    /** Control corner radius used at the collapsed endpoint. */
    internal var cornerRadius: Dp by mutableStateOf(0.dp)

    /** Whether to keep the panel opaque during the transition instead of fading with the icon. */
    internal var floating: Boolean by mutableStateOf(false)

    /** Hides the whole transform anchor, or only the dropdown value, while the popup is open. */
    internal var contentHidden: Boolean by mutableStateOf(false)

    /** Dropdown value opacity; a [State] so the fade only invalidates draw. */
    internal var valueAlphaState: State<Float>? by mutableStateOf(null)

    /** Value opacity during predictive-back collapse. */
    internal val dropdownValueAlpha: Float
        get() = (dropdownBackProgressState?.value ?: dropdownRetainedValueAlpha).coerceIn(0f, 1f)
}

/** The button's sampling and colour treatment at the collapsed endpoint of its menu. */
internal data class GlassAnchorSurface(
    val backdrop: Backdrop?,
    val style: GlassStyle,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val stroke: EdgeStroke?,
    val fill: Color,
)

/** Marks the button's modifier so its renderer can publish the actual surface to this anchor. */
internal data class GlassPopupAnchorElement(val anchor: GlassPopupAnchor) : Modifier.Element

