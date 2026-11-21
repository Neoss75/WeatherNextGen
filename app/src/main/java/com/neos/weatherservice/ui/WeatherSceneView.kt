package com.neos.weatherservice.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated weather picture drawn on a canvas: a turning sun, a moon with twinkling stars,
 * drifting clouds, falling rain or snow, lightning and fog. Draws a still frame when the
 * system animations are turned off.
 */
class WeatherSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var scene: Scene = WeatherStyle.idleScene
        set(value) {
            if (field == value) return
            field = value
            rebuild()
            invalidate()
        }

    private val startTime = Motion.epoch
    private var size = 0f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val sunGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonGlow = Paint(Paint.ANTI_ALIAS_FLAG)

    private val frontCloud = Path()
    private val backCloud = Path()
    private val moon = Path()
    private val bolt = Path()

    // Celestial body position and radius, in pixels.
    private var bodyX = 0f
    private var bodyY = 0f
    private var bodyR = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        size = min(w, h).toFloat()
        rebuild()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Square by default: the scene is drawn in a size × size box.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val side = when {
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED -> w
            MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED -> h
            else -> min(w, h)
        }
        setMeasuredDimension(
            resolveSize(side, widthMeasureSpec),
            resolveSize(side, heightMeasureSpec),
        )
    }

    /** Recomputes the paths that depend on the size and the scene. */
    private fun rebuild() {
        if (size <= 0f) return
        val s = size
        val alone = scene.clouds == 0
        bodyR = s * if (alone) 0.2f else 0.15f
        bodyX = s * if (alone) 0.5f else 0.64f
        bodyY = s * if (alone) 0.5f else 0.34f

        cloudPath(frontCloud, s * 0.46f, s * 0.58f, s * 0.62f)
        cloudPath(backCloud, s * 0.62f, s * 0.42f, s * 0.42f)

        moon.reset()
        val disc = Path().apply { addCircle(bodyX, bodyY, bodyR, Path.Direction.CW) }
        val cut = Path().apply {
            addCircle(bodyX + bodyR * 0.55f, bodyY - bodyR * 0.35f, bodyR * 0.85f, Path.Direction.CW)
        }
        moon.op(disc, cut, Path.Op.DIFFERENCE)

        sunGlow.shader = RadialGradient(
            bodyX, bodyY, bodyR * 2.1f, intArrayOf(0x66FFD54F, 0x00FFD54F), null, Shader.TileMode.CLAMP,
        )
        moonGlow.shader = RadialGradient(
            bodyX, bodyY, bodyR * 1.9f, intArrayOf(0x40FFF3C4, 0x00FFF3C4), null, Shader.TileMode.CLAMP,
        )

        bolt.reset()
        bolt.moveTo(s * 0.50f, s * 0.66f)
        bolt.lineTo(s * 0.41f, s * 0.82f)
        bolt.lineTo(s * 0.48f, s * 0.82f)
        bolt.lineTo(s * 0.43f, s * 0.96f)
        bolt.lineTo(s * 0.58f, s * 0.76f)
        bolt.lineTo(s * 0.51f, s * 0.76f)
        bolt.lineTo(s * 0.57f, s * 0.66f)
        bolt.close()
    }

    /** A flat-bottomed cloud of [width] centred at ([cx], [cy]), merged into one outline. */
    private fun cloudPath(path: Path, cx: Float, cy: Float, width: Float) {
        path.reset()
        val h = width * 0.32f
        val left = cx - width / 2
        val bottom = cy + h / 2
        val part = Path()
        fun union(build: Path.() -> Unit) {
            part.reset()
            part.build()
            path.op(part, Path.Op.UNION)
        }
        union { addRoundRect(left, bottom - h * 0.62f, left + width, bottom, h * 0.31f, h * 0.31f, Path.Direction.CW) }
        union { addCircle(left + width * 0.32f, bottom - h * 0.62f, width * 0.19f, Path.Direction.CW) }
        union { addCircle(left + width * 0.60f, bottom - h * 0.72f, width * 0.25f, Path.Direction.CW) }
        union { addCircle(left + width * 0.82f, bottom - h * 0.5f, width * 0.14f, Path.Direction.CW) }
    }

    /** Set from the app's "Animations" setting; false draws a still frame. */
    var animated = true
        set(value) {
            field = value
            invalidate()
        }

    private val animating get() = animated && windowVisibility == VISIBLE && isAttachedToWindow

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (size <= 0f) return
        // A fixed moment of the loop when animations are off: sun rays, drops and flakes in view.
        val t = if (animating) (SystemClock.uptimeMillis() - startTime) / 1000f else 1.3f
        val dx = (width - size) / 2
        val dy = (height - size) / 2
        canvas.save()
        canvas.translate(dx, dy)

        when (scene.celestial) {
            Scene.Celestial.SUN -> drawSun(canvas, t)
            Scene.Celestial.MOON -> drawMoon(canvas, t)
            Scene.Celestial.NONE -> Unit
        }
        if (scene.clouds > 0) drawClouds(canvas, t)
        when (scene.precipitation) {
            Scene.Precipitation.RAIN -> drawRain(canvas, t)
            Scene.Precipitation.SNOW -> drawSnow(canvas, t)
            Scene.Precipitation.NONE -> Unit
        }
        if (scene.lightning) drawLightning(canvas, t)
        if (scene.fog) drawFog(canvas, t)

        canvas.restore()
        if (animating) postInvalidateOnAnimation()
    }

    private fun drawSun(canvas: Canvas, t: Float) {
        val pulse = 1f + 0.06f * sin(t * 1.6f)
        canvas.save()
        canvas.scale(pulse, pulse, bodyX, bodyY)
        canvas.drawCircle(bodyX, bodyY, bodyR * 2.1f, sunGlow)
        canvas.restore()

        stroke.color = 0xFFFFC107.toInt()
        stroke.strokeWidth = bodyR * 0.16f
        val angle = t * 18f
        canvas.save()
        canvas.rotate(angle, bodyX, bodyY)
        for (i in 0 until 12) {
            val a = (i * 30.0) * PI / 180
            val inner = bodyR * 1.3f
            val outer = bodyR * if (i % 2 == 0) 1.65f else 1.5f
            canvas.drawLine(
                bodyX + (cos(a) * inner).toFloat(), bodyY + (sin(a) * inner).toFloat(),
                bodyX + (cos(a) * outer).toFloat(), bodyY + (sin(a) * outer).toFloat(), stroke,
            )
        }
        canvas.restore()

        fill.color = 0xFFFFD54F.toInt()
        canvas.drawCircle(bodyX, bodyY, bodyR, fill)
        fill.color = 0x55FFFFFF
        canvas.drawCircle(bodyX - bodyR * 0.3f, bodyY - bodyR * 0.3f, bodyR * 0.35f, fill)
    }

    private fun drawMoon(canvas: Canvas, t: Float) {
        // Stars around the moon, each twinkling at its own pace.
        for (i in STARS.indices step 3) {
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * STARS[i + 2] + i))
            fill.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            canvas.drawCircle(STARS[i] * size, STARS[i + 1] * size, size * 0.012f, fill)
        }
        canvas.drawCircle(bodyX, bodyY, bodyR * 1.9f, moonGlow)
        canvas.save()
        canvas.rotate(8f * sin(t * 0.5f), bodyX, bodyY)
        fill.color = 0xFFFFF3C4.toInt()
        canvas.drawPath(moon, fill)
        canvas.restore()
    }

    private fun drawClouds(canvas: Canvas, t: Float) {
        val drift = size * 0.06f
        if (scene.clouds >= 2) {
            fill.color = if (scene.darkClouds) 0xFF78858F.toInt() else 0xFFB8C6D1.toInt()
            canvas.save()
            canvas.translate(-sin(t * 0.5f + 1f) * drift * 1.3f, sin(t * 0.7f) * drift * 0.2f)
            canvas.drawPath(backCloud, fill)
            canvas.restore()
        }
        fill.color = if (scene.darkClouds) 0xFF9AA7B1.toInt() else 0xF5FFFFFF.toInt()
        canvas.save()
        canvas.translate(sin(t * 0.7f) * drift, cos(t * 0.9f) * drift * 0.3f)
        canvas.drawPath(frontCloud, fill)
        canvas.restore()
    }

    private fun drawRain(canvas: Canvas, t: Float) {
        val count = (6 + 6 * scene.intensity).toInt()
        stroke.color = 0xFFB3E5FC.toInt()
        stroke.strokeWidth = size * 0.018f
        val top = size * 0.70f
        val fall = size * 0.28f
        val len = size * (0.04f + 0.04f * scene.intensity)
        for (i in 0 until count) {
            val x = size * (0.24f + 0.46f * i / (count - 1))
            val p = ((t * (1.1f + 0.5f * scene.intensity) + PHASES[i % PHASES.size]) % 1f)
            val y = top + p * fall
            stroke.alpha = ((1f - p) * 255).toInt()
            canvas.drawLine(x, y, x - len * 0.25f, y + len, stroke)
        }
        stroke.alpha = 255
    }

    private fun drawSnow(canvas: Canvas, t: Float) {
        val count = (5 + 5 * scene.intensity).toInt()
        fill.color = Color.WHITE
        val top = size * 0.70f
        val fall = size * 0.28f
        for (i in 0 until count) {
            val p = ((t * 0.35f + PHASES[i % PHASES.size]) % 1f)
            val x = size * (0.24f + 0.46f * i / (count - 1)) + sin(t * 1.5f + i) * size * 0.025f
            fill.alpha = ((1f - p * p) * 255).toInt()
            canvas.drawCircle(x, top + p * fall, size * (0.016f + 0.006f * (i % 3)), fill)
        }
        fill.alpha = 255
    }

    private fun drawLightning(canvas: Canvas, t: Float) {
        // Two quick flashes every 3.5 s.
        val c = t % 3.5f
        val on = c < 0.12f || c in 0.22f..0.30f
        if (!on && animating) return
        fill.color = 0xFFFFEB3B.toInt()
        canvas.drawPath(bolt, fill)
    }

    private fun drawFog(canvas: Canvas, t: Float) {
        stroke.color = 0xD9FFFFFF.toInt()
        stroke.strokeWidth = size * 0.035f
        for (i in 0 until 3) {
            val y = size * (0.74f + 0.09f * i)
            val shift = sin(t * 0.7f + i * 1.7f) * size * 0.06f
            val half = size * (0.26f - 0.04f * i)
            canvas.drawLine(size / 2 - half + shift, y, size / 2 + half + shift, y, stroke)
        }
    }

    private companion object {
        /** Stars around the moon: x, y (fractions of the size) and twinkle speed. */
        val STARS = floatArrayOf(
            0.20f, 0.22f, 2.1f,
            0.32f, 0.12f, 1.4f,
            0.18f, 0.48f, 1.8f,
            0.86f, 0.16f, 2.6f,
            0.88f, 0.58f, 1.2f,
            0.40f, 0.30f, 2.9f,
        )

        /** Start offsets that spread drops and flakes out in time. */
        val PHASES = floatArrayOf(0f, 0.55f, 0.25f, 0.8f, 0.4f, 0.1f, 0.7f, 0.35f, 0.9f, 0.6f, 0.15f, 0.45f)
    }
}
