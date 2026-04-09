package com.hirehuborg.careers.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.RecyclerView
import com.hirehuborg.careers.R

object HireHubAnimUtils {

    // ── Activity Transitions ──────────────────────────────────────────────────

    /**
     * Call AFTER startActivity() to slide new screen in from right.
     * Use for forward navigation.
     */
    fun Activity.slideInFromRight() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Call AFTER finish() to slide back to previous screen.
     * Use for back navigation.
     */
    fun Activity.slideOutToRight() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Fade transition — use for modal-style screens (analysis, detail).
     */
    fun Activity.fadeTransitionIn() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // ── View Animations ───────────────────────────────────────────────────────

    /**
     * Fades in a view with optional delay.
     */
    fun View.fadeIn(durationMs: Long = 350, delayMs: Long = 0) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setStartDelay(delayMs)
            .start()
    }

    /**
     * Fades out and hides a view.
     */
    fun View.fadeOut(durationMs: Long = 250) {
        animate()
            .alpha(0f)
            .setDuration(durationMs)
            .withEndAction { visibility = View.GONE }
            .start()
    }

    /**
     * Slides a view up from below with fade.
     * Great for cards appearing on screen load.
     */
    fun View.slideUpFadeIn(durationMs: Long = 400, delayMs: Long = 0) {
        alpha = 0f
        translationY = 60f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(durationMs)
            .setStartDelay(delayMs)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
            .start()
    }

    /**
     * Bounces a view with a quick scale pulse.
     * Use on save/bookmark actions.
     */
    fun View.pulse() {
        animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(120)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    /**
     * Shake animation — use on validation errors.
     */
    fun View.shake() {
        val anim = AnimationUtils.loadAnimation(context, android.R.anim.cycle_interpolator)
        val shake = android.view.animation.TranslateAnimation(0f, 15f, 0f, 0f).apply {
            duration = 400
            repeatCount = 4
            repeatMode = android.view.animation.Animation.REVERSE
        }
        startAnimation(shake)
    }

    // ── RecyclerView Stagger ──────────────────────────────────────────────────

    /**
     * Applies a staggered fall-down animation to all RecyclerView items.
     * Call after submitList() for smooth list appearance.
     */
    fun RecyclerView.runLayoutAnimation() {
        val controller = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )
        layoutAnimation = controller
        scheduleLayoutAnimation()
    }

    // ── Stagger helper for multiple views ────────────────────────────────────

    /**
     * Animates a list of views sliding up one after another.
     * Perfect for home screen cards.
     */
    fun staggerSlideUp(views: List<View>, baseDelayMs: Long = 80L) {
        views.forEachIndexed { index, view ->
            view.slideUpFadeIn(delayMs = index * baseDelayMs)
        }
    }
}