package com.pixeldual.fold

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.pixeldual.fold.engine.DampedHingeTracker
import com.pixeldual.fold.ui.InnerFoldScreen
import com.pixeldual.fold.ui.PixelDualFoldTheme

class DesktopOverlayService : LifecycleService() {
    private lateinit var hingeTracker: DampedHingeTracker
    private var overlayView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        hingeTracker = DampedHingeTracker(applicationContext).also { it.start() }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DesktopOverlayService)
            setContent {
                val renderAngle by hingeTracker.renderAngle
                PixelDualFoldTheme {
                    InnerFoldScreen(
                        renderAngle = renderAngle,
                        rawHardwareAngle = hingeTracker.rawHardwareAngle.value,
                        sensorRateHz = hingeTracker.sensorRateHz.value,
                        isHardwareAvailable = hingeTracker.isHardwareAvailable,
                        isSimulated = hingeTracker.isSimulated.value,
                        isDualScreenActive = false,
                        isDualScreenSupported = false,
                        isDesktopOverlayEnabled = true,
                        customBitmap = null,
                        onToggleDualScreen = {},
                        onToggleDesktopOverlay = {},
                        onSimulateAngleChange = hingeTracker::setSimulatedAngle,
                        onToggleSimulation = hingeTracker::setSimulated,
                        onPickImage = {},
                        onReloadLatestImage = {},
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "PixelDualFold desktop overlay"
        }

        getSystemService(WindowManager::class.java).addView(view, layoutParams)
        overlayView = view
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { getSystemService(WindowManager::class.java).removeView(view) }
        }
        overlayView = null
        if (::hingeTracker.isInitialized) hingeTracker.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PixelDualFold overlay",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("PixelDualFold overlay active")
            .setContentText("Tracking hinge angle over the desktop")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "pixeldual_overlay"
        private const val NOTIFICATION_ID = 1001
    }
}