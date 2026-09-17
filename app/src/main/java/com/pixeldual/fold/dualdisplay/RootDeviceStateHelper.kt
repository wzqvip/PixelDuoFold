package com.pixeldual.fold.dualdisplay

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Root Device State Helper.
 * Leverages KernelSU/Root permissions to force DeviceState 4 (CONCURRENT_INNER_DEFAULT)
 * on Google Pixel Foldables, ensuring both displays stay illuminated simultaneously.
 */
object RootDeviceStateHelper {
    private const val TAG = "RootDeviceStateHelper"

    suspend fun enableConcurrentMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd device_state state 4"))
            val exitCode = process.waitFor()
            Log.d(TAG, "enableConcurrentMode result: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable concurrent mode via root", e)
            false
        }
    }

    suspend fun resetDeviceState(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd device_state state reset"))
            val exitCode = process.waitFor()
            Log.d(TAG, "resetDeviceState result: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset device state via root", e)
            false
        }
    }
}
