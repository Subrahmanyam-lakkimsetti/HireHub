package com.hirehuborg.careers.ui.activities

import android.R
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hirehuborg.careers.ui.activities.MainActivity
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.databinding.ActivityJobListBinding
import com.hirehuborg.careers.ui.adapters.JobAdapter
import com.hirehuborg.careers.ui.viewmodels.JobListState
import com.hirehuborg.careers.ui.viewmodels.JobViewModel
import com.hirehuborg.careers.utils.Constants
import com.hirehuborg.careers.data.repository.SavedJobsRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class JobListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobListBinding
    private val viewModel: JobViewModel by viewModels()
    private lateinit var adapter: JobAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupClicks()
        observeViewModel()

        viewModel.loadJobs()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        adapter = JobAdapter(
            onJobClick  = { match -> openJobDetail(match) },
            onSaveClick = { match -> toggleSave(match) }
        )
        binding.rvJobs.layoutManager = LinearLayoutManager(this)
        binding.rvJobs.adapter       = adapter
        binding.rvJobs.itemAnimator  = null
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClicks() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.swipeRefresh.setColorSchemeResources(R.color.holo_blue_bright)
        binding.btnRetry.setOnClickListener { viewModel.loadJobs(forceRefresh = true) }
        binding.btnGoToResume.setOnClickListener {
            startActivity(Intent(this, ResumeUploadActivity::class.java))
        }
        binding.btnSavedJobs.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false

            // Hide everything first — then show what's needed
            binding.layoutLoading.visibility = View.GONE
            binding.layoutError.visibility   = View.GONE
            binding.rvJobs.visibility        = View.GONE
            binding.tvJobCount.visibility    = View.GONE

            when (state) {
                is JobListState.Idle    -> Unit

                is JobListState.Loading -> {
                    binding.layoutLoading.visibility = View.VISIBLE
                }

                is JobListState.Success -> {
                    binding.rvJobs.visibility     = View.VISIBLE
                    binding.tvJobCount.visibility = View.VISIBLE
                    binding.tvJobCount.text =
                        "🎯 ${state.matches.size} jobs matched to your skills"
                    adapter.submitList(state.matches)
                }

                is JobListState.NoSkills -> {
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvError.text =
                        "📄 No resume skills found.\n\nUpload your resume to see jobs matched to your profile."
                    binding.btnRetry.text = "Upload Resume"
                    binding.btnRetry.setOnClickListener {
                        startActivity(Intent(this, ResumeUploadActivity::class.java))
                    }
                }

                is JobListState.NoMatches -> {
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvError.text =
                        "😕 No jobs matched your current skill set.\n\nTry refreshing or uploading an updated resume."
                }

                is JobListState.Error -> {
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvError.text           = state.message
                }
            }
        }
    }

    private fun openJobDetail(match: JobMatch) {
        val intent = Intent(this, JobDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_JOB_OBJECT, match.job)
        }
        startActivity(intent)
    }

    // Add this repository at the top of JobListActivity class
    private val savedJobsRepository = SavedJobsRepository()

    // Replace the existing stub toggleSave() with this
    private fun toggleSave(match: JobMatch) {
        val job = match.job
        lifecycleScope.launch {
            if (job.isSaved) {
                // Already saved → remove it
                savedJobsRepository.removeJob(job.id).fold(
                    onSuccess = {
                        job.isSaved = false
                        updateJobSaveIcon(match)
                        showSnackbar("Job removed from saved")
                    },
                    onFailure = {
                        showSnackbar("Could not remove job. Try again.")
                    }
                )
            } else {
                // Not saved → save it
                savedJobsRepository.saveJob(job).fold(
                    onSuccess = {
                        job.isSaved = true
                        updateJobSaveIcon(match)
                        showSnackbar("✅ Job saved!")
                    },
                    onFailure = {
                        showSnackbar("Could not save job. Try again.")
                    }
                )
            }
        }
    }

    private fun updateJobSaveIcon(match: JobMatch) {
        val index = adapter.currentList.indexOfFirst { it.job.id == match.job.id }
        if (index != -1) adapter.notifyItemChanged(index)
    }

    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }
}
