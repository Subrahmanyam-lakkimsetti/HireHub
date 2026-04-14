package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.data.repository.SavedJobsRepository
import com.hirehuborg.careers.databinding.ActivityMainBinding
import com.hirehuborg.careers.databinding.LayoutTabHomeBinding
import com.hirehuborg.careers.databinding.LayoutTabJobsBinding
import com.hirehuborg.careers.databinding.LayoutTabProfileBinding
import com.hirehuborg.careers.ui.adapters.JobAdapter
import com.hirehuborg.careers.ui.viewmodels.JobListState
import com.hirehuborg.careers.ui.viewmodels.JobViewModel
import com.hirehuborg.careers.ui.viewmodels.MainViewModel
import com.hirehuborg.careers.ui.viewmodels.ProfileUpdateState
import com.hirehuborg.careers.ui.views.ScoreCircleView
import com.hirehuborg.careers.utils.Constants
import com.hirehuborg.careers.utils.HireHubAnimUtils.fadeIn
import com.hirehuborg.careers.utils.HireHubAnimUtils.pulse
import com.hirehuborg.careers.utils.HireHubAnimUtils.runLayoutAnimation
import com.hirehuborg.careers.utils.HireHubAnimUtils.slideUpFadeIn
import com.hirehuborg.careers.utils.HireHubAnimUtils.staggerSlideUp
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Nullable bindings — initialized after setContentView
    private var homeBinding:    LayoutTabHomeBinding?    = null
    private var jobsBinding:    LayoutTabJobsBinding?    = null
    private var profileBinding: LayoutTabProfileBinding? = null

    private val mainViewModel: MainViewModel by viewModels()
    private val jobViewModel:  JobViewModel  by viewModels()
    private val savedJobsRepository = SavedJobsRepository()
    private lateinit var jobAdapter: JobAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind each included layout to its own binding object
        homeBinding    = LayoutTabHomeBinding.bind(
            binding.root.findViewById(R.id.tabHome)
        )
        jobsBinding    = LayoutTabJobsBinding.bind(
            binding.root.findViewById(R.id.tabJobs)
        )
        profileBinding = LayoutTabProfileBinding.bind(
            binding.root.findViewById(R.id.tabProfile)
        )

        setupBottomNav()
        setupHomeTab()
        setupJobsTab()
        setupProfileTab()

        mainViewModel.loadUserProfile()
        jobViewModel.loadJobs()

        observeMainViewModel()
        observeJobViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Null out bindings to avoid memory leaks
        homeBinding    = null
        jobsBinding    = null
        profileBinding = null
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome    -> showTab(0)
                R.id.navJobs    -> showTab(1)
                R.id.navProfile -> showTab(2)
            }
            true
        }
        binding.bottomNav.selectedItemId = R.id.navHome
    }

    private fun showTab(index: Int) {
        binding.frameContent.findViewById<View>(R.id.tabHome).visibility    =
            if (index == 0) View.VISIBLE else View.GONE
        binding.frameContent.findViewById<View>(R.id.tabJobs).visibility    =
            if (index == 1) View.VISIBLE else View.GONE
        binding.frameContent.findViewById<View>(R.id.tabProfile).visibility =
            if (index == 2) View.VISIBLE else View.GONE
    }

    // ── Home Tab ──────────────────────────────────────────────────────────────
    private fun setupHomeTab() {
        val home = homeBinding ?: return

        // ✅ FIX: Build list first, then pass to staggerSlideUp — no ?: return inside listOf()
        val cards = listOf(
            home.cardUploadResume,
            home.cardAnalyzeResume,
            home.cardBrowseJobs,
            home.cardSavedJobs
        )
        staggerSlideUp(cards, baseDelayMs = 80L)

        home.cardUploadResume.setOnClickListener {
            it.pulse()
            startActivity(Intent(this, ResumeUploadActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        home.cardAnalyzeResume.setOnClickListener {
            it.pulse()
            startActivity(Intent(this, ResumeUploadActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        home.cardBrowseJobs.setOnClickListener {
            it.pulse()
            binding.bottomNav.selectedItemId = R.id.navJobs
        }
        home.cardSavedJobs.setOnClickListener {
            it.pulse()
            startActivity(Intent(this, SavedJobsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    // ── Jobs Tab ──────────────────────────────────────────────────────────────
    private fun setupJobsTab() {
        val jobs = jobsBinding ?: return

        jobAdapter = JobAdapter(
            onJobClick  = { match -> openJobDetail(match) },
            onSaveClick = { match -> toggleSaveJob(match) }
        )
        jobs.rvJobsMain.layoutManager = LinearLayoutManager(this)
        jobs.rvJobsMain.adapter       = jobAdapter
        jobs.rvJobsMain.itemAnimator  = null

        jobs.etSearchJobs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jobViewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        jobs.swipeRefreshJobs.setOnRefreshListener {
            jobViewModel.refresh()
        }
        jobs.swipeRefreshJobs.setColorSchemeColors(getColor(R.color.primary))

        jobs.btnRetryJobs.setOnClickListener {
            jobViewModel.loadJobs(forceRefresh = true)
        }
    }

    // ── Profile Tab ───────────────────────────────────────────────────────────
    private fun setupProfileTab() {
        val profile = profileBinding ?: return

        profile.btnSaveProfile.setOnClickListener {
            val name  = profile.etProfileName.text.toString().trim()
            val email = profile.etProfileEmail.text.toString().trim()
            mainViewModel.updateProfile(name, email)
        }
        profile.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────
    private fun observeMainViewModel() {
        mainViewModel.user.observe(this) { user ->
            val home    = homeBinding    ?: return@observe
            val profile = profileBinding ?: return@observe

            // ── Home tab ──────────────────────────────────────────────────────
            home.tvGreeting.text  = getGreeting()
            home.tvUserName.text  = user.name
            home.tvUserEmail.text = user.email

            // ✅ FIX: Direct cast to ScoreCircleView — no reflection needed
            if (user.resumeScore > 0) {
                home.root
                    .findViewById<ScoreCircleView>(R.id.miniScoreView)
                    ?.setScore(user.resumeScore, animate = true)
            }

            // Skills chips
            home.chipGroupSkills.removeAllViews()
            if (user.skills.isNotEmpty()) {
                home.tvNoSkills.visibility = View.GONE
                user.skills.take(12).forEach { skill ->
                    val chip = Chip(this).apply {
                        text = skill
                        isClickable = false
                        isCheckable = false
                        chipBackgroundColor = ColorStateList.valueOf(
                            Color.parseColor("#E3F2FD")
                        )
                        setTextColor(Color.parseColor("#0D47A1"))
                        textSize = 11f
                    }
                    home.chipGroupSkills.addView(chip)
                }
            } else {
                home.tvNoSkills.visibility = View.VISIBLE
            }

            // ── Profile tab ───────────────────────────────────────────────────
            val initial = user.name.firstOrNull()?.uppercase() ?: "U"
            profile.tvAvatarInitial.text = initial
            profile.tvProfileName.text   = user.name
            profile.tvProfileEmail.text  = user.email
            profile.tvProfileScore.text  =
                "ATS Score: ${if (user.resumeScore > 0) user.resumeScore else "--"}"
            profile.tvStatScore.text =
                if (user.resumeScore > 0) user.resumeScore.toString() else "--"
            profile.tvStatSkills.text =
                if (user.skills.isNotEmpty()) user.skills.size.toString() else "--"

            if (profile.etProfileName.text.isNullOrBlank()) {
                profile.etProfileName.setText(user.name)
            }
            if (profile.etProfileEmail.text.isNullOrBlank()) {
                profile.etProfileEmail.setText(user.email)
            }
        }

        mainViewModel.profileUpdateState.observe(this) { state ->
            val profile = profileBinding ?: return@observe
            when (state) {
                is ProfileUpdateState.Idle -> {
                    profile.progressProfile.visibility = View.GONE
                    profile.btnSaveProfile.isEnabled   = true
                    profile.tvProfileError.visibility  = View.GONE
                }
                is ProfileUpdateState.Loading -> {
                    profile.progressProfile.visibility = View.VISIBLE
                    profile.btnSaveProfile.isEnabled   = false
                    profile.tvProfileError.visibility  = View.GONE
                }
                is ProfileUpdateState.Success -> {
                    profile.progressProfile.visibility = View.GONE
                    profile.btnSaveProfile.isEnabled   = true
                    profile.tvProfileError.visibility  = View.GONE
                    Toast.makeText(this, "✅ ${state.msg}", Toast.LENGTH_SHORT).show()
                    mainViewModel.resetProfileUpdateState()
                }
                is ProfileUpdateState.Error -> {
                    profile.progressProfile.visibility = View.GONE
                    profile.btnSaveProfile.isEnabled   = true
                    profile.tvProfileError.visibility  = View.VISIBLE
                    profile.tvProfileError.text        = state.msg
                }
            }
        }
    }

    private fun observeJobViewModel() {
        jobViewModel.state.observe(this) { state ->
            val jobs = jobsBinding ?: return@observe

            jobs.swipeRefreshJobs.isRefreshing = false

            // ✅ Now works directly — no findViewById cast needed
            jobs.shimmerJobs.stopShimmer()
            jobs.shimmerJobs.visibility       = View.GONE
            jobs.layoutJobsLoading.visibility = View.GONE
            jobs.layoutJobsError.visibility   = View.GONE
            jobs.rvJobsMain.visibility        = View.GONE
            jobs.tvJobsCount.visibility       = View.GONE

            when (state) {
                is JobListState.Idle -> Unit

                is JobListState.Loading -> {
                    jobs.shimmerJobs.visibility = View.VISIBLE
                    jobs.shimmerJobs.startShimmer()
                }

                is JobListState.Success -> {
                    jobs.rvJobsMain.visibility  = View.VISIBLE
                    jobs.tvJobsCount.visibility = View.VISIBLE
                    jobs.tvJobsCount.fadeIn()
                    jobs.tvJobsCount.text =
                        "🎯 ${state.matches.size} jobs matched to your skills"
                    jobAdapter.submitList(state.matches)
                    jobs.rvJobsMain.runLayoutAnimation()
                }

                is JobListState.NoSkills -> {
                    jobs.layoutJobsError.visibility = View.VISIBLE
                    jobs.layoutJobsError.fadeIn()
                    jobs.tvJobsError.text =
                        "📄 Upload your resume first to see matched jobs."
                    jobs.btnRetryJobs.text = "Upload Resume"
                    jobs.btnRetryJobs.setOnClickListener {
                        startActivity(Intent(this, ResumeUploadActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }

                is JobListState.NoMatches -> {
                    jobs.layoutJobsError.visibility = View.VISIBLE
                    jobs.layoutJobsError.fadeIn()
                    jobs.tvJobsError.text =
                        "😕 No jobs matched your skills yet. Try refreshing."
                }

                is JobListState.Error -> {
                    jobs.layoutJobsError.visibility = View.VISIBLE
                    jobs.layoutJobsError.fadeIn()
                    jobs.tvJobsError.text = state.message
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private fun openJobDetail(match: JobMatch) {
        val intent = Intent(this, JobDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_JOB_OBJECT, match.job)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun toggleSaveJob(match: JobMatch) {
        val job = match.job
        lifecycleScope.launch {
            if (job.isSaved) {
                savedJobsRepository.removeJob(job.id).fold(
                    onSuccess = {
                        job.isSaved = false
                        val idx = jobAdapter.currentList
                            .indexOfFirst { it.job.id == job.id }
                        if (idx != -1) jobAdapter.notifyItemChanged(idx)
                        showSnackbar("Removed from saved jobs")
                    },
                    onFailure = { showSnackbar("Could not remove job") }
                )
            } else {
                savedJobsRepository.saveJob(job).fold(
                    onSuccess = {
                        job.isSaved = true
                        val idx = jobAdapter.currentList
                            .indexOfFirst { it.job.id == job.id }
                        if (idx != -1) {
                            jobAdapter.notifyItemChanged(idx)
                            // ✅ FIX: Use local val to safely access jobsBinding
                            jobsBinding?.rvJobsMain
                                ?.findViewHolderForAdapterPosition(idx)
                                ?.itemView
                                ?.findViewById<ImageView>(R.id.ivSave)
                                ?.pulse()
                        }
                        showSnackbar("✅ Job saved!")
                    },
                    onFailure = { showSnackbar("Could not save job") }
                )
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                mainViewModel.logout()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning 🌅"
            in 12..16 -> "Good afternoon ☀️"
            else      -> "Good evening 🌙"
        }
    }
}