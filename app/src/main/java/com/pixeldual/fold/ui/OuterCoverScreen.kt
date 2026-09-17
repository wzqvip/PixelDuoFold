package com.pixeldual.fold.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pixeldual.fold.shader.SoloTiltRayShader

/**
 * High-fidelity Outer Cover Screen View.
 * Displays the Right Half of the wallpaper with optical inverse defocus:
 * - Near the hinge crease (left edge), it is crystal sharp.
 * - Outward to the right, subtle optical blur.
 * - As the device closes (90° -> 0°), the outer screen clarifies and becomes 100% sharp.
 */
@Composable
fun OuterCoverView(
    renderAngle: Float,
    customBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    val outerShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(SoloTiltRayShader.OUTER_COVER_RAY_SHADER)
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && outerShader != null) {
                        outerShader.setFloatUniform("size", size.width, size.height)
                        outerShader.setFloatUniform("hingeAngle", renderAngle)
                        renderEffect = RenderEffect
                            .createRuntimeShaderEffect(outerShader, "content")
                            .asComposeRenderEffect()
                    }
                },
        ) {
            WallpaperCoverView(
                customBitmap = customBitmap,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
