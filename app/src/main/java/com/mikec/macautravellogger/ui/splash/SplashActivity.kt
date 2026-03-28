package com.mikec.macautravellogger.ui.splash

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.mikec.macautravellogger.MainActivity
import com.mikec.macautravellogger.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val dp = resources.displayMetrics.density

        // ── Root container ────────────────────────────────────────────────
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#00785A"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        // ── Icon ──────────────────────────────────────────────────────────
        val iconSize = (96 * dp).toInt()
        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            alpha = 0f  // hidden until animation starts
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        container.addView(iconView)

        // ── Title ─────────────────────────────────────────────────────────
        val titleView = TextView(this).apply {
            text = "Macau Travel Logger"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            alpha = 0f
            translationY = 20f * dp
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (20 * dp).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        container.addView(titleView)

        // ── Subtitle ──────────────────────────────────────────────────────
        val subtitleView = TextView(this).apply {
            text = "UM PRODUCTIONS"
            textSize = 12f
            setTextColor(Color.WHITE)
            alpha = 0f
            translationY = 20f * dp
            letterSpacing = 0.2f   // ~2sp equivalent
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * dp).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        container.addView(subtitleView)

        // ── Pulsing dots ──────────────────────────────────────────────────
        val dotsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (28 * dp).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        repeat(3) { i ->
            TextView(this).apply {
                text = "●"
                textSize = 16f
                setTextColor(Color.WHITE)
                alpha = 0.3f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (i > 0) marginStart = (12 * dp).toInt()
                }
                dotsLayout.addView(this)
            }
        }
        container.addView(dotsLayout)
        root.addView(container)
        setContentView(root)

        // ── Animations ────────────────────────────────────────────────────

        // Icon: scale 0.6→1.0 + fade in (600ms, overshoot) — uses splash_icon_anim.xml
        iconView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_icon_anim))

        // Title: slide up + fade in (300ms delay, 500ms duration, decelerate)
        // Uses splash_text_anim parameters via ViewPropertyAnimator for precise alpha target
        iconView.postDelayed({
            titleView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 300)

        // Subtitle: same slide animation, staggered 200ms after title, fades to 60% opacity
        iconView.postDelayed({
            subtitleView.animate()
                .alpha(0.6f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, 500)

        // Pulsing dots: alpha 0.3→1.0→0.3, looping, staggered 200ms between each dot
        for (i in 0 until dotsLayout.childCount) {
            ObjectAnimator.ofFloat(dotsLayout.getChildAt(i), "alpha", 0.3f, 1.0f, 0.3f).apply {
                duration = 800
                startDelay = (i * 200).toLong()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
            }.start()
        }

        // Navigate to MainActivity after 2000ms total
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 2000)
    }
}
