package com.pixeldual.fold.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WallpaperHalfView(
    isLeftHalf: Boolean,
    customBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (customBitmap != null) Color.Transparent else Color(0xFF0A0C14)),
    ) {
        if (customBitmap != null) {
            CustomBitmapHalf(
                bitmap = customBitmap,
                isLeftHalf = isLeftHalf,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ProceduralWallpaperBackground(isLeftHalf = isLeftHalf)

            if (isLeftHalf) {
                LeftHalfDesktopContent()
            } else {
                RightHalfDesktopContent()
            }
        }
    }
}

@Composable
fun WallpaperCoverView(
    customBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (customBitmap != null) Color.Transparent else Color(0xFF0A0C14)),
    ) {
        if (customBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val destinationWidth = size.width.toInt()
                val destinationHeight = size.height.toInt()
                val rightHalfLeft = customBitmap.width / 2
                val rightHalfWidth = customBitmap.width - rightHalfLeft
                val sourceAspect = rightHalfWidth.toFloat() / customBitmap.height
                val destinationAspect = destinationWidth.toFloat() / destinationHeight
                val sourceWidth: Int
                val sourceHeight: Int
                if (sourceAspect > destinationAspect) {
                    sourceHeight = customBitmap.height
                    sourceWidth = (customBitmap.height * destinationAspect).toInt().coerceAtMost(rightHalfWidth)
                } else {
                    sourceWidth = rightHalfWidth
                    sourceHeight = (rightHalfWidth / destinationAspect).toInt().coerceAtMost(customBitmap.height)
                }
                val sourceLeft = rightHalfLeft + (rightHalfWidth - sourceWidth) / 2
                val sourceTop = (customBitmap.height - sourceHeight) / 2
                drawImage(
                    image = customBitmap,
                    srcOffset = IntOffset(sourceLeft, sourceTop),
                    srcSize = IntSize(sourceWidth, sourceHeight),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(destinationWidth, destinationHeight),
                )
            }
        } else {
            ProceduralWallpaperBackground(isLeftHalf = false)
            RightHalfDesktopContent()
        }
    }
}

@Composable
private fun CustomBitmapHalf(
    bitmap: ImageBitmap,
    isLeftHalf: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val bmpW = bitmap.width
        val bmpH = bitmap.height

        val srcLeft = if (isLeftHalf) 0 else bmpW / 2
        val srcRight = if (isLeftHalf) bmpW / 2 else bmpW
        val srcRect = IntRect(srcLeft, 0, srcRight, bmpH)
        val dstSize = IntSize(size.width.toInt(), size.height.toInt())

        drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcRect.left, srcRect.top),
            srcSize = IntSize(srcRect.width, srcRect.height),
            dstOffset = IntOffset.Zero,
            dstSize = dstSize,
        )
    }
}

@Composable
private fun ProceduralWallpaperBackground(isLeftHalf: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Rich cosmic dusk gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E1B4B),
                    Color(0xFF2E1065),
                    Color(0xFF090D16),
                ),
                startY = 0f,
                endY = h,
            ),
        )

        if (isLeftHalf) {
            // Violet-cyan aurora glow on left half
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(w * 0.4f, h * 0.35f),
                    radius = w * 0.9f,
                ),
                center = Offset(w * 0.4f, h * 0.35f),
                radius = w * 0.9f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.7f),
                    radius = w * 0.7f,
                ),
                center = Offset(w * 0.85f, h * 0.7f),
                radius = w * 0.7f,
            )
        } else {
            // Amber-coral glow on right half
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.7f),
                    radius = w * 0.8f,
                ),
                center = Offset(w * 0.15f, h * 0.7f),
                radius = w * 0.8f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.6f, h * 0.3f),
                    radius = w * 0.85f,
                ),
                center = Offset(w * 0.6f, h * 0.3f),
                radius = w * 0.85f,
            )
        }
    }
}

@Composable
private fun LeftHalfDesktopContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "09:41",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = "Pixel 10 Pro Fold · 120Hz Ultra Smooth",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SPATIAL ARCHITECTURE",
                    color = Color(0xFF67E8F9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "iPhone Duo Fold Optics",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "120Hz Spring Damped · Vogel 16-Tap Golden Spiral",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            AppIconItem("Camera", Color(0xFFEF4444))
            AppIconItem("Photos", Color(0xFF3B82F6))
            AppIconItem("Notes", Color(0xFFEAB308))
            AppIconItem("Settings", Color(0xFF64748B))
        }
    }
}

@Composable
private fun RightHalfDesktopContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF38BDF8), CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search apps, dual-screen & system...",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6)),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spatial Depth Horizon",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Concurrent Outer Display · Playing",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            AppIconItem("Maps", Color(0xFF10B981))
            AppIconItem("Browser", Color(0xFF8B5CF6))
            AppIconItem("Terminal", Color(0xFF0F172A))
            AppIconItem("Files", Color(0xFFF59E0B))
        }
    }
}

@Composable
private fun AppIconItem(name: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(color, RoundedCornerShape(15.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
        )
    }
}
