package com.hirehuborg.careers.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.databinding.ItemJobCardBinding

class JobAdapter(
    private val onJobClick:  (JobMatch) -> Unit,
    private val onSaveClick: (JobMatch) -> Unit
) : ListAdapter<JobMatch, JobAdapter.JobViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    inner class JobViewHolder(
        private val binding: ItemJobCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: JobMatch) {
            val job = match.job

            // ── Basic fields ──────────────────────────────────────────────────
            binding.tvJobTitle.text  = job.title
            binding.tvCompany.text   = job.company
            binding.tvLocation.text  = job.location.ifBlank { "Remote" }

            binding.tvSalary.text       = job.salary
            binding.tvSalary.visibility = if (job.salary.isBlank()) View.GONE else View.VISIBLE

            // ── Source badge ──────────────────────────────────────────────────
            binding.tvSource.text = job.source.replaceFirstChar { it.uppercase() }
            val sourceColor = when (job.source) {
                "remotive"  -> "#1A73E8"
                "arbeitnow" -> "#00C853"
                else        -> "#6B7280"
            }
            binding.tvSource.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(sourceColor))

            // ── Match score badge ─────────────────────────────────────────────
            if (match.matchScore > 0) {
                binding.tvMatchScore.visibility = View.VISIBLE
                binding.tvMatchScore.text       = "${match.matchScore}% Match"
                binding.tvMatchScore.backgroundTintList =
                    ColorStateList.valueOf(matchScoreColor(match.matchScore))
            } else {
                binding.tvMatchScore.visibility = View.GONE
            }

            // ── Matched skill pills ───────────────────────────────────────────
            binding.layoutMatchedSkills.removeAllViews()
            val displaySkills = match.matchedSkills.take(4)

            if (displaySkills.isNotEmpty()) {
                binding.scrollMatchedSkills.visibility = View.VISIBLE
                displaySkills.forEach { skill ->
                    val pill = buildSkillPill(skill, binding.root.context)
                    binding.layoutMatchedSkills.addView(pill)
                }
            } else {
                binding.scrollMatchedSkills.visibility = View.GONE
            }

            // ── Save icon ─────────────────────────────────────────────────────
            binding.ivSave.setImageResource(
                if (job.isSaved) R.drawable.ic_bookmark_filled
                else             R.drawable.ic_bookmark_outline
            )

            // ── Clicks ────────────────────────────────────────────────────────
            binding.root.setOnClickListener    { onJobClick(match) }
            binding.ivSave.setOnClickListener  { onSaveClick(match) }

            itemView.alpha = 0f
            itemView.animate()
                .alpha(1f)
                .translationYBy(-10f)
                .setDuration(300)
                .setStartDelay((bindingAdapterPosition % 10 * 40).toLong())
                .start()
        }

        private fun buildSkillPill(skill: String, context: Context): TextView {
            return TextView(context).apply {
                text = skill
                textSize = 10f
                setTextColor(Color.parseColor("#1A73E8"))
                setPadding(16, 4, 16, 4)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E3F2FD"))
                background = context.getDrawable(R.drawable.bg_chip_rounded)
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                layoutParams = params
            }
        }

        /**
         * Color of match score badge:
         *  ≥ 70 → green   (strong match)
         *  ≥ 40 → orange  (decent match)
         *  < 40 → blue    (partial match)
         */
        private fun matchScoreColor(score: Int): Int = when {
            score >= 70 -> Color.parseColor("#00C853")
            score >= 40 -> Color.parseColor("#FF9800")
            else        -> Color.parseColor("#1A73E8")
        }
    }

    // ── DiffCallback ──────────────────────────────────────────────────────────
    class DiffCallback : DiffUtil.ItemCallback<JobMatch>() {
        override fun areItemsTheSame(old: JobMatch, new: JobMatch) =
            old.job.id == new.job.id
        override fun areContentsTheSame(old: JobMatch, new: JobMatch) =
            old == new
    }
}