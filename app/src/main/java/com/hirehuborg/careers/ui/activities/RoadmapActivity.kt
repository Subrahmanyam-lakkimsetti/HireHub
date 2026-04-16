package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.Roadmap
import com.hirehuborg.careers.data.repository.RoadmapRepository
import com.hirehuborg.careers.ui.adapters.SavedRoadmapsAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoadmapActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var containerContent: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    private val roadmapRepository = RoadmapRepository()
    private lateinit var savedRoadmapsAdapter: SavedRoadmapsAdapter

    // Views for Generate tab
    private lateinit var generateView: View
    private lateinit var chipGroupMissingSkills: ChipGroup
    private lateinit var tvNoMissingSkills: TextView
    private lateinit var btnGenerateRoadmap: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Views for Saved tab
    private lateinit var savedView: View
    private lateinit var rvSavedRoadmaps: RecyclerView
    private lateinit var tvNoSavedRoadmaps: TextView

    private var currentMissingSkills: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roadmap)

        setupToolbar()
        setupBottomNav()
        setupTabs()
        createGenerateTabView()
        createSavedTabView()
        loadMissingSkills()
        observeSavedRoadmaps()

        // Default to Generate tab
        showGenerateTab()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarRoadmap)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Learning Roadmaps"
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.navRoadmap -> {
                    // Already in roadmap activity
                    true
                }
                R.id.navJobs -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("selected_tab", 1)
                    })
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.navProfile -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("selected_tab", 2)
                    })
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
        // Highlight the roadmap item
        bottomNav.selectedItemId = R.id.navRoadmap
    }

    private fun setupTabs() {
        tabLayout = findViewById(R.id.tabLayout)
        containerContent = findViewById(R.id.containerContent)

        tabLayout.addTab(tabLayout.newTab().setText("Generate New"))
        tabLayout.addTab(tabLayout.newTab().setText("Saved Roadmaps"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showGenerateTab()
                    1 -> showSavedTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun createGenerateTabView() {
        generateView = LayoutInflater.from(this).inflate(R.layout.layout_generate_roadmap, containerContent, false)
        chipGroupMissingSkills = generateView.findViewById(R.id.chipGroupMissingSkills)
        tvNoMissingSkills = generateView.findViewById(R.id.tvNoMissingSkills)
        btnGenerateRoadmap = generateView.findViewById(R.id.btnGenerateRoadmap)
        progressBar = generateView.findViewById(R.id.progressBar)

        btnGenerateRoadmap.setOnClickListener {
            onGenerateRoadmapClick()
        }
    }

    private fun createSavedTabView() {
        savedView = LayoutInflater.from(this).inflate(R.layout.layout_saved_roadmaps, containerContent, false)
        rvSavedRoadmaps = savedView.findViewById(R.id.rvSavedRoadmaps)
        tvNoSavedRoadmaps = savedView.findViewById(R.id.tvNoSavedRoadmaps)

        savedRoadmapsAdapter = SavedRoadmapsAdapter(
            onItemClick = { roadmap -> viewRoadmapDetails(roadmap) },
            onDeleteClick = { roadmap -> confirmDeleteRoadmap(roadmap) }
        )
        rvSavedRoadmaps.layoutManager = LinearLayoutManager(this)
        rvSavedRoadmaps.adapter = savedRoadmapsAdapter
    }

    private fun showGenerateTab() {
        containerContent.removeAllViews()
        containerContent.addView(generateView)
    }

    private fun showSavedTab() {
        containerContent.removeAllViews()
        containerContent.addView(savedView)
    }

    private fun loadMissingSkills() {
        lifecycleScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val database = FirebaseDatabase.getInstance()
                    val snapshot = database.getReference("resumes")
                        .child(uid)
                        .child("analysis")
                        .child("missingSkills")
                        .get()
                        .await()

                    val skills = mutableListOf<String>()
                    for (child in snapshot.children) {
                        val skill = child.getValue(String::class.java)
                        if (skill != null) {
                            skills.add(skill)
                        }
                    }
                    currentMissingSkills = skills
                    displayMissingSkillsChips(skills)
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoadmapActivity, "Error loading skills: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayMissingSkillsChips(skills: List<String>) {
        chipGroupMissingSkills.removeAllViews()

        if (skills.isEmpty()) {
            tvNoMissingSkills.visibility = View.VISIBLE
            btnGenerateRoadmap.isEnabled = false
            return
        }

        tvNoMissingSkills.visibility = View.GONE
        btnGenerateRoadmap.isEnabled = true

        for (skill in skills) {
            val chip = Chip(this).apply {
                text = skill
                id = View.generateViewId()
                isClickable = true
                isCheckable = true
                setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        for (i in 0 until chipGroupMissingSkills.childCount) {
                            val child = chipGroupMissingSkills.getChildAt(i)
                            if (child is Chip && child != buttonView) {
                                child.isChecked = false
                            }
                        }
                        btnGenerateRoadmap.isEnabled = true
                    } else if (chipGroupMissingSkills.checkedChipId == View.NO_ID) {
                        btnGenerateRoadmap.isEnabled = false
                    }
                }
            }
            chipGroupMissingSkills.addView(chip)
        }
    }

    private fun onGenerateRoadmapClick() {
        var selectedSkill: String? = null

        for (i in 0 until chipGroupMissingSkills.childCount) {
            val child = chipGroupMissingSkills.getChildAt(i)
            if (child is Chip && child.isChecked) {
                selectedSkill = child.text.toString()
                break
            }
        }

        if (selectedSkill.isNullOrEmpty()) {
            Toast.makeText(this@RoadmapActivity, "Please select a skill first", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnGenerateRoadmap.isEnabled = false

        lifecycleScope.launch {
            val result = roadmapRepository.generateRoadmap(selectedSkill)
            progressBar.visibility = View.GONE
            btnGenerateRoadmap.isEnabled = true

            result.onSuccess { roadmap ->
                Toast.makeText(
                    this@RoadmapActivity,
                    "Roadmap generated for $selectedSkill!",
                    Toast.LENGTH_SHORT
                ).show()
                viewRoadmapDetails(roadmap)
                tabLayout.getTabAt(1)?.select()
                loadSavedRoadmaps()
            }.onFailure { error ->
                Toast.makeText(
                    this@RoadmapActivity,
                    "Failed to generate roadmap: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadSavedRoadmaps() {
        lifecycleScope.launch {
            val roadmaps = roadmapRepository.getSavedRoadmaps()
            savedRoadmapsAdapter.submitList(roadmaps)
            if (roadmaps.isEmpty()) {
                tvNoSavedRoadmaps.visibility = View.VISIBLE
                rvSavedRoadmaps.visibility = View.GONE
            } else {
                tvNoSavedRoadmaps.visibility = View.GONE
                rvSavedRoadmaps.visibility = View.VISIBLE
            }
        }
    }

    private fun observeSavedRoadmaps() {
        lifecycleScope.launch {
            roadmapRepository.observeSavedRoadmaps().collect { roadmaps ->
                savedRoadmapsAdapter.submitList(roadmaps)
                if (roadmaps.isEmpty()) {
                    tvNoSavedRoadmaps.visibility = View.VISIBLE
                    rvSavedRoadmaps.visibility = View.GONE
                } else {
                    tvNoSavedRoadmaps.visibility = View.GONE
                    rvSavedRoadmaps.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun viewRoadmapDetails(roadmap: Roadmap) {
        val intent = Intent(this, RoadmapDetailActivity::class.java)
        intent.putExtra("roadmap_id", roadmap.id)
        intent.putExtra("skill_name", roadmap.skillName)
        startActivity(intent)
    }

    private fun confirmDeleteRoadmap(roadmap: Roadmap) {
        AlertDialog.Builder(this)
            .setTitle("Delete Roadmap")
            .setMessage("Are you sure you want to delete the roadmap for '${roadmap.skillName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    roadmapRepository.deleteRoadmap(roadmap.id)
                        .onSuccess {
                            Toast.makeText(
                                this@RoadmapActivity,
                                "Roadmap deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this@RoadmapActivity,
                                "Failed to delete: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}