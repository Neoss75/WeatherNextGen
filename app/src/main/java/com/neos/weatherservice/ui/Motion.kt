package com.neos.weatherservice.ui

import android.animation.TimeInterpolator
import android.content.Context
import android.view.Choreographer
import android.view.View
import android.view.animation.PathInterpolator
import androidx.preference.PreferenceManager
import com.neos.weatherservice.Prefs
import com.neos.weatherservice.R

/**
 * Animations of the main screen.
 *
 * They run on [Choreographer] frames with real time instead of Android's animators, because many
 * car head units ship with the system animation scale set to 0, which makes every
 * ValueAnimator/ViewPropertyAnimator jump straight to its end. Whether to animate is the
 * app's own setting ([Prefs.KEY_ANIMATIONS]).
 */
object Motion {

    /** Shared clock origin, so the weather picture and the backdrop flash in sync. */
    val epoch: Long = android.os.SystemClock.uptimeMillis()

    /** Material "emphasized decelerate"-like curve for things entering the screen. */
    val enter: TimeInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    /** Material standard curve for things changing in place. */
    val standard: TimeInterpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    /** Ease in and out, for looping motion. */
    val smooth: TimeInterpolator = PathInterpolator(0.4f, 0f, 0.6f, 1f)

    fun enabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Prefs.KEY_ANIMATIONS, true)

    /**
     * A tween from 0 to 1 over [durationMs], driven by display frames.
     * With [loop] it goes back and forth until cancelled.
     */
    class Tween(
        private val durationMs: Long,
        private val interpolator: TimeInterpolator,
        private val delayMs: Long = 0,
        private val loop: Boolean = false,
        private val onUpdate: (Float) -> Unit,
    ) : Choreographer.FrameCallback {
        private var startNanos = -1L
        private var running = false
        private var onEnd: (() -> Unit)? = null

        fun then(action: () -> Unit) = apply { onEnd = action }

        fun start() = apply {
            running = true
            startNanos = -1L
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun cancel() {
            running = false
            Choreographer.getInstance().removeFrameCallback(this)
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (startNanos < 0) startNanos = frameTimeNanos + delayMs * 1_000_000
            val elapsedMs = (frameTimeNanos - startNanos) / 1_000_000f
            if (elapsedMs < 0) {
                Choreographer.getInstance().postFrameCallback(this)
                return
            }
            var t = elapsedMs / durationMs
            if (loop) {
                val cycle = t % 2f
                t = if (cycle > 1f) 2f - cycle else cycle
            } else if (t >= 1f) {
                running = false
                onUpdate(interpolator.getInterpolation(1f))
                onEnd?.invoke()
                return
            }
            onUpdate(interpolator.getInterpolation(t.coerceIn(0f, 1f)))
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Linear blend of [a] and [b]. */
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

    /**
     * Animates view properties from their current values to the given ones.
     * A new animation of the same view cancels the previous one.
     */
    fun animate(
        view: View,
        durationMs: Long,
        interpolator: TimeInterpolator = standard,
        delayMs: Long = 0,
        alpha: Float? = null,
        translationY: Float? = null,
        scale: Float? = null,
        rotation: Float? = null,
    ): Tween {
        (view.getTag(R.id.motion_tween) as? Tween)?.cancel()
        val a0 = view.alpha
        val y0 = view.translationY
        val s0 = view.scaleX
        val r0 = view.rotation
        val tween = Tween(durationMs, interpolator, delayMs) { f ->
            alpha?.let { view.alpha = lerp(a0, it, f) }
            translationY?.let { view.translationY = lerp(y0, it, f) }
            scale?.let {
                view.scaleX = lerp(s0, it, f)
                view.scaleY = view.scaleX
            }
            rotation?.let { view.rotation = lerp(r0, it, f) }
        }
        view.setTag(R.id.motion_tween, tween)
        return tween
    }

    /** Overshoot curve ("a bit past the target and back"). */
    fun overshoot(tension: Float): TimeInterpolator = TimeInterpolator { t ->
        val x = t - 1f
        x * x * ((tension + 1) * x + tension) + 1f
    }
}
