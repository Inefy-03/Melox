// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component.effect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.RuntimeShader
import top.yukonga.miuix.kmp.blur.asBrush
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Android adaptation of the OS3 background used by the official Miuix About
 * example. Unsupported devices keep the same content on the theme surface.
 */
@Composable
internal fun AboutEffectBackground(
    modifier: Modifier = Modifier,
    backgroundModifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    val shaderSupported = remember { isRuntimeShaderSupported() }
    if (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        !shaderSupported
    ) {
        Box(modifier = modifier, content = content)
        return
    }
    AboutRuntimeEffectBackground(
        modifier = modifier,
        backgroundModifier = backgroundModifier,
        alpha = alpha,
        content = content,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AboutRuntimeEffectBackground(
    modifier: Modifier,
    backgroundModifier: Modifier,
    alpha: () -> Float,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        val surface = MiuixTheme.colorScheme.surface
        val isDark = surface.luminance() < 0.5f
        val preset = remember(isDark) { AboutEffectConfig.forTheme(isDark) }
        val painter = remember { AboutEffectPainter() }
        val colorStage = remember { Animatable(0f) }

        LaunchedEffect(preset) {
            var targetStage = floor(colorStage.value) + 1f
            while (isActive) {
                delay((preset.colorInterpPeriod * 500).toLong())
                colorStage.animateTo(
                    targetValue = targetStage,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
                )
                targetStage += 1f
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .then(backgroundModifier)
                .aboutEffectDraw(
                    painter = painter,
                    preset = preset,
                    surface = surface,
                    colorStage = { colorStage.value },
                    alpha = alpha,
                ),
        )
        content()
    }
}

private object AboutEffectConfig {
    class Config(
        val points: FloatArray,
        val colors1: FloatArray,
        val colors2: FloatArray,
        val colors3: FloatArray,
        val colorInterpPeriod: Float,
        val lightOffset: Float,
        val saturateOffset: Float,
        val pointOffset: Float,
    )

    private val light = Config(
        points = floatArrayOf(
            0.8f, 0.2f, 1.0f,
            0.8f, 0.9f, 1.0f,
            0.2f, 0.9f, 1.0f,
            0.2f, 0.2f, 1.0f,
        ),
        colors1 = floatArrayOf(
            1.0f, 0.9f, 0.94f, 1.0f,
            1.0f, 0.84f, 0.89f, 1.0f,
            0.97f, 0.73f, 0.82f, 1.0f,
            0.64f, 0.65f, 0.98f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.58f, 0.74f, 1.0f, 1.0f,
            1.0f, 0.9f, 0.93f, 1.0f,
            0.74f, 0.76f, 1.0f, 1.0f,
            0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.98f, 0.86f, 0.9f, 1.0f,
            0.6f, 0.73f, 0.98f, 1.0f,
            0.92f, 0.93f, 1.0f, 1.0f,
            0.56f, 0.69f, 1.0f, 1.0f,
        ),
        colorInterpPeriod = 5f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )

    private val dark = Config(
        points = light.points,
        colors1 = floatArrayOf(
            0.2f, 0.06f, 0.88f, 0.4f,
            0.3f, 0.14f, 0.55f, 0.5f,
            0.0f, 0.64f, 0.96f, 0.5f,
            0.11f, 0.16f, 0.83f, 0.4f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f,
            0.62f, 0.21f, 0.67f, 0.5f,
            0.06f, 0.25f, 0.84f, 0.5f,
            0.0f, 0.2f, 0.78f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.4f,
            0.27f, 0.18f, 0.6f, 0.5f,
            0.66f, 0.26f, 0.62f, 0.5f,
            0.12f, 0.16f, 0.7f, 0.6f,
        ),
        colorInterpPeriod = 8f,
        lightOffset = 0f,
        saturateOffset = 0.17f,
        pointOffset = 0.4f,
    )

    fun forTheme(isDark: Boolean): Config = if (isDark) dark else light
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AboutEffectPainter {
    private val runtimeShader by lazy {
        RuntimeShader(OS3_BACKGROUND_SHADER).also {
            it.setFloatUniform("uTranslateY", 0f)
            it.setFloatUniform("uNoiseScale", 1.5f)
            it.setFloatUniform("uPointRadiusMulti", 1f)
            it.setFloatUniform("uAlphaMulti", 1f)
        }
    }
    val brush: Brush get() = runtimeShader.asBrush()

    private val resolution = FloatArray(2)
    private val bounds = FloatArray(4)
    private val colors = FloatArray(16)
    private val animatedPoints = FloatArray(8)
    private var cachedWidth = Float.NaN
    private var cachedHeight = Float.NaN
    private var cachedPreset: AboutEffectConfig.Config? = null
    private var cachedColorStage = Float.NaN
    private var cachedPointTime = Float.NaN
    private var cachedAnimationTime = Float.NaN

    fun update(
        width: Float,
        height: Float,
        time: Float,
        preset: AboutEffectConfig.Config,
        colorStage: Float,
    ) {
        if (cachedWidth != width || cachedHeight != height) {
            resolution[0] = width
            resolution[1] = height
            runtimeShader.setFloatUniform("uResolution", resolution)
            updateBounds(width, height)
            runtimeShader.setFloatUniform("uBound", bounds)
            cachedWidth = width
            cachedHeight = height
        }
        if (cachedPreset !== preset) {
            runtimeShader.setFloatUniform("uPoints", preset.points)
            runtimeShader.setFloatUniform("uLightOffset", preset.lightOffset)
            runtimeShader.setFloatUniform("uSaturateOffset", preset.saturateOffset)
            cachedPreset = preset
            cachedColorStage = Float.NaN
            cachedPointTime = Float.NaN
        }
        if (cachedAnimationTime != time) {
            runtimeShader.setFloatUniform("uAnimTime", time)
            cachedAnimationTime = time
        }
        updateColors(preset, colorStage)
        updatePoints(preset, time)
    }

    private fun updateBounds(width: Float, height: Float) {
        val effectHeight = height * 0.5f
        val heightRatio = effectHeight / height
        if (width <= height) {
            bounds[0] = 0f
            bounds[1] = 1f - heightRatio
            bounds[2] = 1f
            bounds[3] = heightRatio
        } else {
            val aspectRatio = width / height
            val centerY = 1f - heightRatio / 2f
            bounds[0] = 0f
            bounds[1] = centerY - aspectRatio / 2f
            bounds[2] = 1f
            bounds[3] = aspectRatio
        }
    }

    private fun updateColors(preset: AboutEffectConfig.Config, stage: Float) {
        if (cachedColorStage == stage) return
        val base = stage.toInt()
        val fraction = stage - base
        val start = colorsForIndex(preset, base)
        val end = colorsForIndex(preset, base + 1)
        for (index in colors.indices) {
            colors[index] = start[index] + (end[index] - start[index]) * fraction
        }
        runtimeShader.setFloatUniform("uColors", colors)
        cachedColorStage = stage
    }

    private fun colorsForIndex(
        preset: AboutEffectConfig.Config,
        index: Int,
    ): FloatArray = when (index.mod(4)) {
        1 -> preset.colors1
        3 -> preset.colors3
        else -> preset.colors2
    }

    private fun updatePoints(preset: AboutEffectConfig.Config, time: Float) {
        if (cachedPointTime == time) return
        for (index in 0 until 4) {
            val sourceX = preset.points[index * 3]
            val sourceY = preset.points[index * 3 + 1]
            val animatedX = sourceX + sin(time + sourceY) * preset.pointOffset
            animatedPoints[index * 2] = animatedX
            animatedPoints[index * 2 + 1] =
                sourceY + cos(time + animatedX) * preset.pointOffset
        }
        runtimeShader.setFloatUniform("uPointsAnim", animatedPoints)
        cachedPointTime = time
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.aboutEffectDraw(
    painter: AboutEffectPainter,
    preset: AboutEffectConfig.Config,
    surface: Color,
    colorStage: () -> Float,
    alpha: () -> Float,
): Modifier = this then AboutEffectElement(
    painter = painter,
    preset = preset,
    surface = surface,
    colorStage = colorStage,
    alpha = alpha,
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class AboutEffectElement(
    val painter: AboutEffectPainter,
    val preset: AboutEffectConfig.Config,
    val surface: Color,
    val colorStage: () -> Float,
    val alpha: () -> Float,
) : ModifierNodeElement<AboutEffectNode>() {
    override fun create(): AboutEffectNode = AboutEffectNode(
        painter = painter,
        preset = preset,
        surface = surface,
        colorStage = colorStage,
        alpha = alpha,
    )

    override fun update(node: AboutEffectNode) {
        node.update(painter, preset, surface, colorStage, alpha)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AboutEffectNode(
    private var painter: AboutEffectPainter,
    private var preset: AboutEffectConfig.Config,
    private var surface: Color,
    private var colorStage: () -> Float,
    private var alpha: () -> Float,
) : Modifier.Node(), DrawModifierNode {
    private var animationJob: Job? = null
    private var animationTime = 0f
    private var startOffset = 0f

    override fun onAttach() {
        startAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: AboutEffectPainter,
        preset: AboutEffectConfig.Config,
        surface: Color,
        colorStage: () -> Float,
        alpha: () -> Float,
    ) {
        this.painter = painter
        this.preset = preset
        this.surface = surface
        this.colorStage = colorStage
        this.alpha = alpha
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        startOffset = animationTime
        animationJob = coroutineScope.launch {
            val minimumDeltaNanos = 1_000_000_000L / 60L
            val origin = withFrameNanos { it }
            var lastFrame = origin
            while (isActive) {
                val frame = withFrameNanos { it }
                if (frame - lastFrame < minimumDeltaNanos) continue
                lastFrame = frame
                animationTime = startOffset + (frame - origin) / 1_000_000_000f
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawRect(surface)
        val effectAlpha = alpha()
        if (effectAlpha > 0f) {
            painter.update(
                width = size.width,
                height = size.height,
                time = animationTime,
                preset = preset,
                colorStage = colorStage(),
            )
            drawRect(painter.brush, alpha = effectAlpha)
        }
        drawContent()
    }
}

private const val OS3_BACKGROUND_SHADER = """
    uniform vec2 uResolution;
    uniform float uAnimTime;
    uniform vec4 uBound;
    uniform float uTranslateY;
    uniform vec3 uPoints[4];
    uniform vec2 uPointsAnim[4];
    uniform vec4 uColors[4];
    uniform float uAlphaMulti;
    uniform float uNoiseScale;
    uniform float uPointRadiusMulti;
    uniform float uSaturateOffset;
    uniform float uLightOffset;

    vec3 rgb2hsv(vec3 c) {
        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }

    vec3 hsv2rgb(vec3 c) {
        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }

    float hash(vec2 p) {
        vec3 p3 = fract(vec3(p.xyx) * 0.13);
        p3 += dot(p3, p3.yzx + 3.333);
        return fract((p3.x + p3.y) * p3.z);
    }

    float perlin(vec2 x) {
        vec2 i = floor(x);
        vec2 f = fract(x);
        float a = hash(i);
        float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0));
        float d = hash(i + vec2(1.0, 1.0));
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) +
            (d - b) * u.x * u.y;
    }

    float gradientNoise(in vec2 uv) {
        return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
    }

    vec4 main(vec2 fragCoord) {
        vec2 vUv = fragCoord / uResolution;
        vUv.y = 1.0 - vUv.y;
        vec2 uv = vUv;
        uv -= vec2(0.0, uTranslateY);
        uv.xy -= uBound.xy;
        uv.xy /= uBound.zw;

        vec4 color = vec4(0.0);
        float noiseValue = perlin(vUv * uNoiseScale + vec2(-uAnimTime));

        for (int i = 0; i < 4; i++) {
            vec4 pointColor = uColors[i];
            pointColor.rgb *= pointColor.a;
            vec2 point = uPointsAnim[i];
            float radius = uPoints[i].z * uPointRadiusMulti;
            float distanceToPoint = distance(uv, point);
            float percentage = smoothstep(radius, 0.0, distanceToPoint);
            color.rgb = mix(color.rgb, pointColor.rgb, percentage);
            color.a = mix(color.a, pointColor.a, percentage);
        }

        float oppositeNoise = smoothstep(0.0, 1.0, noiseValue);
        color.rgb /= color.a;
        vec3 hsv = rgb2hsv(color.rgb);
        hsv.y = mix(hsv.y, 0.0, oppositeNoise * uSaturateOffset);
        color.rgb = hsv2rgb(hsv);
        color.rgb += oppositeNoise * uLightOffset;
        color.a = clamp(color.a, 0.0, 1.0) * uAlphaMulti;
        color += (10.0 / 255.0) * gradientNoise(fragCoord.xy) - (5.0 / 255.0);
        return vec4(color.rgb * color.a, color.a);
    }
"""
