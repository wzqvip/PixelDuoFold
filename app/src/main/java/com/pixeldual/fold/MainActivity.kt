package com.pixeldual.fold

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pixeldual.fold.dualdisplay.DualScreenManager
import com.pixeldual.fold.dualdisplay.RootDeviceStateHelper
import com.pixeldual.fold.engine.DampedHingeTracker
import com.pixeldual.fold.ui.InnerFoldScreen
import com.pixeldual.fold.ui.OuterCoverView
import com.pixeldual.fold.ui.PixelDualFoldTheme
import com.pixeldual.fold.util.MediaStoreHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        // Prevent fold-to-sleep: Keep screen energized when folding
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            PixelDualFoldTheme {
            val scope = rememberCoroutineScope()
            val hingeTracker = remember { DampedHingeTracker(applicationContext) }
            val dualScreenManager = remember { DualScreenManager(this, lifecycleScope) }

            val renderAngle by hingeTracker.renderAngle
            val rawHardwareAngle by hingeTracker.rawHardwareAngle
            val isSimulated by hingeTracker.isSimulated
            val sensorRateHz by hingeTracker.sensorRateHz
            val isHardwareAvailable = hingeTracker.isHardwareAvailable

            val isDualScreenActive by dualScreenManager.isDualScreenActive
            val isDualScreenSupported by dualScreenManager.isDualScreenSupported
            val configuration = LocalConfiguration.current
            val isCoverDisplay = configuration.screenWidthDp < 600
            val shouldShowCover = isCoverDisplay || renderAngle <= 50f
            val concurrentPhase = when {
                !isCoverDisplay && renderAngle <= 60f -> 1
                renderAngle >= 90f -> 0
                else -> 2
            }
            var concurrentOverrideRequested by remember { mutableStateOf(false) }
            var isDesktopOverlayEnabled by remember { mutableStateOf(false) }

            var customBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current

            // Permission handling for photo gallery
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                if (isGranted) {
                    customBitmap = MediaStoreHelper.loadLatestGalleryImage(applicationContext)
                }
            }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
            ) { uri ->
                uri?.let {
                    try {
                        contentResolver.openInputStream(it)?.use { stream ->
                            val bytes = stream.readBytes()
                            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                customBitmap = bmp.asImageBitmap()
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                    customBitmap = MediaStoreHelper.loadLatestGalleryImage(applicationContext)
                } else {
                    permissionLauncher.launch(permission)
                }
            }

            // Sync outer display presentation content with 120Hz render angle
            LaunchedEffect(renderAngle, customBitmap) {
                if (!isCoverDisplay && isDualScreenActive) {
                    dualScreenManager.updatePresentationContent(renderAngle, customBitmap)
                }
            }

            // Auto-engage dual-screen concurrent mode when folding down past 135°
            LaunchedEffect(renderAngle) {
                if (!isCoverDisplay && renderAngle <= 135f && !isDualScreenActive && isDualScreenSupported) {
                    dualScreenManager.startDualScreenPresentation(renderAngle, customBitmap)
                }
            }

            LaunchedEffect(concurrentPhase) {
                when (concurrentPhase) {
                    1 -> {
                        if (!concurrentOverrideRequested) {
                            concurrentOverrideRequested = RootDeviceStateHelper.enableConcurrentMode()
                        }
                    }
                    0 -> {
                        if (concurrentOverrideRequested) {
                            RootDeviceStateHelper.resetDeviceState()
                            concurrentOverrideRequested = false
                        }
                    }
                }
            }

            // Lifecycle observation for high-priority 120Hz tracker
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> hingeTracker.start()
                        Lifecycle.Event.ON_STOP -> {
                            hingeTracker.stop()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    hingeTracker.stop()
                    dualScreenManager.stopDualScreenPresentation()
                }
            }

            if (shouldShowCover) {
                OuterCoverView(
                    renderAngle = renderAngle,
                    customBitmap = customBitmap,
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                )
            } else {
                InnerFoldScreen(
                    renderAngle = renderAngle,
                    rawHardwareAngle = rawHardwareAngle,
                    sensorRateHz = sensorRateHz,
                    isHardwareAvailable = isHardwareAvailable,
                    isSimulated = isSimulated,
                    isDualScreenActive = isDualScreenActive,
                    isDualScreenSupported = isDualScreenSupported,
                    isDesktopOverlayEnabled = isDesktopOverlayEnabled,
                    customBitmap = customBitmap,
                    onToggleDualScreen = {
                        if (isDualScreenActive) {
                            dualScreenManager.stopDualScreenPresentation()
                        } else {
                            dualScreenManager.startDualScreenPresentation(renderAngle, customBitmap)
                        }
                    },
                    onToggleDesktopOverlay = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ),
                            )
                        } else {
                            val serviceIntent = Intent(this@MainActivity, DesktopOverlayService::class.java)
                            if (isDesktopOverlayEnabled) {
                                stopService(serviceIntent)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                            isDesktopOverlayEnabled = !isDesktopOverlayEnabled
                        }
                    },
                    onSimulateAngleChange = { angle -> hingeTracker.setSimulatedAngle(angle) },
                    onToggleSimulation = { enabled -> hingeTracker.setSimulated(enabled) },
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    onReloadLatestImage = {
                        val latest = MediaStoreHelper.loadLatestGalleryImage(applicationContext)
                        if (latest != null) {
                            customBitmap = latest
                        } else {
                            imagePickerLauncher.launch("image/*")
                        }
                    },
                )
            }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
