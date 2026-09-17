package com.pixeldual.fold.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldual.fold.shader.SoloTiltRayShader

@Composable
fun InnerFoldScreen(
    renderAngle: Float,
    rawHardwareAngle: Float,
    sensorRateHz: Int,
    isHardwareAvailable: Boolean,
    isSimulated: Boolean,
    isDualScreenActive: Boolean,
    isDualScreenSupported: Boolean,
    isDesktopOverlayEnabled: Boolean,
    customBitmap: ImageBitmap?,
    onToggleDualScreen: () -> Unit,
    onToggleDesktopOverlay: () -> Unit,
    onSimulateAngleChange: (Float) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onReloadLatestImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHud by remember { mutableStateOf(false) }
    var useSoloPerspective by remember { mutableStateOf(true) }
    var showOuterPreviewExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    val innerShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(SoloTiltRayShader.INNER_LEFT_RAY_SHADER)
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                showHud = !showHud
            },
    ) {
        // --- SEAMLESS DUAL-PANE INNER LAYOUT ---
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half: Folding surface with 120Hz smooth optical ray shader
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black)
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && innerShader != null && renderAngle < 179.9f) {
                            innerShader.setFloatUniform("size", size.width, size.height)
                            innerShader.setFloatUniform("hingeAngle", renderAngle)
                            innerShader.setFloatUniform("useShaderPerspective", if (useSoloPerspective) 1.0f else 0.0f)
                            renderEffect = RenderEffect
                                .createRuntimeShaderEffect(innerShader, "content")
                                .asComposeRenderEffect()
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (!useSoloPerspective) {
                                val turn = (180f - renderAngle).coerceIn(0f, 180f)
                                rotationY = -(turn * 0.36f)
                                scaleX = 1.0f + (turn / 180f) * 1.5f
                                cameraDistance = 14f * density
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            }
                        },
                ) {
                    WallpaperHalfView(
                        isLeftHalf = true,
                        customBitmap = customBitmap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Right Half: Stationary Anchor, 100% unblurred & razor sharp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                WallpaperHalfView(
                    isLeftHalf = false,
                    customBitmap = customBitmap,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // --- COVER SCREEN PREVIEW HUD THUMBNAIL (when angle <= 135°) ---
        AnimatedVisibility(
            visible = showHud && renderAngle <= 135f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 24.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F1420).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .clickable { showOuterPreviewExpanded = !showOuterPreviewExpanded }
                    .width(if (showOuterPreviewExpanded) 220.dp else 140.dp)
                    .height(if (showOuterPreviewExpanded) 320.dp else 200.dp),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "OUTER DISPLAY",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = if (isDualScreenActive) "CONCURRENT" else "PIP PREVIEW",
                            color = if (isDualScreenActive) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    ) {
                        OuterCoverView(
                            renderAngle = renderAngle,
                            customBitmap = customBitmap,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // --- INTERACTIVE DIAGNOSTIC HUD & DEBUG SLIDER ---
        DiagnosticHud(
            visible = showHud,
            renderAngle = renderAngle,
            rawHardwareAngle = rawHardwareAngle,
            sensorRateHz = sensorRateHz,
            isHardwareAvailable = isHardwareAvailable,
            isSimulated = isSimulated,
            isDualScreenActive = isDualScreenActive,
            isDualScreenSupported = isDualScreenSupported,
            isDesktopOverlayEnabled = isDesktopOverlayEnabled,
            useSoloPerspective = useSoloPerspective,
            hasCustomImage = customBitmap != null,
            onTogglePerspective = { useSoloPerspective = !useSoloPerspective },
            onToggleDualScreen = onToggleDualScreen,
            onToggleDesktopOverlay = onToggleDesktopOverlay,
            onSimulateAngleChange = onSimulateAngleChange,
            onToggleSimulation = onToggleSimulation,
            onPickImage = onPickImage,
            onReloadLatestImage = onReloadLatestImage,
        )
    }
}
