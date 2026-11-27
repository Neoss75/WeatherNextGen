package com.neos.weatherservice

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.neos.weatherservice.ui.Motion
import com.neos.weatherservice.ui.WeatherBackdropView
import com.neos.weatherservice.ui.WeatherStyle

/** The guide: what the service does and how to set it up, in glass cards over the current weather. */
class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        val motion = Motion.enabled(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.action_help) + " · " + BuildConfig.VERSION_NAME
        toolbar.setNavigationOnClickListener { finish() }

        val w = WeatherService.status.value.weather
        val palette = w?.let { WeatherStyle.palette(it.code.condition, it.isDay) } ?: WeatherStyle.idlePalette
        window.setBackgroundDrawable(
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(palette.top, palette.bottom)),
        )
        @Suppress("DEPRECATION") // system bar colours still apply: the app opts out of edge-to-edge
        window.apply {
            statusBarColor = palette.top
            navigationBarColor = palette.bottom
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        findViewById<WeatherBackdropView>(R.id.backdrop).apply {
            scene = w?.let { WeatherStyle.scene(it.code.condition, it.isDay) } ?: WeatherStyle.idleScene
            animated = motion
        }

        val container = findViewById<LinearLayout>(R.id.sections)
        val inflater = LayoutInflater.from(this)
        val views = SECTIONS.map { (icon, title, body) ->
            inflater.inflate(R.layout.item_help, container, false).apply {
                findViewById<TextView>(R.id.help_title).apply {
                    setText(title)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
                }
                findViewById<TextView>(R.id.help_body).text =
                    HtmlCompat.fromHtml(getString(body), HtmlCompat.FROM_HTML_MODE_LEGACY).trim()
                container.addView(this)
            }
        }
        if (motion && savedInstanceState == null) enter(views)
    }

    private fun enter(views: List<View>) {
        val shift = 40 * resources.displayMetrics.density
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = shift
            Motion.animate(v, 650, Motion.enter, delayMs = 80L * i, alpha = 1f, translationY = 0f).start()
        }
    }

    private companion object {
        val SECTIONS = listOf(
            Triple(R.drawable.ic_place, R.string.help_1_title, R.string.help_1_body),
            Triple(R.drawable.ic_settings, R.string.help_2_title, R.string.help_2_body),
            Triple(R.drawable.ic_folder, R.string.help_3_title, R.string.help_3_body),
            Triple(R.drawable.ic_car, R.string.help_4_title, R.string.help_4_body),
            Triple(R.drawable.ic_refresh, R.string.help_5_title, R.string.help_5_body),
            Triple(R.drawable.ic_file, R.string.help_6_title, R.string.help_6_body),
            Triple(R.drawable.ic_warning, R.string.help_7_title, R.string.help_7_body),
        )
    }
}
