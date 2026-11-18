package com.melox.player.ui.component.playback

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import top.yukonga.miuix.kmp.material.RuntimeShader
import top.yukonga.miuix.kmp.material.asComposeShader

/** Final-output noise is never enlarged with the working bitmap or fed into frame history. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class DynamicFlowDitherPainter {
    private val shader = RuntimeShader(DYNAMIC_FLOW_DITHER_SHADER)
    private val paint = Paint().apply { shader = this@DynamicFlowDitherPainter.shader.asComposeShader() }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        current: Shader,
        previous: Shader,
        progress: Float,
        dark: Boolean = true,
    ) {
        shader.setInputShader("currentFrame", current)
        shader.setInputShader("previousFrame", previous)
        shader.setFloatUniform("progress", progress)
        shader.setFloatUniform("height", height)
        shader.setFloatUniform("lightStyle", if (dark) 0f else 1f)
        canvas.drawRect(0f, 0f, width, height, paint)
    }
}

private const val DYNAMIC_FLOW_DITHER_SHADER = """
uniform shader currentFrame;
uniform shader previousFrame;
uniform float progress;
uniform float height;
uniform float lightStyle;

half4 main(float2 coord) {
    float4 current = currentFrame.eval(coord);
    float4 color = current;
    if (progress < 1.0) {
        float4 previous = previousFrame.eval(coord);
        // Frames are opaque. Difference-based interpolation preserves identical samples exactly.
        color = previous + (current - previous) * progress;
    }
    float y = clamp(coord.y / height, 0.0, 1.0);
    float scrim = y < 0.5 ? 0.18 * (1.0 - y * 2.0) : 0.30 * (y * 2.0 - 1.0);
    color.rgb *= 1.0 - scrim;
    color.rgb += float3(lightStyle * scrim);
    color.a = scrim + color.a * (1.0 - scrim);
    // Interleaved gradient noise is fixed at physical-pixel centers; there is no time uniform.
    float noise = fract(52.9829189 * fract(dot(floor(coord), float2(0.06711056, 0.00583715))));
    color.rgb = clamp(color.rgb + (noise - 0.5) / 255.0 * color.a, 0.0, color.a);
    return half4(color);
}
"""
