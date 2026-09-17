package com.pixeldual.fold.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosticHud(
    visible: Boolean,
    renderAngle: Float,
    rawHardwareAngle: Float,
    sensorRateHz: Int,
    isHardwareAvailable: Boolean,
    isSimulated: Boolean,
    isDualScreenActive: Boolean,
    isDualScreenSupported: Boolean,
    isDesktopOverlayEnabled: Boolean,
    useSoloPerspective: Boolean,
    hasCustomImage: Boolean,
    onTogglePerspective: () -> Unit,
    onToggleDualScreen: () -> Unit,
    onToggleDesktopOverlay: () -> Unit,
    onSimulateAngleChange: (Float) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onReloadLatestImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "PixelDualFold",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (hasCustomImage) "Custom wallpaper" else "Optical fold preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(
                            onClick = { onToggleSimulation(!isSimulated) },
                            label = {
                                Text(
                                    text = if (isSimulated) "Simulation" else "Live sensor",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }

                    Spacer(Modifier.width(1.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Cover display", style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    isDualScreenActive -> "Active"
                                    isDualScreenSupported -> "Ready"
                                    else -> "Unavailable"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDualScreenActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = isDualScreenActive,
                                onCheckedChange = { onToggleDualScreen() },
                                enabled = isDualScreenSupported || isDualScreenActive,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Desktop overlay", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Draw the fold visual above other apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isDesktopOverlayEnabled,
                            onCheckedChange = { onToggleDesktopOverlay() },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onPickImage, modifier = Modifier.weight(1f)) {
                            Text("Choose image")
                        }
                        TextButton(onClick = onReloadLatestImage) {
                            Text("Reload")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "%.1f°".format(renderAngle),
                                style = MaterialTheme.typography.displaySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Spring %.1f°  ·  HAL %.0f°  ·  %d Hz".format(
                                    renderAngle,
                                    rawHardwareAngle,
                                    sensorRateHz,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilterChip(
                            selected = useSoloPerspective,
                            onClick = onTogglePerspective,
                            label = { Text(if (useSoloPerspective) "Ray perspective" else "3D matrix") },
                        )
                    }

                    Slider(
                        value = renderAngle,
                        onValueChange = { newAngle ->
                            if (!isSimulated) onToggleSimulation(true)
                            onSimulateAngleChange(newAngle)
                        },
                        valueRange = 0f..180f,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Closed", style = MaterialTheme.typography.labelSmall)
                        Text("Flat", style = MaterialTheme.typography.labelSmall)
                    }

                    if (isSimulated && isHardwareAvailable) {
                        TextButton(
                            onClick = { onToggleSimulation(false) },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Use live hinge sensor")
                        }
                    }
                }
            }
        }
    }
}