package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.ResumeAnalysis
import com.hirehuborg.careers.databinding.ActivityResumeAnalysisBinding
import com.hirehuborg.careers.ui.viewmodels.AnalysisState
import com.hirehuborg.careers.ui.viewmodels.AnalysisViewModel
import com.hirehuborg.careers.ui.views.ScoreBreakdownBar
import com.hirehuborg.careers.utils.Constants

class ResumeAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumeAnalysisBinding
    private val viewModel: AnalysisViewModel by viewModels()

    private var resumeText: String = ""
    private var detectedSkills: List<String> = emptyList()

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumeAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        extractIntentData()
        setupClicks()
        observeViewModel()

        viewModel.analyze(resumeText, detectedSkills)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun extractIntentData() {
        resumeText     = intent.getStringExtra(Constants.EXTRA_RESUME_TEXT) ?: ""
        detectedSkills = intent.getStringArrayListExtra(Constants.EXTRA_DETECTED_SKILLS)
            ?: arrayListOf()
    }

    private fun setupClicks() {
        binding.btnRetry.setOnClickListener {
            viewModel.analyze(resumeText, detectedSkills)
        }
        binding.btnBrowseJobs.setOnClickListener {
            startActivity(Intent(this, JobListActivity::class.java))
        }
    }

    // ── Observer ──────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is AnalysisState.Idle    -> { showLoading(false); hideError() }
                is AnalysisState.Loading -> {
                    showLoading(true)
                    hideError()
                    binding.scrollResults.visibility = View.GONE
                }
                is AnalysisState.Success -> {
                    showLoading(false)
                    hideError()
                    renderResults(state.analysis)
                }
                is AnalysisState.Error   -> {
                    showLoading(false)
                    showError(state.message)
                    binding.scrollResults.visibility = View.GONE
                }
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    private fun renderResults(analysis: ResumeAnalysis) {
        binding.scrollResults.visibility = View.VISIBLE

        // ── Animate cards sliding in from bottom ─────────────────────────────
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        binding.scrollResults.startAnimation(slideUp)

        // ── 1. Score circle (Canvas animation) ───────────────────────────────
        binding.scoreCircleView.setScore(analysis.atsScore, animate = true)
        binding.tvScoreLabel.text      = scoreLabel(analysis.atsScore)
        binding.tvScoreLabel.setTextColor(scoreColor(analysis.atsScore))

        // ── 2. Score breakdown bars (Canvas animation, staggered) ─────────────
        renderBreakdownBars(analysis)

        // ── 3. Strengths ──────────────────────────────────────────────────────
        binding.layoutStrengths.removeAllViews()
        analysis.strengths.forEachIndexed { index, point ->
            val item = buildBulletItem(point, R.color.success)
            item.alpha = 0f
            item.animate().alpha(1f).setStartDelay(index * 80L).setDuration(300).start()
            binding.layoutStrengths.addView(item)
        }

        // ── 4. Improvements ───────────────────────────────────────────────────
        binding.layoutImprovements.removeAllViews()
        analysis.improvements.forEachIndexed { index, point ->
            val item = buildBulletItem(point, R.color.warning)
            item.alpha = 0f
            item.animate().alpha(1f).setStartDelay(index * 80L).setDuration(300).start()
            binding.layoutImprovements.addView(item)
        }

        // ── 5. Missing skills chips ────────────────────────────────────────────
        binding.chipGroupMissingSkills.removeAllViews()
        analysis.missingSkills.forEach { skill ->
            binding.chipGroupMissingSkills.addView(
                buildChip(skill, "#FFEBEE", "#C62828")
            )
        }

        // ── 6. Recommended roles chips ────────────────────────────────────────
        binding.chipGroupRoles.removeAllViews()
        analysis.recommendedRoles.forEach { role ->
            binding.chipGroupRoles.addView(
                buildChip(role, "#E3F2FD", "#0D47A1")
            )
        }
    }

    /**
     * Builds and animates score breakdown bars.
     * Derives sub-scores from ATS score with slight variation per category.
     * In a production app these would come from Gemini directly.
     */
    private fun renderBreakdownBars(analysis: ResumeAnalysis) {
        binding.layoutBreakdownBars.removeAllViews()

        val base = analysis.atsScore

        // Derived sub-scores — realistic variation per dimension
        val breakdowns = listOf(
            Triple("Technical Skills",    (base + 8).coerceIn(0, 100),  "#1A73E8"),
            Triple("Work Experience",     (base - 5).coerceIn(0, 100),  "#00C853"),
            Triple("Resume Structure",    (base + 3).coerceIn(0, 100),  "#FF9800"),
            Triple("Keyword Relevance",   (base - 10).coerceIn(0, 100), "#9C27B0"),
            Triple("Achievements",        (base - 15).coerceIn(0, 100), "#F44336")
        )

        breakdowns.forEachIndexed { index, (label, score, color) ->
            val bar = ScoreBreakdownBar(this).apply {
                setValues(label, score, color)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 4.dpToPx()
                }
            }
            binding.layoutBreakdownBars.addView(bar)
            // Stagger animation so bars fill one after another
            bar.startAnimation(startDelay = index * 150L)
        }
    }

    // ── View builders ─────────────────────────────────────────────────────────
    private fun buildBulletItem(text: String, dotColorRes: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dpToPx() }
        }

        val dot = TextView(this).apply {
            this.text = "•"
            textSize  = 18f
            setTextColor(ContextCompat.getColor(this@ResumeAnalysisActivity, dotColorRes))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 10.dpToPx() }
        }

        val content = TextView(this).apply {
            this.text = text
            textSize  = 13f
            setTextColor(ContextCompat.getColor(this@ResumeAnalysisActivity, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setLineSpacing(3f, 1f)
        }

        row.addView(dot)
        row.addView(content)
        return row
    }

    private fun buildChip(label: String, bgHex: String, textHex: String): Chip {
        return Chip(this).apply {
            text = label
            isClickable = false
            isCheckable = false
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                Color.parseColor(bgHex)
            )
            setTextColor(Color.parseColor(textHex))
            textSize = 12f
        }
    }

    // ── Score helpers ──────────────────────────────────────────────────────────
    private fun scoreLabel(score: Int): String = when {
        score >= 80 -> "Excellent 🎉"
        score >= 60 -> "Good 👍"
        score >= 40 -> "Needs Work 📝"
        else        -> "Poor — needs major revision ⚠️"
    }

    private fun scoreColor(score: Int): Int = when {
        score >= 75 -> Color.parseColor("#00C853")
        score >= 50 -> Color.parseColor("#FF9800")
        else        -> Color.parseColor("#F44336")
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text           = message
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}