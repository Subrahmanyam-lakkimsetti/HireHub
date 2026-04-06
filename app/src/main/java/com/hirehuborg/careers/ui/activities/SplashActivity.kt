package com.hirehuborg.careers.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.hirehuborg.careers.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var rootLayout: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen first
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        initViews()
        createModernLogo()
        startModernAnimations()

        lifecycleScope.launch {
            delay(1800L)
            navigateToNextScreen()
        }
    }

    private fun initViews() {
        ivLogo = findViewById(R.id.ivLogo)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        rootLayout = findViewById(R.id.rootLayout)

        // Set initial states for animations
        ivLogo.alpha = 0f
        ivLogo.scaleX = 0.6f
        ivLogo.scaleY = 0.6f
        tvAppName.alpha = 0f
        tvAppName.translationY = 80f
        tvTagline.alpha = 0f
        tvTagline.translationY = 30f
    }

    private fun createModernLogo() {
        // Create a beautiful, modern "H" logo with matching color scheme
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Center coordinates
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2.2f

        // 1. Main background with radial gradient (matching your splash_bg color)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radialGradient = RadialGradient(
            centerX, centerY, radius,
            Color.parseColor("#FF6B6B"),  // Light red
            Color.parseColor("#FF4757"),  // Your primary red color
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = radialGradient
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        // 2. Inner ring with gradient (white to light red)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = 10f
        val ringGradient = SweepGradient(
            centerX, centerY,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#FFE6E6"),
                Color.parseColor("#FFB6B6"),
                Color.parseColor("#FFFFFF")
            ),
            null
        )
        ringPaint.shader = ringGradient
        canvas.drawCircle(centerX, centerY, radius - 20, ringPaint)

        // 3. Draw modern "H" letter with matching white color
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE  // Pure white for better contrast
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 210f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // Add subtle shadow to text
        textPaint.setShadowLayer(6f, 3f, 3f, Color.parseColor("#40FF4757"))

        // Draw the "H" with better positioning
        val textBounds = Rect()
        textPaint.getTextBounds("H", 0, 1, textBounds)
        val yOffset = (textBounds.height() / 2f)
        canvas.drawText("H", centerX, centerY + yOffset - 12, textPaint)

        // 4. Add decorative dots around the logo (matching theme)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = Color.parseColor("#B3FFFFFF")  // Semi-transparent white
        dotPaint.style = Paint.Style.FILL

        for (i in 0..11) {
            val angle = (i * 30).toDouble()
            val dotX = centerX + (radius - 12) * Math.cos(Math.toRadians(angle)).toFloat()
            val dotY = centerY + (radius - 12) * Math.sin(Math.toRadians(angle)).toFloat()
            canvas.drawCircle(dotX, dotY, 4f, dotPaint)
        }

        // 5. Add inner shine effect
        val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shineGradient = RadialGradient(
            centerX - 25, centerY - 25, 50f,
            Color.parseColor("#60FFFFFF"),
            Color.parseColor("#00FFFFFF"),
            Shader.TileMode.CLAMP
        )
        shinePaint.shader = shineGradient
        canvas.drawCircle(centerX - 20, centerY - 20, 60f, shinePaint)

        // Set the bitmap to ImageView
        ivLogo.setImageBitmap(bitmap)
    }

    private fun startModernAnimations() {
        // Modern logo animation: Scale + Rotation + Fade with spring effect
        val logoScaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.6f, 1.05f, 1.0f)
        val logoScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.6f, 1.05f, 1.0f)
        val logoFade = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f)
        val logoRotate = ObjectAnimator.ofFloat(ivLogo, "rotation", -8f, 4f, 0f)

        val logoAnimator = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoFade, logoRotate)
            duration = 500
            interpolator = AnticipateOvershootInterpolator(1.3f)
        }

        // App name animation: Slide up with elastic effect
        val nameSlide = ObjectAnimator.ofFloat(tvAppName, "translationY", 80f, -3f, 0f)
        val nameFade = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f)

        val nameAnimator = AnimatorSet().apply {
            playTogether(nameSlide, nameFade)
            duration = 450
            startDelay = 200
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Tagline animation: Gentle fade and slide
        val taglineSlide = ObjectAnimator.ofFloat(tvTagline, "translationY", 30f, 0f)
        val taglineFade = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f)

        val taglineAnimator = AnimatorSet().apply {
            playTogether(taglineSlide, taglineFade)
            duration = 400
            startDelay = 400
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Start animations
        logoAnimator.start()
        nameAnimator.start()
        taglineAnimator.start()

        // Add subtle pulse effect after logo animation
        logoAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val pulseScaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 1.0f, 1.05f, 1.0f)
                val pulseScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 1.0f, 1.05f, 1.0f)

                AnimatorSet().apply {
                    playTogether(pulseScaleX, pulseScaleY)
                    duration = 300
                    startDelay = 200
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        })
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun navigateToNextScreen() {
        // Crossfade animation to prevent white screen
        val intent = Intent(this, LoginActivity::class.java)

        // Start LoginActivity with fade in animation while SplashActivity fades out
        startActivity(intent)

        // Apply smooth crossfade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Finish current activity after transition starts
        finish()

        // Override default window animation to prevent white flash
        window.setWindowAnimations(0)
    }
}