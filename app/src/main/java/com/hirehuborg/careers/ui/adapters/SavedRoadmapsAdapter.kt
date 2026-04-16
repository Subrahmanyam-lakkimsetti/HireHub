package com.hirehuborg.careers.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.Roadmap
import java.text.SimpleDateFormat
import java.util.*

class SavedRoadmapsAdapter(
    private val onItemClick: (Roadmap) -> Unit,
    private val onDeleteClick: (Roadmap) -> Unit
) : ListAdapter<Roadmap, SavedRoadmapsAdapter.RoadmapViewHolder>(RoadmapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoadmapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_roadmap, parent, false)
        return RoadmapViewHolder(view, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: RoadmapViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoadmapViewHolder(
        itemView: android.view.View,
        private val onItemClick: (Roadmap) -> Unit,
        private val onDeleteClick: (Roadmap) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvSkillName: TextView = itemView.findViewById(R.id.tvSkillName)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvStepsCount: TextView = itemView.findViewById(R.id.tvStepsCount)
        private val tvGeneratedAt: TextView = itemView.findViewById(R.id.tvGeneratedAt)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(roadmap: Roadmap) {
            tvSkillName.text = roadmap.skillName
            tvDuration.text = roadmap.estimatedDuration
            tvStepsCount.text = "${roadmap.steps.size} steps"

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvGeneratedAt.text = dateFormat.format(Date(roadmap.generatedAt))

            itemView.setOnClickListener {
                onItemClick(roadmap)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(roadmap)
            }
        }
    }

    class RoadmapDiffCallback : DiffUtil.ItemCallback<Roadmap>() {
        override fun areItemsTheSame(oldItem: Roadmap, newItem: Roadmap): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Roadmap, newItem: Roadmap): Boolean {
            return oldItem == newItem
        }
    }
}