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
 * Custom Canvas view for a single animated horizontal progress bar.
 *
 * Usage (programmatic):
 *   val bar = ScoreBreakdownBar(context)
 *   bar.setValues(label = "Technical Skills", score = 78, color = "#1A73E8")
 *   bar.startAnimation(startDelay = 200)
 *   container.addView(bar)
 */
class ScoreBreakdownBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Config ────────────────────────────────────────────────────────────────
    private var label: String  = ""
    private var targetScore: Int = 0
    private var barColor: Int  = Color.parseColor("#1A73E8")
    private var animatedWidth: Float = 0f
    private var animator: ValueAnimator? = null

    // ── Paints ────────────────────────────────────────────────────────────────
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F0F0")
        style = Paint.Style.FILL
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#374151")
        textAlign = Paint.Align.LEFT
        isFakeBoldText = false
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }

    private val trackRect = RectF()
    private val barRect   = RectF()

    // ── Measure ───────────────────────────────────────────────────────────────
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (56 * resources.displayMetrics.density).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        labelPaint.textSize = h * 0.27f
        valuePaint.textSize = h * 0.27f
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val textRowH    = h * 0.45f     // top half for labels
        val barTop      = textRowH + h * 0.06f
        val barBottom   = h - h * 0.08f
        val barRadius   = (barBottom - barTop) / 2f

        // ── Labels ────────────────────────────────────────────────────────────
        valuePaint.color = barColor
        canvas.drawText(label, 0f, textRowH, labelPaint)
        canvas.drawText("$targetScore%", w, textRowH, valuePaint)

        // ── Track ─────────────────────────────────────────────────────────────
        trackRect.set(0f, barTop, w, barBottom)
        canvas.drawRoundRect(trackRect, barRadius, barRadius, trackPaint)

        // ── Animated fill bar ─────────────────────────────────────────────────
        if (animatedWidth > 0f) {
            barPaint.color = barColor
            barRect.set(0f, barTop, animatedWidth, barBottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun setValues(label: String, score: Int, colorHex: String) {
        this.label       = label
        this.targetScore = score.coerceIn(0, 100)
        this.barColor    = Color.parseColor(colorHex)
        invalidate()
    }

    fun startAnimation(startDelay: Long = 0L) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration          = 900L
            this.startDelay   = startDelay
            interpolator      = DecelerateInterpolator(1.5f)
            addUpdateListener { anim ->
                val fraction  = anim.animatedValue as Float
                animatedWidth = (width.toFloat()) * (targetScore / 100f) * fraction
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}