package com.hirehuborg.careers.utils

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.recyclerview.widget.RecyclerView
import com.hirehuborg.careers.R

object HireHubAnimUtils {

    fun View.fadeIn(durationMs: Long = 350, delayMs: Long = 0) {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setStartDelay(delayMs)
            .start()
    }

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
     * Uses ObjectAnimator — no XML interpolator needed.
     */
    fun View.shake() {
        ObjectAnimator.ofFloat(this, "translationX", 0f, -18f, 18f, -14f, 14f, -10f, 10f, -6f, 6f, 0f).apply {
            duration = 450
            start()
        }
    }

    fun RecyclerView.runLayoutAnimation() {
        val controller = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )
        layoutAnimation = controller
        scheduleLayoutAnimation()
    }

    fun staggerSlideUp(views: List<View>, baseDelayMs: Long = 80L) {
        views.forEachIndexed { index, view ->
            view.slideUpFadeIn(delayMs = index * baseDelayMs)
        }
    }
}