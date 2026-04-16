package com.hirehuborg.careers.ui.views


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Custom Canvas view that draws an animated circular ATS score meter.
 *
 * Features:
 *  - Background track arc (light grey)
 *  - Animated foreground arc filling from 0 → score
 *  - Dynamic color: red (0-49) → orange (50-74) → green (75-100)
 *  - Score number drawn in center with Canvas drawText
 *  - Label text beneath score number
 *  - Smooth decelerate animation (600ms default)
 */
class ScoreCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Configurable properties ───────────────────────────────────────────────
    var targetScore: Int = 0
        private set

    var animationDuration: Long = 1200L

    // ── Internal state ────────────────────────────────────────────────────────
    private var animatedScore: Float = 0f   // current animated value (0–100)
    private var animator: ValueAnimator? = null

    // ── Paint objects (created once, reused every draw call) ──────────────────

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style  = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color  = Color.parseColor("#E8ECEF")
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#6B7280")
    }

    // Arc bounding rectangle — calculated in onSizeChanged
    private val arcRect = RectF()

    // ── Size ──────────────────────────────────────────────────────────────────
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeWidth = w * 0.09f   // 9% of width as stroke thickness
        val inset = strokeWidth / 2f + w * 0.04f

        trackPaint.strokeWidth = strokeWidth
        arcPaint.strokeWidth   = strokeWidth

        arcRect.set(inset, inset, w - inset, h - inset)

        scorePaint.textSize = w * 0.28f   // score number ~28% of view width
        labelPaint.textSize = w * 0.09f   // label ~9%
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f

        // ── Background track ─────────────────────────────────────────────────
        // 270° arc (starts at 135°, sweeps 270°) → leaves a gap at the bottom
        canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

        // ── Score arc ─────────────────────────────────────────────────────────
        val sweepAngle = (animatedScore / 100f) * 270f
        arcPaint.color = scoreColor(animatedScore)
        canvas.drawArc(arcRect, 135f, sweepAngle, false, arcPaint)

        // ── Score number ──────────────────────────────────────────────────────
        val displayScore = animatedScore.toInt().toString()
        scorePaint.color = scoreColor(animatedScore)
        // Vertically center the score text
        val textBounds = android.graphics.Rect()
        scorePaint.getTextBounds(displayScore, 0, displayScore.length, textBounds)
        canvas.drawText(displayScore, cx, cy + textBounds.height() / 2f, scorePaint)

        // ── "/ 100" label below score ─────────────────────────────────────────
        canvas.drawText("/ 100", cx, cy + textBounds.height() / 2f + labelPaint.textSize * 1.4f, labelPaint)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sets the score and starts the fill animation.
     * Safe to call multiple times — cancels previous animation first.
     */
    fun setScore(score: Int, animate: Boolean = true) {
        targetScore = score.coerceIn(0, 100)

        animator?.cancel()

        if (!animate) {
            animatedScore = targetScore.toFloat()
            invalidate()
            return
        }

        animator = ValueAnimator.ofFloat(0f, targetScore.toFloat()).apply {
            duration     = animationDuration
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { anim ->
                animatedScore = anim.animatedValue as Float
                invalidate()   // triggers onDraw every frame
            }
            start()
        }
    }

    // ── Color logic ───────────────────────────────────────────────────────────

    /**
     * Interpolates arc color based on current animated score:
     *   0–49  → red   (#F44336)
     *   50–74 → orange (#FF9800)
     *   75–100 → green (#00C853)
     *
     * Uses ArgbEvaluator for smooth color transition during animation.
     */
    private fun scoreColor(score: Float): Int {
        return when {
            score < 50f -> {
                val fraction = score / 50f
                interpolateColor(Color.parseColor("#F44336"), Color.parseColor("#FF9800"), fraction)
            }
            score < 75f -> {
                val fraction = (score - 50f) / 25f
                interpolateColor(Color.parseColor("#FF9800"), Color.parseColor("#00C853"), fraction)
            }
            else -> Color.parseColor("#00C853")
        }
    }

    private fun interpolateColor(start: Int, end: Int, fraction: Float): Int {
        val startA = Color.alpha(start);  val endA = Color.alpha(end)
        val startR = Color.red(start);    val endR = Color.red(end)
        val startG = Color.green(start);  val endG = Color.green(end)
        val startB = Color.blue(start);   val endB = Color.blue(end)
        return Color.argb(
            (startA + (endA - startA) * fraction).toInt(),
            (startR + (endR - startR) * fraction).toInt(),
            (startG + (endG - startG) * fraction).toInt(),
            (startB + (endB - startB) * fraction).toInt()
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}