package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.SkillCategory
import com.hirehuborg.careers.databinding.ActivitySkillDetectionBinding
import com.hirehuborg.careers.ui.viewmodels.SkillDetectionState
import com.hirehuborg.careers.ui.viewmodels.SkillViewModel
import com.hirehuborg.careers.utils.Constants

class SkillDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillDetectionBinding
    private val viewModel: SkillViewModel by viewModels()

    private var resumeText: String = ""
    private var detectedFlatSkills: List<String> = emptyList()

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClicks()
        observeViewModel()
        startDetection()
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClicks() {
        binding.btnRetry.setOnClickListener {
            startDetection()
        }

        binding.btnAnalyze.setOnClickListener {
            val intent = Intent(this, ResumeAnalysisActivity::class.java).apply {
                putExtra(Constants.EXTRA_RESUME_TEXT, resumeText)
                putStringArrayListExtra(
                    Constants.EXTRA_DETECTED_SKILLS,
                    ArrayList(detectedFlatSkills)
                )
            }
            startActivity(intent)
        }
    }

    private fun startDetection() {
        resumeText = intent.getStringExtra(Constants.EXTRA_RESUME_TEXT) ?: ""
        if (resumeText.isBlank()) {
            showError("No resume text found. Please re-upload your resume.")
            return
        }
        viewModel.detectSkills(resumeText)
    }

    // ── Observer ─────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is SkillDetectionState.Idle    -> showLoading(false)

                is SkillDetectionState.Loading -> {
                    showLoading(true)
                    hideError()
                    hideContent()
                }

                is SkillDetectionState.Success -> {
                    showLoading(false)
                    hideError()
                    detectedFlatSkills = state.flatSkills
                    renderSkills(state.categories, state.flatSkills.size)
                }

                is SkillDetectionState.Error -> {
                    showLoading(false)
                    hideContent()
                    showError(state.message)
                }
            }
        }
    }

    // ── Render skills ─────────────────────────────────────────────────────────
    private fun renderSkills(categories: List<SkillCategory>, totalCount: Int) {
        binding.scrollContent.visibility = View.VISIBLE
        binding.tvSkillCount.text = "$totalCount skills found"

        // Clear old views before re-rendering (e.g. on retry)
        binding.layoutCategories.removeAllViews()

        for (category in categories) {
            val categoryView = layoutInflater.inflate(
                R.layout.item_skill_category,
                binding.layoutCategories,
                false
            )

            val tvTitle = categoryView.findViewById<android.widget.TextView>(R.id.tvCategoryTitle)
            val chipGroup = categoryView.findViewById<ChipGroup>(R.id.chipGroup)

            tvTitle.text = "${category.emoji}  ${category.categoryName}"

            for (skill in category.skills) {
                val chip = buildChip(skill, category.chipColorRes)
                chipGroup.addView(chip)
            }

            // Wrap in a CardView programmatically for visual separation
            val card = androidx.cardview.widget.CardView(this).apply {
                radius = 48f
                cardElevation = 8f
                setContentPadding(40, 32, 40, 32)
                useCompatPadding = true
                setCardBackgroundColor(
                    ContextCompat.getColor(this@SkillDetectionActivity, android.R.color.white)
                )
            }
            card.addView(categoryView)

            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            binding.layoutCategories.addView(card, params)
        }
    }

    private fun buildChip(label: String, backgroundColorRes: Int): Chip {
        return Chip(this).apply {
            text = label
            isClickable = false
            isCheckable = false
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@SkillDetectionActivity, backgroundColorRes)
            )
            setTextColor(ContextCompat.getColor(this@SkillDetectionActivity, R.color.text_primary))
            textSize = 12f
            chipMinHeight = 32f.dpToPxFloat()
            setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
        }
    }

    // ── UI state helpers ──────────────────────────────────────────────────────
    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun hideContent() {
        binding.scrollContent.visibility = View.GONE
    }

    // ── Extensions ────────────────────────────────────────────────────────────
    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    private fun Float.dpToPxFloat(): Float =
        this * resources.displayMetrics.density
}