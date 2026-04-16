package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.databinding.ActivitySavedJobsBinding
import com.hirehuborg.careers.ui.adapters.SavedJobsAdapter
import com.hirehuborg.careers.ui.viewmodels.SavedJobsState
import com.hirehuborg.careers.ui.viewmodels.SavedJobsViewModel
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.launch

class SavedJobsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedJobsBinding
    private val viewModel: SavedJobsViewModel by viewModels()
    private lateinit var adapter: SavedJobsAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedJobsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClicks()
        observeViewModel()

        // Start observing real-time RTDB changes
        viewModel.observeSavedJobs()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SavedJobsAdapter(
            onJobClick    = { job -> openJobDetail(job) },
            onRemoveClick = { job -> removeJob(job) }
        )
        binding.rvSavedJobs.layoutManager = LinearLayoutManager(this)
        binding.rvSavedJobs.adapter       = adapter
        binding.rvSavedJobs.itemAnimator  = null
    }

    private fun setupClicks() {
        binding.btnBrowseJobs.setOnClickListener {
            finish() // go back to JobListActivity
        }
    }

    // ── Observer ──────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                // Reset all views
                binding.progressBar.visibility  = View.GONE
                binding.layoutEmpty.visibility  = View.GONE
                binding.layoutError.visibility  = View.GONE
                binding.rvSavedJobs.visibility  = View.GONE
                binding.tvSavedCount.visibility = View.GONE

                when (state) {
                    is SavedJobsState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is SavedJobsState.Empty -> {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    }

                    is SavedJobsState.Success -> {
                        binding.rvSavedJobs.visibility  = View.VISIBLE
                        binding.tvSavedCount.visibility = View.VISIBLE
                        binding.tvSavedCount.text =
                            "🔖 ${state.jobs.size} saved job${if (state.jobs.size != 1) "s" else ""}"
                        adapter.submitList(state.jobs)
                    }

                    is SavedJobsState.Error -> {
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text           = state.message
                    }
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private fun openJobDetail(job: Job) {
        val intent = Intent(this, JobDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_JOB_OBJECT, job)
        }
        startActivity(intent)
    }

    private fun removeJob(job: Job) {
        viewModel.removeJob(job.id)
        // UI updates automatically via the real-time Flow observer
    }
}