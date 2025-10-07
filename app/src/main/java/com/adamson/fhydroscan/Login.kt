package com.adamson.fhydroscan

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adamson.fhydroscan.database.DatabaseHelper

class Login : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize database
        databaseHelper = DatabaseHelper(this)
        
        usernameInput = findViewById(R.id.username)
        passwordInput = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.btsignup)
        forgotPasswordText = findViewById(R.id.forgotPasswordLink)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            when {
                username.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                username.length < 4 -> {
                    Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show()
                }
                username.length > 25 -> {
                    Toast.makeText(this, "Username must be 25 characters or less", Toast.LENGTH_SHORT).show()
                }
                password.length < 8 -> {
                    Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                }
                password.length > 50 -> {
                    Toast.makeText(this, "Password must be 50 characters or less", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Check credentials in database
                    val user = databaseHelper.getUser(username, password)
                    if (user != null) {
                        // Save current user ID for cart functionality
                        saveCurrentUserId(user.username)
                        
                        // Navigate based on user type
                        when (user.userType) {
                            "ADMIN" -> {
                                startActivity(Intent(this, Dashboard::class.java))
                                finish()
                            }
                            "STAFF" -> {
                                startActivity(Intent(this, DashboardE::class.java))
                                finish()
                            }
                            "CUSTOMER" -> {
                                startActivity(Intent(this, Customer::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Invalid user type", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        
        val usernameEditText = dialogView.findViewById<EditText>(R.id.forgotUsername)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.confirmNewPassword)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val resetButton = dialogView.findViewById<Button>(R.id.resetButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        resetButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            when {
                username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                username.length < 4 -> {
                    Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show()
                }
                username.length > 25 -> {
                    Toast.makeText(this, "Username must be 25 characters or less", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 8 -> {
                    Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                }
                newPassword.length > 50 -> {
                    Toast.makeText(this, "Password must be 50 characters or less", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.length < 8 -> {
                    Toast.makeText(this, "Confirm password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.length > 50 -> {
                    Toast.makeText(this, "Confirm password must be 50 characters or less", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                !databaseHelper.isUsernameExists(username) -> {
                    Toast.makeText(this, "Username does not exist", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Update password in database
                    val success = databaseHelper.updatePassword(username, newPassword)
                    if (success) {
                        Toast.makeText(this, "Password reset successfully! Please login with your new password.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Failed to reset password. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun saveCurrentUserId(userId: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("current_user_id", userId)
            apply()
        }
    }
}
