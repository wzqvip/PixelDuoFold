package com.pixeldual.fold.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.Choreographer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlin.math.exp

/**
 * High-performance 120Hz Critically Damped Physics Spring Hinge Angle Tracker.
 *
 * Resolves Google Pixel Fold 5.00° HAL quantization stepping:
 * 1. Collects raw Sensor.TYPE_HINGE_ANGLE on a dedicated high-priority HandlerThread.
 * 2. Drives an analytical critically-damped spring oscillator synchronized with the
 *    device's 120Hz display refresh rate (Choreographer frame callbacks).
 * 3. Smoothly interpolates the discrete 5° hardware jumps into a silky-smooth
 *    continuous angle curve with 0.01° sub-degree precision.
 */
class DampedHingeTracker(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    val isHardwareAvailable: Boolean = hingeSensor != null

    // Raw sensor angle (5° steps from HAL)
    private val _rawHardwareAngle = mutableFloatStateOf(180f)
    val rawHardwareAngle: State<Float> = _rawHardwareAngle

    // 120Hz Critically Damped Smooth Render Angle
    private val _renderAngle = mutableFloatStateOf(180f)
    val renderAngle: State<Float> = _renderAngle

    // Developer debug simulation
    private val _simulatedAngle = mutableFloatStateOf(180f)
    val simulatedAngle: State<Float> = _simulatedAngle

    private val _isSimulated = mutableStateOf(!isHardwareAvailable)
    val isSimulated: State<Boolean> = _isSimulated

    // Live sampling frequency (Hz)
    private val _sensorRateHz = mutableIntStateOf(0)
    val sensorRateHz: State<Int> = _sensorRateHz

    // Spring physics parameters
    // Natural frequency (rad/s): ~22 rad/s provides an instant ~50-60ms physical response
    private var naturalFrequency = 22.0f
    private var currentPosition = 180f
    private var currentVelocity = 0f
    private var targetAngle = 180f

    // Sampling rate calculation
    private var eventCount = 0
    private var lastRateCalcTimeNs = 0L

    @Volatile
    private var isRunning = false
    private var sensorThread: HandlerThread? = null

    // Choreographer frame callback for 120Hz rendering loop
    private var lastFrameTimeNs = 0L
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            if (lastFrameTimeNs == 0L) {
                lastFrameTimeNs = frameTimeNanos
            }

            val dt = ((frameTimeNanos - lastFrameTimeNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
            lastFrameTimeNs = frameTimeNanos

            // Analytical critically damped spring step:
            // Ensures 100% unconditional stability without overshoot
            val target = if (_isSimulated.value || !isHardwareAvailable) {
                _simulatedAngle.floatValue
            } else {
                targetAngle
            }

            val omega = naturalFrequency
            val y = currentPosition - target
            val decay = exp(-omega * dt)

            val newPos = target + (y + (currentVelocity + omega * y) * dt) * decay
            val newVel = (currentVelocity - omega * (currentVelocity + omega * y) * dt) * decay

            currentPosition = newPos.coerceIn(0f, 180f)
            currentVelocity = newVel

            // Update Compose state for 120 FPS render loop
            _renderAngle.floatValue = currentPosition

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun setSimulatedAngle(angle: Float) {
        val clamped = angle.coerceIn(0f, 180f)
        _simulatedAngle.floatValue = clamped
        targetAngle = clamped
    }

    fun setSimulated(enabled: Boolean) {
        _isSimulated.value = enabled
        if (enabled) {
            targetAngle = _simulatedAngle.floatValue
        } else {
            targetAngle = _rawHardwareAngle.floatValue
        }
    }

    fun setSpringStiffness(frequencyRadPerSec: Float) {
        naturalFrequency = frequencyRadPerSec.coerceIn(5f, 50f)
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        lastFrameTimeNs = 0L

        // Start Choreographer 120Hz loop on Main Looper
        Choreographer.getInstance().postFrameCallback(frameCallback)

        // Register hardware sensor on dedicated high-priority thread
        hingeSensor?.let { sensor ->
            val thread = HandlerThread("PixelHingeSensorThread", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply {
                start()
            }
            sensorThread = thread
            val handler = Handler(thread.looper)

            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                0,
                handler,
            )
        }
    }

    @Synchronized
    fun stop() {
        if (!isRunning) return
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        sensorManager?.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        _sensorRateHz.intValue = 0
        eventCount = 0
        lastRateCalcTimeNs = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            val raw = event.values.firstOrNull() ?: return
            val clamped = raw.coerceIn(0f, 180f)
            _rawHardwareAngle.floatValue = clamped
            targetAngle = clamped

            // Measure live sensor event rate
            val now = System.nanoTime()
            eventCount++
            if (lastRateCalcTimeNs == 0L) {
                lastRateCalcTimeNs = now
            } else {
                val elapsed = now - lastRateCalcTimeNs
                if (elapsed >= 500_000_000L) {
                    val hz = ((eventCount.toDouble() / elapsed.toDouble()) * 1_000_000_000L).toInt()
                    _sensorRateHz.intValue = hz
                    eventCount = 0
                    lastRateCalcTimeNs = now
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
