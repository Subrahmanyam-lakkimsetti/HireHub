package com.hirehuborg.careers.ui.activities


import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hirehuborg.careers.ui.activities.MainActivity
import com.hirehuborg.careers.databinding.ActivityRegisterBinding
import com.hirehuborg.careers.ui.viewmodels.AuthState
import com.hirehuborg.careers.ui.viewmodels.AuthViewModel
import com.hirehuborg.careers.utils.ValidationUtils

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            val error = ValidationUtils.validateRegisterInputs(name, email, password, confirm)
            if (error != null) {
                showError(error)
                return@setOnClickListener
            }

            hideError()
            viewModel.register(name, email, password)
        }

        binding.tvLogin.setOnClickListener { finish() }
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
                }

                is AuthState.Idle -> showLoading(false)
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, ResumeUploadActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnRegister.alpha = if (show) 0.6f else 1f
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}