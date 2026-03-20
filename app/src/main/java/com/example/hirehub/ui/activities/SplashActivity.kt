package com.example.hirehub.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.hirehub.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.hirehub.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        lifecycleScope.launch {
            delay(1500L)
            navigateToNextScreen()
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun navigateToNextScreen() {


          val intent = Intent(this, LoginActivity::class.java)

        startActivity(intent)
        finish()
    }
}