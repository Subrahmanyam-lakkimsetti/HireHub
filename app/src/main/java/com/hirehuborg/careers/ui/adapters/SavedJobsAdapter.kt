package com.hirehuborg.careers.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.databinding.ItemSavedJobCardBinding

class SavedJobsAdapter(
    private val onJobClick:   (Job) -> Unit,
    private val onRemoveClick: (Job) -> Unit
) : ListAdapter<Job, SavedJobsAdapter.SavedJobViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedJobViewHolder {
        val binding = ItemSavedJobCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SavedJobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedJobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedJobViewHolder(
        private val binding: ItemSavedJobCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(job: Job) {
            binding.tvJobTitle.text  = job.title
            binding.tvCompany.text   = job.company
            binding.tvLocation.text  = job.location.ifBlank { "Remote" }
            binding.tvSource.text    = job.source.replaceFirstChar { it.uppercase() }

            // Salary
            binding.tvSalary.text       = job.salary
            binding.tvSalary.visibility = if (job.salary.isBlank()) View.GONE else View.VISIBLE

            // Source badge color
            val sourceColor = when (job.source) {
                "remotive"  -> "#1A73E8"
                "arbeitnow" -> "#00C853"
                else        -> "#6B7280"
            }
            binding.tvSource.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(sourceColor))

            // Clicks
            binding.root.setOnClickListener      { onJobClick(job) }
            binding.ivRemove.setOnClickListener  { onRemoveClick(job) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(old: Job, new: Job)     = old.id == new.id
        override fun areContentsTheSame(old: Job, new: Job)  = old == new
    }
}