package com.hirehuborg.careers.utils

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun validateLoginInputs(email: String, password: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !isValidEmail(email) -> "Enter a valid email address"
            password.isBlank() -> "Password cannot be empty"
            !isValidPassword(password) -> "Password must be at least 6 characters"
            else -> null // null = valid
        }
    }

    fun validateRegisterInputs(name: String, email: String, password: String, confirmPassword: String): String? {
        return when {
            name.isBlank() -> "Name cannot be empty"
            !isValidName(name) -> "Name must be at least 2 characters"
            email.isBlank() -> "Email cannot be empty"
            !isValidEmail(email) -> "Enter a valid email address"
            password.isBlank() -> "Password cannot be empty"
            !isValidPassword(password) -> "Password must be at least 6 characters"
            confirmPassword.isBlank() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }
}