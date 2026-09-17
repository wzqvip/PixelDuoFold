package com.pixeldual.fold.dualdisplay

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import com.pixeldual.fold.ui.OuterCoverView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages Dual-Screen Concurrent Display Mode on Google Pixel Foldables.
 *
 * Uses Jetpack WindowManager 1.3+ WindowAreaController to present content
 * on the physical outer cover display concurrently with the inner display.
 */
@OptIn(ExperimentalWindowApi::class)
class DualScreenManager(
    private val activity: ComponentActivity,
    private val coroutineScope: CoroutineScope,
) {
    private val TAG = "DualScreenManager"

    private val windowAreaController: WindowAreaController by lazy {
        WindowAreaController.getOrCreate()
    }

    private var currentSession: WindowAreaSessionPresenter? = null
    private var composeView: ComposeView? = null
    @Volatile
    private var isStartingSession = false

    private val _isDualScreenActive = mutableStateOf(false)
    val isDualScreenActive: State<Boolean> = _isDualScreenActive

    private val _isDualScreenSupported = mutableStateOf(false)
    val isDualScreenSupported: State<Boolean> = _isDualScreenSupported

    private var dualScreenInfo: WindowAreaInfo? = null

    init {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                windowAreaController.windowAreaInfos.collect { list ->
                    val info = list.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
                    dualScreenInfo = info
                    val capability = info?.getCapability(
                        WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA
                    )
                    val isSupported = capability?.status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE ||
                        capability?.status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE
                    _isDualScreenSupported.value = isSupported
                    Log.d(TAG, "WindowArea dual screen supported: $isSupported")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error collecting windowAreaInfos", e)
            }
        }
    }

    fun startDualScreenPresentation(
        renderAngle: Float,
        customBitmap: ImageBitmap?,
    ) {
        if (_isDualScreenActive.value || isStartingSession) {
            updatePresentationContent(renderAngle, customBitmap)
            return
        }

        val info = dualScreenInfo
        if (info == null) {
            Log.w(TAG, "dualScreenInfo is null, attempting root fallback")
            coroutineScope.launch {
                RootDeviceStateHelper.enableConcurrentMode()
            }
            return
        }

        val token = info.token
    isStartingSession = true
        try {
            windowAreaController.presentContentOnWindowArea(
                token = token,
                activity = activity,
                executor = ContextCompat.getMainExecutor(activity),
                windowAreaPresentationSessionCallback = object : WindowAreaPresentationSessionCallback {
                    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
                        try {
                            val view = ComposeView(session.context).apply {
                                // A presentation window does not inherit the activity's view tree owners.
                                setViewTreeLifecycleOwner(activity)
                                setViewTreeViewModelStoreOwner(activity)
                                setViewTreeSavedStateRegistryOwner(activity)
                                setContent {
                                    OuterCoverView(
                                        renderAngle = renderAngle,
                                        customBitmap = customBitmap,
                                    )
                                }
                            }
                            session.setContentView(view)
                            currentSession = session
                            composeView = view
                            isStartingSession = false
                            _isDualScreenActive.value = true
                            Log.d(TAG, "Dual Screen Presentation successfully mounted on Outer Display!")
                        } catch (e: Throwable) {
                            isStartingSession = false
                            Log.e(TAG, "Failed to mount Compose content on WindowArea session", e)
                            try {
                                session.close()
                            } catch (_: Throwable) {
                            }
                        }
                    }

                    override fun onSessionEnded(t: Throwable?) {
                        isStartingSession = false
                        currentSession = null
                        composeView = null
                        _isDualScreenActive.value = false
                        Log.d(TAG, "Dual Screen Session ended: ${t?.message}")
                    }

                    override fun onContainerVisibilityChanged(isVisible: Boolean) {
                        Log.d(TAG, "Outer container visibility changed: $isVisible")
                    }
                }
            )
        } catch (e: Throwable) {
            isStartingSession = false
            Log.e(TAG, "Failed to start WindowArea presentation session", e)
        }
    }

    fun updatePresentationContent(
        renderAngle: Float,
        customBitmap: ImageBitmap?,
    ) {
        composeView?.setContent {
            OuterCoverView(
                renderAngle = renderAngle,
                customBitmap = customBitmap,
            )
        }
    }

    fun stopDualScreenPresentation() {
        try {
            currentSession?.close()
        } catch (_: Throwable) {
        }
        currentSession = null
        composeView = null
        isStartingSession = false
        _isDualScreenActive.value = false
        coroutineScope.launch {
            RootDeviceStateHelper.resetDeviceState()
        }
    }
}
