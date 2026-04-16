package com.hirehuborg.careers.ui.activities

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.Roadmap
import com.hirehuborg.careers.data.model.RoadmapStep
import com.hirehuborg.careers.data.repository.RoadmapRepository
import com.hirehuborg.careers.databinding.ActivityRoadmapDetailBinding
import kotlinx.coroutines.launch

class RoadmapDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoadmapDetailBinding
    private val roadmapRepository = RoadmapRepository()
    private var currentRoadmap: Roadmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoadmapDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadRoadmap()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetail.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadRoadmap() {
        val roadmapId = intent.getStringExtra("roadmap_id")
        val skillName = intent.getStringExtra("skill_name")

        if (roadmapId != null) {
            lifecycleScope.launch {
                val roadmaps = roadmapRepository.getSavedRoadmaps()
                currentRoadmap = roadmaps.find { it.id == roadmapId }
                currentRoadmap?.let { displayRoadmap(it) }
            }
        } else if (skillName != null) {
            supportActionBar?.title = "Roadmap: $skillName"
            Toast.makeText(this, "Loading roadmap...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayRoadmap(roadmap: Roadmap) {
        supportActionBar?.title = "Roadmap: ${roadmap.skillName}"

        binding.tvSkillName.text = roadmap.skillName
        binding.tvEstimatedDuration.text = "⏱️ Estimated Duration: ${roadmap.estimatedDuration}"
        binding.tvGeneratedAt.text = "Generated: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(roadmap.generatedAt))}"

        // Display steps
        displaySteps(roadmap.steps)

        // Display resources
        displayResources(roadmap.resources)

        // Display full content in a scrollable text view
        binding.tvRoadmapContent.text = roadmap.roadmapContent
        binding.tvRoadmapContent.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun displaySteps(steps: List<RoadmapStep>) {
        val stepsContainer = binding.linearLayoutSteps
        stepsContainer.removeAllViews()

        steps.forEach { step ->
            val stepView = layoutInflater.inflate(R.layout.item_roadmap_step, stepsContainer, false)
            val tvStepTitle = stepView.findViewById<android.widget.TextView>(R.id.tvStepTitle)
            val tvStepDuration = stepView.findViewById<android.widget.TextView>(R.id.tvStepDuration)
            val tvStepDescription = stepView.findViewById<android.widget.TextView>(R.id.tvStepDescription)

            tvStepTitle.text = "Step ${step.stepNumber}: ${step.title}"
            tvStepDuration.text = step.duration
            tvStepDescription.text = step.description

            stepsContainer.addView(stepView)
        }
    }

    private fun displayResources(resources: List<com.hirehuborg.careers.data.model.Resource>) {
        val resourcesContainer = binding.linearLayoutResources
        resourcesContainer.removeAllViews()

        if (resources.isEmpty()) {
            val tvNoResources = android.widget.TextView(this).apply {
                text = "No specific resources provided. Try searching online for tutorials and courses."
                setTextColor(getColor(android.R.color.darker_gray))
                setPadding(0, 16, 0, 16)
            }
            resourcesContainer.addView(tvNoResources)
            return
        }

        resources.forEach { resource ->
            val resourceView = layoutInflater.inflate(R.layout.item_roadmap_resource, resourcesContainer, false)
            val tvResourceTitle = resourceView.findViewById<android.widget.TextView>(R.id.tvResourceTitle)
            val tvResourceType = resourceView.findViewById<android.widget.TextView>(R.id.tvResourceType)
            val tvResourceUrl = resourceView.findViewById<android.widget.TextView>(R.id.tvResourceUrl)

            tvResourceTitle.text = resource.title
            tvResourceType.text = resource.type.uppercase()
            tvResourceUrl.text = resource.url
            tvResourceUrl.movementMethod = LinkMovementMethod.getInstance()

            resourcesContainer.addView(resourceView)
        }
    }
}