// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

// Effect entry points gate runtime shaders to API 33, the upstream module minimum.
@file:android.annotation.SuppressLint("NewApi")

package top.yukonga.miuix.kmp.upstream.blur.internal

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import top.yukonga.miuix.kmp.upstream.material.RuntimeShader
import top.yukonga.miuix.kmp.upstream.material.asAndroidRuntimeShader

internal fun RenderEffect?.chain(other: RenderEffect): RenderEffect = if (this != null) {
    android.graphics.RenderEffect.createChainEffect(
        other.asAndroidRenderEffect(),
        this.asAndroidRenderEffect(),
    ).asComposeRenderEffect()
} else {
    other
}

internal fun runtimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String,
): RenderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
    runtimeShader.asAndroidRuntimeShader(),
    uniformShaderName,
).asComposeRenderEffect()
