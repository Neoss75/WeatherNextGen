package com.neos.weatherservice.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen weather ambience behind the content: rain or snow across the whole screen,
 * twinkling stars and shooting stars at night, sun rays, slowly drifting clouds, fog and
 * lightning flashes. Everything is soft and translucent so the text stays readable.
 */
class WeatherBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var scene: Scene = WeatherStyle.idleScene
        set(value) {
            if (field == value) return
            field = value
            fadeStart = SystemClock.uptimeMillis()
            invalidate()
        }

    /** Set from the app's "Animations" setting; false draws nothing but the gradient. */
    var animated = true
        set(value) {
            field = value
            invalidate()
        }

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloud = Path()
    private val ray = Path()

    /** Particles: x and y as fractions of the view, speed, size and phase, all random. */
    private val count = 60
    private val px = FloatArray(count)
    private val py = FloatArray(count)
    private val speed = FloatArray(count)
    private val sizeF = FloatArray(count)
    private val phase = FloatArray(count)

    /** When the scene last changed: the new ambience fades in. */
    private var fadeStart = 0L

    init {
        val r = Random(75)
        for (i in 0 until count) {
            px[i] = r.nextFloat()
            py[i] = r.nextFloat()
            speed[i] = 0.6f + 0.8f * r.nextFloat()
            sizeF[i] = r.nextFloat()
            phase[i] = r.nextFloat() * 6.28f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // A soft cloud 1 unit wide; drawn scaled to different sizes.
        cloud.reset()
        val part = Path()
        fun union(build: Path.() -> Unit) {
            part.reset()
            part.build()
            cloud.op(part, Path.Op.UNION)
        }
        union { addRoundRect(0f, 0.12f, 1f, 0.32f, 0.1f, 0.1f, Path.Direction.CW) }
        union { addCircle(0.32f, 0.14f, 0.19f, Path.Direction.CW) }
        union { addCircle(0.6f, 0.1f, 0.25f, Path.Direction.CW) }
        union { addCircle(0.84f, 0.18f, 0.14f, Path.Direction.CW) }

        val cx = w * 0.88f
        val cy = -h * 0.04f
        val reach = max(w, h) * 0.9f
        glow.shader = RadialGradient(
            cx, cy, reach * 0.55f, intArrayOf(0x55FFE082, 0x00FFE082), null, Shader.TileMode.CLAMP,
        )
        ray.reset()
        for (i in 0 until 7) {
            val a = Math.toRadians(95.0 + i * 13.0)
            val b = a + Math.toRadians(4.0)
            ray.moveTo(cx, cy)
            ray.lineTo(cx + (Math.cos(a) * reach).toFloat(), cy + (Math.sin(a) * reach).toFloat())
            ray.lineTo(cx + (Math.cos(b) * reach).toFloat(), cy + (Math.sin(b) * reach).toFloat())
            ray.close()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!animated || width == 0) return
        val now = SystemClock.uptimeMillis()
        val t = (now - Motion.epoch) / 1000f
        val fade = ((now - fadeStart) / 1200f).coerceIn(0f, 1f)
        val s = scene

        if (s.celestial == Scene.Celestial.SUN) drawSun(canvas, t, fade)
        if (s.celestial == Scene.Celestial.MOON) drawStars(canvas, t, fade)
        if (s.clouds > 0) drawClouds(canvas, t, fade, s.clouds, s.darkClouds)
        if (s.fog) drawFog(canvas, t, fade)
        when (s.precipitation) {
            Scene.Precipitation.RAIN -> drawRain(canvas, t, fade, s.intensity)
            Scene.Precipitation.SNOW -> drawSnow(canvas, t, fade, s.intensity)
            Scene.Precipitation.NONE -> Unit
        }
        if (s.lightning) drawFlash(canvas, t)

        if (windowVisibility == VISIBLE) postInvalidateOnAnimation()
    }

    private fun alpha(base: Int, fade: Float) = (base * fade).toInt().coerceIn(0, 255)

    private fun drawSun(canvas: Canvas, t: Float, fade: Float) {
        glow.alpha = alpha((200 + 55 * sin(t * 0.8f)).toInt(), fade)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = alpha((22 + 10 * sin(t * 0.6f)).toInt(), fade)
        canvas.save()
        canvas.rotate(sin(t * 0.15f) * 9f, width * 0.88f, -height * 0.04f)
        canvas.drawPath(ray, paint)
        canvas.restore()
    }

    private fun drawStars(canvas: Canvas, t: Float, fade: Float) {
        paint.color = 0xFFFFFFFF.toInt()
        for (i in 0 until 45) {
            val twinkle = 0.5f + 0.5f * sin(t * (1f + speed[i]) + phase[i])
            paint.alpha = alpha((40 + 190 * twinkle).toInt(), fade)
            canvas.drawCircle(px[i] * width, py[i] * height * 0.75f, (0.8f + 1.4f * sizeF[i]) * dp, paint)
        }
        // A shooting star every 7 seconds.
        val c = t % 7f
        if (c < 0.9f) {
            val p = c / 0.9f
            val x0 = width * (0.15f + 0.5f * ((t / 7f).toInt() % 3) / 3f)
            val y0 = height * 0.08f
            val len = width * 0.35f
            val hx = x0 + len * p
            val hy = y0 + len * 0.4f * p
            stroke.color = 0xFFFFFFFF.toInt()
            stroke.strokeWidth = 2f * dp
            stroke.alpha = alpha((255 * (1 - p)).toInt(), fade)
            canvas.drawLine(hx - len * 0.18f, hy - len * 0.072f, hx, hy, stroke)
        }
    }

    private fun drawClouds(canvas: Canvas, t: Float, fade: Float, clouds: Int, dark: Boolean) {
        paint.color = if (dark) 0xFF1E2A33.toInt() else 0xFFFFFFFF.toInt()
        val n = if (clouds >= 2) 4 else 2
        for (i in 0 until n) {
            val w = width * (0.45f + 0.25f * sizeF[i])
            val travel = width + w
            val x = ((px[i] * travel + t * (6f + 5f * speed[i]) * dp) % travel) - w
            val y = height * (0.05f + 0.6f * py[i])
            paint.alpha = alpha(if (dark) 40 else 30 + 12 * i, fade)
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(w, w)
            canvas.drawPath(cloud, paint)
            canvas.restore()
        }
    }

    private fun drawFog(canvas: Canvas, t: Float, fade: Float) {
        stroke.color = 0xFFFFFFFF.toInt()
        for (i in 0 until 6) {
            stroke.strokeWidth = (18f + 14f * sizeF[i]) * dp
            stroke.alpha = alpha(28, fade)
            val y = height * (0.3f + 0.12f * i)
            val shift = sin(t * 0.25f * speed[i] + phase[i]) * width * 0.15f
            canvas.drawLine(-width * 0.1f + shift, y, width * 0.75f + shift, y, stroke)
        }
    }

    private fun drawRain(canvas: Canvas, t: Float, fade: Float, intensity: Float) {
        stroke.color = 0xFFCFE8FF.toInt()
        stroke.strokeWidth = 1.6f * dp
        val n = (25 + 35 * intensity).toInt().coerceAtMost(count)
        val len = (14f + 14f * intensity) * dp
        val h = height + len
        for (i in 0 until n) {
            val v = (0.9f + 0.6f * intensity) * speed[i] * height
            val y = (py[i] * h + t * v) % h - len
            val x = px[i] * (width + 40 * dp) - y * 0.12f
            stroke.alpha = alpha((70 + 70 * sizeF[i]).toInt(), fade)
            canvas.drawLine(x, y, x - len * 0.12f, y + len, stroke)
        }
    }

    private fun drawSnow(canvas: Canvas, t: Float, fade: Float, intensity: Float) {
        paint.color = 0xFFFFFFFF.toInt()
        val n = (25 + 35 * intensity).toInt().coerceAtMost(count)
        val h = height + 20 * dp
        for (i in 0 until n) {
            val v = (28f + 30f * speed[i]) * dp
            val y = (py[i] * h + t * v) % h - 10 * dp
            val x = px[i] * width + sin(t * 0.9f * speed[i] + phase[i]) * 18 * dp
            paint.alpha = alpha((110 + 130 * sizeF[i]).toInt(), fade)
            canvas.drawCircle(x, y, (1.6f + 2.6f * sizeF[i]) * dp, paint)
        }
    }

    /** Matches the lightning of [WeatherSceneView]: two quick flashes every 3.5 s. */
    private fun drawFlash(canvas: Canvas, t: Float) {
        val c = t % 3.5f
        val a = when {
            c < 0.12f -> 70
            c in 0.22f..0.30f -> 45
            else -> return
        }
        canvas.drawColor((a shl 24) or 0xFFFFFF)
    }
}
