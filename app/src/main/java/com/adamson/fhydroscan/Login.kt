package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        usernameInput = findViewById(R.id.username)
        passwordInput = findViewById(R.id.password)
        loginButton = findViewById(R.id.btlogin)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            when {
                username.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                username == "admin123" && password == "12345" -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    finish()
                }
                username == "staff123" && password == "12345" -> {
                    startActivity(Intent(this, DashboardE::class.java))
                    finish()
                }
                username == "customer123" && password == "12345" -> {
                    startActivity(Intent(this, Customer::class.java))
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
