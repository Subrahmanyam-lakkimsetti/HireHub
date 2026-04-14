package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.databinding.ActivityJobDetailBinding
import com.hirehuborg.careers.utils.Constants

class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding
    private var currentJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadJob()
        setupClicks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadJob() {
        @Suppress("DEPRECATION")
        val job = intent.getSerializableExtra(Constants.EXTRA_JOB_OBJECT) as? Job
        currentJob = job ?: return

        binding.tvDetailTitle.text    = job.title
        binding.tvDetailCompany.text  = job.company
        binding.tvDetailLocation.text = "📍 ${job.location.ifBlank { "Remote" }}"
        binding.tvDetailSource.text   = job.source.replaceFirstChar { it.uppercase() }

        binding.tvDetailSalary.text = job.salary
        binding.tvDetailSalary.visibility = if (job.salary.isBlank())
            View.GONE else View.VISIBLE

        // Strip HTML tags from description if present
        binding.tvDetailDescription.text = job.description
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("&[a-zA-Z]+;"), " ")
            .trim()

        // Source badge color
        val badgeColor = when (job.source) {
            "remotive"  -> "#1A73E8"
            "arbeitnow" -> "#00C853"
            else        -> "#6B7280"
        }
        binding.tvDetailSource.setBackgroundTintList(
            ColorStateList.valueOf(
                Color.parseColor(badgeColor)
            )
        )
    }

    private fun setupClicks() {
        binding.btnApply.setOnClickListener {
            val url = currentJob?.applyUrl ?: return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Could not open link. Check your browser.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}