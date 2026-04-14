package com.hirehuborg.careers.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hirehuborg.careers.databinding.ActivityLoginBinding
import com.hirehuborg.careers.ui.viewmodels.AuthState
import com.hirehuborg.careers.ui.viewmodels.AuthViewModel
import com.hirehuborg.careers.utils.HireHubAnimUtils.fadeIn
import com.hirehuborg.careers.utils.HireHubAnimUtils.slideUpFadeIn
import com.hirehuborg.careers.utils.HireHubAnimUtils.shake
import com.hirehuborg.careers.utils.ValidationUtils

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate UI elements on load
        animateEntrance()
        setupClickListeners()
        observeAuthState()
    }

    private fun animateEntrance() {
        binding.viewHeader.slideUpFadeIn(durationMs = 400, delayMs = 0)
        binding.cardForm.slideUpFadeIn(durationMs = 500, delayMs = 150)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val error    = ValidationUtils.validateLoginInputs(email, password)
            if (error != null) {
                showError(error)
                binding.cardForm.shake()
                return@setOnClickListener
            }
            hideError()
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(
                com.hirehuborg.careers.R.anim.slide_in_right,
                com.hirehuborg.careers.R.anim.slide_out_left
            )
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> showLoading(true)
                is AuthState.Success -> {
                    showLoading(false)
                    navigateToMain()
                }
                is AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                    binding.cardForm.shake()
                }
                is AuthState.Idle -> showLoading(false)
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled     = !show
        binding.btnLogin.alpha         = if (show) 0.6f else 1f
    }

    private fun showError(message: String) {
        binding.tvError.text       = message
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.fadeIn(durationMs = 250)
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            com.hirehuborg.careers.R.anim.slide_in_left,
            com.hirehuborg.careers.R.anim.slide_out_right
        )
    }
}